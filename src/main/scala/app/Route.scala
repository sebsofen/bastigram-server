package app

//critical for path directives etc

import actors.LikeActor.SendLike
import actors.RepoActor.{EntriesByLocation, EntryBySlug, EntryListSliceByDate, HashTagsBySearchString}
import actors.SearchActor.{FindStuff, SearchActorRef, SearchResults}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import repo.EntriesReadRepo._
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Created by sebastian on 20.06.17.
  */
class Route(
    repoAct: ActorRef, lActor: ActorRef, sActor: SearchActorRef)(implicit system: ActorSystem, _ec: ExecutionContextExecutor, materializer: ActorMaterializer)
    extends RouterTrait {
  override implicit val ec: ExecutionContext = _ec

  val repoActor = repoAct
  override val likesActor: ActorRef = lActor
  override val searchActor: SearchActorRef = sActor

}

trait AppJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
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
  val likesActor: ActorRef
  val searchActor: SearchActorRef

  val route = pathPrefix("v1") {
    pathPrefix("posts") {
      path("by-slug" / Segment) { str: String =>
        get {
          completeWith(implicitly[ToResponseMarshaller[Option[Entry]]]) { completewithFunction =>
            repoActor ? EntryBySlug(completewithFunction, str)
          }
        }

      } ~
        path("by-location" / Segment / IntNumber / IntNumber) { (tag: String, start: Int, limit: Int) =>
          get {
            completeWith(implicitly[ToResponseMarshaller[List[Entry]]]) { com =>
              (repoActor ? EntriesByLocation(com, EntryLocation(tag))).mapTo[List[Entry]].map(_.slice(start, limit))
            }
          }

        } ~
        pathPrefix("slice") {
          path("by-date" / IntNumber / IntNumber) { (start: Int, stop: Int) =>
            get {
              completeWith(implicitly[ToResponseMarshaller[List[Entry]]]) { com =>
                val complete: List[Entry] => Unit = a => {
                  a.foreach(entry => likesActor ! SendLike(entry.slug))
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
      }
  }   ~
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

}
