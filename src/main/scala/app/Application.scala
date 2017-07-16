package app

import actors.CacheActor.CacheActorRef
import actors.RepoActor.RepoActorRef
import actors.SearchActor.SearchActorRef
import actors.{CacheActor, LikeActor, RepoActor, SearchActor}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import repo.EntriesReadRepo

import scala.concurrent.duration._

object Application extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit private val timeout: Timeout = 10 seconds

  val cacheActor = system.actorOf(Props[CacheActor], name = "cacheActor")
  val cacheActorRef = CacheActorRef(cacheActor)

  val likesActor = system.actorOf(Props[LikeActor], name = "likesActor")

  val repoActor = system.actorOf(RepoActor.props(cacheActor,likesActor), name = "repoActor")
  val repoActorRef = RepoActorRef(repoActor)

  val searchActor = SearchActorRef(system.actorOf(SearchActor.props(cacheActorRef), name = "searchActor"))

  //a.onSuccess {
  //  case (a) => println("test123" + a)
 // }
  val entriesRepo = new EntriesReadRepo(cacheActor)

  system.scheduler.schedule(0 second,Conf.REBUILDINTERVAL seconds, entriesRepo.cacheRebuildActor, RebuildCache())


  case class RebuildCache()

  val router = new Route(repoActor,likesActor,searchActor )

  Http().bindAndHandle(router.route,
                       Conf.INTERFACE.interface,
                       Conf.INTERFACE.port)

}
