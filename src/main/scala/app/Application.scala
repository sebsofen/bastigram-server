package app

import actors.{CacheActor, LikeActor, RepoActor}
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

  val likesActor = system.actorOf(Props[LikeActor], name = "likesActor")

  val repoActor = system.actorOf(Props[RepoActor](RepoActor.Props(cacheActor,likesActor)), name = "repoActor")


  //a.onSuccess {
  //  case (a) => println("test123" + a)
 // }
  val entriesRepo = new EntriesReadRepo(cacheActor)

  system.scheduler.schedule(0 second,Conf.REBUILDINTERVAL seconds, entriesRepo.cacheRebuildActor, RebuildCache())


  case class RebuildCache()

  val router = new Route(repoActor,likesActor)

  Http().bindAndHandle(router.route,
                       Conf.INTERFACE.interface,
                       Conf.INTERFACE.port)

}
