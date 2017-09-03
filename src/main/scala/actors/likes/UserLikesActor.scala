package actors.likes

import actors.likes.LikesBus.UserPostLike
import akka.actor.{Actor, Props}

import scala.collection.mutable

/**
  * Created by sebastian on 7/25/17.
  */
class UserLikesActor(userId: String, bus: LikesBus) extends Actor {
  import UserLikesActor._
  val likesMap = mutable.Map[String, LikeRecord]()
  bus.subscribe(self, new UserLikesClassifier(userId)) //TODO: MOVE OUT OF ACTOR?

  override def receive: Receive = {
    case GetLiked(slug) =>
      sender() ! likesMap.getOrElse(slug, UserLikeNoRecord)

    case SetLiked(slug) =>
      likesMap += slug -> UserLikeRecord()

    case SetDisliked(slug) =>
      likesMap += slug -> UserDisLikeRecord()

    case SetLikedClass(slug, liked) =>
      likesMap += slug -> liked

    case UserPostLike(user, post, like) =>
      likesMap += post -> like

  }
}

object UserLikesActor {
  def props(userId: String, bus: LikesBus): Props = Props(new UserLikesActor(userId, bus))

  abstract class LikeRecord
  case class UserLikeRecord() extends LikeRecord
  case class UserDisLikeRecord() extends LikeRecord
  case class UserLikeNoRecord() extends LikeRecord

  case class GetLiked(slug: String)
  case class SetLiked(slug: String)
  case class SetLikedClass(slug: String, liked: LikeRecord)
  case class SetDisliked(slug: String)
}
