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
  val USER_LIKE_STR = "1"
  val USER_DISLIKE_STR = "0"
  def props(userId: String, bus: LikesBus): Props = Props(new UserLikesActor(userId, bus))

  abstract class LikeRecord
  case class UserLikeRecord() extends LikeRecord {
    override def toString() = USER_LIKE_STR
  }
  case class UserDisLikeRecord() extends LikeRecord {
    override def toString() = USER_DISLIKE_STR
  }
  case class UserLikeNoRecord() extends LikeRecord {
    override def toString() = ""
  }

  case class GetLiked(slug: String)
  case class SetLiked(slug: String)
  case class SetLikedClass(slug: String, liked: LikeRecord)
  case class SetDisliked(slug: String)

  case object LikeRecord {
    def props(like: String) = like match {
      case USER_LIKE_STR => UserLikeRecord()
      case USER_DISLIKE_STR => UserDisLikeRecord()
      case _ => UserLikeNoRecord()
    }
  }
}
