package v2.actors

import actors.likes.LikesBus.UserPostLike
import actors.likes.UserLikesActor.{LikeRecord, UserDisLikeRecord, UserLikeNoRecord, UserLikeRecord}
import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import post.PostCompiler
import v2.actors.PostLikesReadActor.{GetPostLikeCounts, GetUserLikePost}

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

object PostLikesReadActor {

  case class GetUserLikePost(userid: String, postSlug: String)
  case class GetPostLikeCounts(postSlug: String)

  def props()(implicit system: ActorSystem, _ec: ExecutionContextExecutor, materializer: ActorMaterializer): Props =
    Props(new PostLikesReadActor())
}

class PostLikesReadActor()(implicit system: ActorSystem,
                           _ec: ExecutionContextExecutor,
                           materializer: ActorMaterializer)
    extends Actor {
  val logger = Logger(classOf[PostCompiler])
  val getUserLikePostBuffer = mutable.HashMap[(String, String), LikeRecord]()
  val getPostCountBuffer = mutable.HashMap[String, Set[String]]()
  override def receive: Receive = {
    case upl: UserPostLike =>
      getUserLikePostBuffer += ((upl.userId, upl.postSlug) -> upl.like)
      upl.like match {
        case UserLikeRecord() =>
          val set = getPostCountBuffer.getOrElse(upl.postSlug, Set())
          getPostCountBuffer += (upl.postSlug -> (set + upl.userId))
        case UserDisLikeRecord() =>
          val set = getPostCountBuffer.getOrElse(upl.postSlug, Set())
          getPostCountBuffer += (upl.postSlug -> (set - upl.userId))
      }

    case GetPostLikeCounts(postSlug) =>
      sender() ! getPostCountBuffer.getOrElse(postSlug, Set()).size

    case GetUserLikePost(userid, postSlug) =>
      logger.debug("getuserlike " + userid + postSlug)
      sender() ! getUserLikePostBuffer.getOrElse((userid, postSlug), UserLikeNoRecord())

  }
}
