package v2.actors

import actors.likes.LikesBus
import actors.likes.LikesBus.UserPostLike
import actors.likes.UserLikesActor.{UserDisLikeRecord, UserLikeNoRecord, UserLikeRecord}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import v2.actors.PostLikesReadActor.GetUserLikePost
import v2.actors.PostLikesWriteActor.{DisLikePost, LikePost, LikePostIfNotExists}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

object PostLikesWriteActor {
  case class LikePostIfNotExists(userId: String, postSlug: String)
  case class LikePost(userId: String, postSlug: String)
  case class DisLikePost(userId: String, postSlug: String)

  def props(likesBus: LikesBus, likesReadActor: ActorRef)(implicit system: ActorSystem,
                                                          _ec: ExecutionContextExecutor,
                                                          materializer: ActorMaterializer): Props =
    Props(new PostLikesWriteActor(likesBus, likesReadActor))

}

class PostLikesWriteActor(likesBus: LikesBus, likesReadActor: ActorRef)(implicit system: ActorSystem,
                                                                        _ec: ExecutionContextExecutor,
                                                                        materializer: ActorMaterializer)
    extends Actor {
  implicit val timeout: Timeout = 10 seconds
  override def receive: Receive = {

    case LikePostIfNotExists(userId, postSlug) =>
      (likesReadActor ? GetUserLikePost(userId, postSlug)).onSuccess {
        case UserLikeNoRecord() => likesBus.publish(UserPostLike(userId, postSlug, UserLikeRecord()))

      }

    case LikePost(userId, postSlug) =>
      likesBus.publish(UserPostLike(userId, postSlug, UserLikeRecord()))

    case DisLikePost(userId, postSlug) =>
      likesBus.publish(UserPostLike(userId, postSlug, UserDisLikeRecord()))

  }
}
