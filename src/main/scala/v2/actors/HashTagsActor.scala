package v2.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Supervision}
import v2.actors.HashTagsActor.SearchHashTag
import v2.model.CompiledPost
import v2.utils.TextProcessing

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

object HashTagsActor {

  def props()(implicit system: ActorSystem, ec: ExecutionContextExecutor, materializer: ActorMaterializer): Props =
    Props(new HashTagsActor)
  case class SearchHashTag(tag: String, limit: Int = 10)

}

class HashTagsActor()(implicit system: ActorSystem, ec: ExecutionContextExecutor, materializer: ActorMaterializer)
    extends Actor
    with TextProcessing {

  val buffer = mutable.HashMap[String, HashTagStats]()

  val decider: Supervision.Decider = {
    case _ => Supervision.Resume

  }

  val bufferSize = 1000

  //if the buffer fills up then this strategy drops the oldest elements
  //upon the arrival of a new element.
  val overflowStrategy = akka.stream.OverflowStrategy.dropHead


  override def receive: Receive = {
    case p: CompiledPost =>
      p.getBody() match {
        case Some(body) =>
          body.hashTags.foreach { hashTag =>
            //hashTagQueue offer hashTag
            val oldStats = buffer.getOrElse(hashTag, HashTagStatsBuilder.emptyStats())
            buffer += hashTag -> (HashTagStats(1) + oldStats)
          }
        case _ =>
      }

    case SearchHashTag(searchString, limit) =>
      sender() ! buffer.toList.sortBy { case (tag, stats) => levensthein(searchString, tag) }.take(limit).map {
        case (tag, stat) => HashTagAndStats(tag, stat)
      }
  }
}

case class HashTagAndStats(tag: String, stats: HashTagStats)

case class HashTagStats(occurences: Int) {
  def +(s: HashTagStats): HashTagStats = HashTagStats(s.occurences + occurences)

}
case object HashTagStatsBuilder {
  def emptyStats(): HashTagStats = HashTagStats(0)
}
