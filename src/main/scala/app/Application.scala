package app

import actors.CacheActor.CacheActorRef
import actors.RepoActor.RepoActorRef
import actors.SearchActor.SearchActorRef
import actors.likes.PostLikesMasterActor.PostLikesMasterActorRef
import actors.likes.{LikesBus, PostLikesMasterActor, UserLikesMasterActor}
import actors.likes.UserLikesMasterActor.UserLikesMasterActorRef
import actors.{CacheActor, RepoActor, SearchActor}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import repo.EntriesReadRepo

import scala.concurrent.duration._

object Application extends RouterTrait {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  override implicit val ec = system.dispatcher

  override implicit val timeout: Timeout = 10 seconds


  val cacheActor = system.actorOf(Props[CacheActor], name = "cacheActor")
  val cacheActorRef = CacheActorRef(cacheActor)




  val repoActorRef = RepoActorRef(repoActor)

  override val searchActor = SearchActorRef(system.actorOf(SearchActor.props(cacheActorRef), name = "searchActor"))


  val likesBus = new LikesBus
  override val likesActor = PostLikesMasterActorRef(system.actorOf(PostLikesMasterActor.props(likesBus)))

  override val repoActor = system.actorOf(RepoActor.props(cacheActor, likesActor), name = "repoActor")

  override val userLikesMasterActor: UserLikesMasterActor.UserLikesMasterActorRef =
    UserLikesMasterActorRef(system.actorOf(UserLikesMasterActor.props(likesBus)))

  val entriesRepo = new EntriesReadRepo(cacheActor)

  system.scheduler.schedule(0 second, Conf.REBUILDINTERVAL seconds, entriesRepo.cacheRebuildActor, RebuildCache())

  case class RebuildCache()

  Http().bindAndHandle(route, Conf.INTERFACE.interface, Conf.INTERFACE.port)

}
