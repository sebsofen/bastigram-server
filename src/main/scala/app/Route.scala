package app

//critical for path directives etc

import actors.RepoActor._
import actors.SearchActor.{FindStuff, SearchActorRef, SearchResults}
import actors.likes.PostLikesMasterActor.PostLikesMasterActorRef
import actors.likes.UserLikesActor.{UserDisLikeRecord, UserLikeRecord}
import actors.likes.UserLikesMasterActor.{SetUserLikesPost, UserLikesMasterActorRef}
import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{DateTime, HttpResponse}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import repo.EntriesReadRepo._
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait RestResponseProtocol {
  case class RestSuccess(reason: String, message: String)
  case class RestFailure(reason: String, message: String)
}

trait AppJsonSupport extends SprayJsonSupport with DefaultJsonProtocol with RestResponseProtocol {

  implicit val restSuccessFormat = jsonFormat2(RestSuccess)
  implicit val restFailureFormat = jsonFormat2(RestFailure)

  implicit val hashTagMarshaller = jsonFormat1(HashTag)
  implicit val imageMarshaller = jsonFormat1(EntryImage)

  implicit val locationMarshaller = jsonFormat1(EntryLocation)
  implicit val bodyMarshaller = jsonFormat1(EntryBody)

  implicit val entryMarshaller = jsonFormat7(Entry)
  implicit val searchResultFormat = jsonFormat2(SearchResults)
}

trait RouterTrait extends AppJsonSupport {
  implicit val ec: ExecutionContext
  implicit val timeout = Timeout(5 seconds)

  val repoActor: ActorRef
  val likesActor: PostLikesMasterActorRef
  val searchActor: SearchActorRef

  val userLikesMasterActor: UserLikesMasterActorRef

  val route = pathPrefix("v1") {

    pathPrefix("posts") {

      pathPrefix("like") {

        path("like" / Segment) { postSlug: String =>
          post {
            authenticated { userId =>
              userLikesMasterActor.ref ! SetUserLikesPost(userId, postSlug, UserLikeRecord())
              complete("setting user like")
            }
          }
        } ~
          path("dislike" / Segment) { postSlug: String =>
            post {
              authenticated { userId =>
                userLikesMasterActor.ref ! SetUserLikesPost(userId, postSlug, UserDisLikeRecord())
                complete("setting user like")
              }
            }
          }
      } ~
        path("by-slug" / Segment) { str: String =>
          get {
            onSuccess((repoActor ? EntryBySlug(str)).mapTo[Option[Entry]]) {
              case Some(a) =>
                completeWith(instanceOf[Entry]) { f =>
                  f(a)
                }
              case _ =>
                complete(HttpResponse(status = 404))
            }

          }

        } ~
        path("by-tag" / Segment / IntNumber / IntNumber) { (tag: String, start: Int, limit: Int) =>
          get {
            completeWith(implicitly[ToResponseMarshaller[List[Entry]]]) { com =>
              (repoActor ? EntriesByTag(com, tag)).mapTo[List[Entry]].map(_.slice(start, limit))
            }
          }

        } ~
        path("by-location" / Segment / IntNumber / IntNumber) { (location: String, start: Int, limit: Int) =>
          get {
            completeWith(implicitly[ToResponseMarshaller[List[Entry]]]) { com =>
              (repoActor ? EntriesByLocation(com, EntryLocation(location)))
                .mapTo[List[Entry]]
                .map(_.slice(start, limit))
            }
          }

        } ~
        pathPrefix("slice") {
          path("by-date" / IntNumber / IntNumber) { (start: Int, stop: Int) =>
            get {
              completeWith(implicitly[ToResponseMarshaller[List[Entry]]]) { com =>
                val complete: List[Entry] => Unit = a => {
                  //a.foreach(entry => likesActor.ref ! SendLike(entry.slug))
                  com(a)
                }

                repoActor ? EntryListSliceByDate(complete, start, stop)

              }
            }
          }
        }
    } ~
      pathPrefix("hashtags") {
        path("find" / Segment) { searchstring: String =>
          get {
            completeWith(implicitly[ToResponseMarshaller[List[HashTag]]]) { com =>
              repoActor ? HashTagsBySearchString(com, searchstring)
            }
          }

        }
      } ~
      path("find" / Segment) { searchstring: String =>
        get {
          completeWith(implicitly[ToResponseMarshaller[SearchResults]]) { com =>
            searchActor.ref ? FindStuff(com, searchstring)
          }

        }
      } ~
      path("auth") {
        get {
          authenticated { userId =>
            complete(RestSuccess("user-loggedin", userId))
          }

        }
      }
  } ~
    pathPrefix("postassets") {
      encodeResponse {
        getFromDirectory(Conf.ENTRYDIR.dir)
        //getFromFile()
      }
    } ~
    pathPrefix("") {
      encodeResponse {
        getFromDirectory(Conf.WEBDIR)
        //getFromFile()
      }
    } ~
    pathPrefix("") {
      encodeResponse {
        getFromFile(Conf.WEBDIR + "/index.html")
      }
    }

  /**
    * authenticate user and create if not exists
    * @param auther
    * @return
    */
  def authenticated(auther: String => Route) = optionalCookie("userId") { optUserId =>
    {
      optUserId match {
        case Some(userId) =>
          //to nothing, because user is loggedin already?
          auther(userId.value)
        case None =>
          val exp = Some(DateTime.now.+(10 * 365 * 86400 + 1000)) //expires in ten years
          val newUserId = java.util.UUID.randomUUID.toString
          setCookie(HttpCookie("userId", value = newUserId, expires = exp)) {
            auther(newUserId)
          }
      }

    }

  }

}
