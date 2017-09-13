package v2.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import v2.busses.PlainPostBus
import v2.sources.PlainPostSource

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object PlainPostActor {
  def props(plainPostBus: PlainPostBus, plainPostSource: PlainPostSource)(implicit system: ActorSystem,
                                                                          _ec: ExecutionContextExecutor,
                                                                          materializer: ActorMaterializer): Props = {
    Props(new PlainPostActor(plainPostBus, plainPostSource))

  }
  case class Refresh()
}

class PlainPostActor(plainPostBus: PlainPostBus, plainPostSource: PlainPostSource, refreshOnCreation: Boolean = true)(
    implicit system: ActorSystem,
    _ec: ExecutionContextExecutor,
    materializer: ActorMaterializer)
    extends Actor {
  import PlainPostActor._

  //trigger self refresh on start
  if (refreshOnCreation) {
    self ! Refresh()
  }

  //TODO: make configurable
  system.scheduler.schedule(10 second, 10 seconds, self, Refresh())


  override def receive: Receive = {

    case Refresh() =>
      println("read files from file")
      plainPostSource.postSource().runForeach { plainPost =>
        //send out to bus?

        plainPostBus.publish(plainPost)

      }

  }
}

class PlainPostActorSettings(config: Config) {}
