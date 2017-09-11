package v2.actors

import java.nio.file.Paths

import actors.likes.LikesBus.UserPostLike
import actors.likes.UserLikesActor.LikeRecord
import actors.likes.{AllLikesClassifier, LikesBus}
import akka.Done
import akka.actor.{Actor, ActorSystem}
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import akka.util.ByteString
import com.typesafe.config.Config
import v2.model.CompiledPost

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}


class PersistentPostLikesActor(likesBus: LikesBus, settings: PersistentPostLikesActorSettings)(
    implicit system: ActorSystem,
    _ec: ExecutionContextExecutor,
    materializer: ActorMaterializer)
    extends Actor {
  likesBus.subscribe(self, new AllLikesClassifier)
  type PostSlug = String
  type UserID = String
  val likesBuffer = new mutable.HashMap[PostSlug, HashMap[UserID, LikeRecord]]()

  system.scheduler.schedule(settings.SAVE_INTERVAL second, settings.SAVE_INTERVAL seconds, self, SaveLikesToDisk())

  //TODO: save likes state from time to time

  val bufferSize = 100

  //if the buffer fills up then this strategy drops the oldest elements
  //upon the arrival of a new element.
  val overflowStrategy = akka.stream.OverflowStrategy.dropHead
  val decider: Supervision.Decider = {
    case e: Exception =>
      println("ERROR IN PERSISTENTPOSTLIKESACTOR")
      e.printStackTrace()
      Supervision.Resume
  }

  val queue = Source
    .queue[CompiledPost](bufferSize, overflowStrategy)
    .flatMapConcat(p => readLikesFromFile(p.slug))
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
    .to(Sink.foreach(like => likesBus publish like))
    .run()

  val likesSaveQueue =
    Source
      .queue[PostSlug](bufferSize, overflowStrategy)
      .mapAsync(1) { case (slug) => saveLikesToFile(slug) }
      .to(Sink.ignore)
      .run()

  override def receive: Receive = {
    case post: CompiledPost => queue.offer(post)

    case like: UserPostLike =>
      val likesList = likesBuffer.getOrElse(like.postSlug, HashMap())
      likesBuffer += like.postSlug -> (likesList + (like.userId -> like.like))

    case SaveLikesToDisk() =>
      likesBuffer.keys.foreach(key => likesSaveQueue offer key)
  }

  def saveLikesToFile(slug: String): Future[Any] = {
    val file = Paths.get(settings.LIKES_DIR, slug)
    likesBuffer.get(slug) match {
      case Some(likes) =>
        Source
          .fromIterator(() => likes.toIterator)
          .map { case (id, like) => id + "," + like.toString + "\n" }
          .map(ByteString(_))
          .runWith(FileIO.toPath(file))

      case _ =>
        Future.successful(Done)
    }
  }

  def readLikesFromFile(slug: String): Source[UserPostLike, Any] = {
    val file = Paths.get(settings.LIKES_DIR, slug)

    FileIO
      .fromPath(file)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024, allowTruncation = true))
      .map(_.utf8String)
      .map { line =>
        val cols = line.split(",")
        UserPostLike(cols(0), slug, LikeRecord.props(cols(1)))
      }

  }
}

case class SaveLikesToDisk()

case class PersistentPostLikesActorSettings(config: Config) {
  val M_CONFIG = config.getConfig("likesstore")
  val LIKES_DIR = M_CONFIG.getString("dir")
  val SAVE_INTERVAL = M_CONFIG.getInt("savensecs")
}
