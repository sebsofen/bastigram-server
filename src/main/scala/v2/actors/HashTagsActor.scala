package v2.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import de.bastigram.model.CompiledPost
import v2.actors.HashTagsActor.{GetPostsForHashTag, SearchHashTag}
import v2.utils.TextProcessing

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

object HashTagsActor {

  def props()(implicit system: ActorSystem, ec: ExecutionContextExecutor, materializer: ActorMaterializer): Props =
    Props(new HashTagsActor)
  case class SearchHashTag(tag: String, limit: Int = 10)
  case class GetPostsForHashTag(tag:String)

}

class HashTagsActor()(implicit system: ActorSystem, ec: ExecutionContextExecutor, materializer: ActorMaterializer)
    extends Actor
    with TextProcessing {

  val buffer = mutable.HashMap[String, HashTagStats]()
  val hashTagAndPostsBuffer = mutable.HashMap[String, Set[String]]()



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


            val oldSet = hashTagAndPostsBuffer.getOrElse(hashTag,Set())
            hashTagAndPostsBuffer += hashTag -> (oldSet + p.slug)
          }
        case _ =>
      }

    case SearchHashTag(searchString, limit) =>
      sender() ! buffer.toList.sortBy { case (tag, stats) => levensthein(searchString, tag) }.take(limit).map {
        case (tag, stat) => HashTagAndStats(tag, stat)
      }
    case GetPostsForHashTag(tag) =>
      sender() ! hashTagAndPostsBuffer.getOrElse(tag,Set())
  }
}

case class HashTagAndStats(tag: String, stats: HashTagStats)

case class HashTagStats(occurences: Int) {
  def +(s: HashTagStats): HashTagStats = HashTagStats(s.occurences + occurences)

}
case object HashTagStatsBuilder {
  def emptyStats(): HashTagStats = HashTagStats(0)
}
