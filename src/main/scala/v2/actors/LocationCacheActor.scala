package v2.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import de.bastigram.model.CompiledPost
import de.bastigram.post.postentities.MapPostEntity
import v2.actors.LocationCacheActor.GetLocations
import v2.filters.LocationFilter

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

object LocationCacheActor {
  case class GetLocations(filter:LocationFilter)

  def props()(implicit system: ActorSystem, _ec: ExecutionContextExecutor, materializer: ActorMaterializer): Props =
    Props(new LocationCacheActor())
}
class LocationCacheActor(implicit system: ActorSystem, _ec: ExecutionContextExecutor, materializer: ActorMaterializer)
    extends Actor {
  val locationSet: mutable.Set[MapPostEntity] = mutable.Set()
  override def receive: Receive = {
    case post: CompiledPost if post.getLocation().isDefined =>
      locationSet += post.getLocation().get

    case GetLocations(filter) =>
      sender() ! locationSet.toSeq.filter(filter.filter)

  }

}
