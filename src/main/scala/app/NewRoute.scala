package app

import actors.likes.UserLikesActor.{LikeRecord, UserDisLikeRecord, UserLikeRecord}
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives.{complete, get, onSuccess, path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import _root_.post.postentities.MapPostEntity
import v2.actors.HashTagAndStats
import v2.busses.CompiledPostBus.{CompiledPostClassifier, PostByLocationNameClassifier}
import v2.filters.AllLocationsFilter
import v2.model.CompiledPost
import v2.rest.NewJsonSupport
import v2.sorters.PostsComparer

import scala.concurrent.Future

trait NewRoute extends NewJsonSupport with PostsComparer {
  val webdir: String
  val entrydir: String

  def searchHashTag(searchString: String): Future[List[HashTagAndStats]]

  def getPostBySlug(slug: String): Future[Option[CompiledPost]]

  def getLocations(filter: AllLocationsFilter): Future[Seq[MapPostEntity]]

  def getPostBySlice(offset: Int,
                     limit: Int,
                     sorter: (CompiledPost, CompiledPost) => Boolean): Future[List[CompiledPost]]

  def getPostByTag(offset: Int,
                   limit: Int,
                   tag: String,
                   sorter: (CompiledPost, CompiledPost) => Boolean): Future[List[CompiledPost]]

  def getPostsFilteredAndSorted(offset: Int,
                                limit: Int,
                                filter: CompiledPostClassifier,
                                sorter: (CompiledPost, CompiledPost) => Boolean): Future[List[CompiledPost]]

  def getLikeRecord(userId: String, postSlug: String): Future[Any]

  def setLikeRecord(userId: String, postSlug: String, like: LikeRecord)

  def setLikeRecordIfNotExists(userId: String, postSlug: String)

  def getLikeCountBySlug(postSlug: String): Future[Int]

  def getLikedPostsByUser(offset: Int,
                          limit: Int,
                          userId: String,
                          sorter: (CompiledPost, CompiledPost) => Boolean): Future[List[CompiledPost]]

  val route = pathPrefix("v1") {
    authenticated { userId =>
      pathPrefix("likes") {
        path("doilike" / Segment) { postSlug: String =>
          get {

            onSuccess(getLikeRecord(userId, postSlug)) {
              case UserLikeRecord() => complete("yes")
              case f =>
                complete("no")

            }
          }
        } ~
          path("ilike" / Segment) { postSlug: String =>
            get {
              println("user " + userId + " like " + postSlug)
              setLikeRecord(userId, postSlug, UserLikeRecord())
              complete("")

            }
          } ~
          path("idislike" / Segment) { postSlug: String =>
            get {
              println("user " + userId + " dislike " + postSlug)
              setLikeRecord(userId, postSlug, UserDisLikeRecord())
              complete("")

            }
          } ~
          path("iautolike" / Segment) { postSlug: String =>
            get {
              setLikeRecordIfNotExists(userId, postSlug)
              complete("")

            }
          } ~
          path("by-slug" / Segment) { postSlug: String =>
            get {
              onSuccess(getLikeCountBySlug(postSlug)) { count =>
                complete("" + count)
              }

            }

          } ~
          pathPrefix("slice" / IntNumber / IntNumber) { (offset: Int, limit: Int) =>
            path("my-likes") {
              get {
                onSuccess(getLikedPostsByUser(offset, limit, userId, comparePostsByDate)) { list =>
                  complete(list)
                }

              }
            }
          }
      } ~
        pathPrefix("hashtags") {
          path("find" / Segment) { searchstring: String =>
            get {
              onSuccess(searchHashTag(searchstring)) { result =>
                complete(result)
              }
            }
          }
        } ~
        pathPrefix("posts") {
          path("by-slug" / Segment) { slug: String =>
            get {
              onSuccess(getPostBySlug(slug)) { post =>
                complete(post)
              }
            }

          } ~
            pathPrefix("slice" / IntNumber / IntNumber) { (offset: Int, limit: Int) =>
              path("by-date") {
                get {
                  onSuccess(getPostBySlice(offset, limit, comparePostsByDate)) { postList =>
                    complete(postList)
                  }
                }
              } ~
                path("by-tag" / Segment) { tag =>
                  get {
                    onSuccess(getPostByTag(offset, limit, tag, comparePostsByDate)) { postList =>
                      complete(postList)
                    }
                  }
                } ~
                path("by-location" / Segment) { location =>
                  get {
                    onSuccess(getPostsFilteredAndSorted(offset, limit, PostByLocationNameClassifier(location), comparePostsByDate)) { postList =>
                      complete(postList)
                    }
                  }
                } ~
                path("") { //default behaviour
                  get {
                    onSuccess(getPostBySlice(offset, limit, comparePostsByDate)) { postList =>
                      complete(postList)
                    }
                  }
                }
            }
        } ~
        pathPrefix("locations") {
          path("all") {
            get {
              onSuccess(getLocations(AllLocationsFilter())) { locations =>
                complete(locations)
              }
            }

          }
        }
    }
  } ~
    pathPrefix("postassets") {
      encodeResponse {
        getFromDirectory(entrydir)
      }
    } ~
    pathPrefix("") {
      encodeResponse {
        getFromDirectory(webdir)
      }
    } ~
    pathPrefix("") {
      encodeResponse {
        getFromFile(webdir + "/index.html")
      }
    }

  /**
    * authenticate user and create if not exists
    * @param auther
    * @return
    */
  def authenticated(auther: String => Route) = optionalCookie("user") { optUserId =>
    {
      optUserId match {
        case Some(userId) => auther(userId.value)
        case None =>
          val exp = Some(DateTime.now.+(10 * 365 * 86400 * 1000L)) //expires in ten years
          println(exp.get)
          val newUserId = java.util.UUID.randomUUID.toString
          setCookie(
            HttpCookie("user", value = newUserId, expires = exp, path = Some("/"), domain = Some("bastigram.de"))) {
            auther(newUserId)
          }
      }

    }

  }

}
