package actors.likes

import actors.likes.LikesBus.UserPostLike
import actors.likes.UserLikesActor.{GetLiked, LikeRecord, UserLikeNoRecord}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Success

/**
  * Created by sebastian on 7/25/17.
  */
class UserLikesMasterActor(bus: LikesBus)(implicit system: ActorSystem,
                           _ec: ExecutionContextExecutor,
                           materializer: ActorMaterializer)
    extends Actor {
  import UserLikesMasterActor._
  implicit val timeout = Timeout(5 seconds)
  val userLikesActors = mutable.Map[String, ActorRef]()
  override def receive: Receive = {

    case NewUser(userId) if !userLikesActors.contains(userId) =>
      val userLikesActorRef = system.actorOf(UserLikesActor.props(userId,bus))
      userLikesActors += userId -> userLikesActorRef

    case SetUserLikesPost(userId, postSlug, like) =>
      val userLikesActor = userLikesActors.get(userId) match {
        case Some(user) =>
          user
        case None =>
          val userLikesActorRef = system.actorOf(UserLikesActor.props(userId,bus))
          userLikesActors += userId -> userLikesActorRef
          userLikesActorRef
      }

      bus.publish(UserPostLike(userId,postSlug,like))



    case GetUserLikesPost(userId, postSlug) if userLikesActors.contains(userId) =>
      val s = sender()
      (userLikesActors(userId) ? GetLiked(postSlug)).mapTo[LikeRecord].onComplete {
        case Success(likeRecord) => s ! likeRecord
      }
    case GetUserLikesPost(userId, postSlug) if !userLikesActors.contains(userId) =>
      sender() ! UserLikeNoRecord()

  }
}

object UserLikesMasterActor {
  case class UserLikesMasterActorRef(ref: ActorRef)
  case class NewUser(id: String)
  case class GetUserLikesPost(userId: String, postSlug: String)
  case class SetUserLikesPost(userId: String, postSlug: String, like: LikeRecord)

  def props(bus: LikesBus)(implicit system: ActorSystem, _ec: ExecutionContextExecutor, materializer: ActorMaterializer): Props =
    Props(new UserLikesMasterActor(bus: LikesBus))
}
