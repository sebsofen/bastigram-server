package actors.likes


import actors.likes.LikesBus.UserPostLike
import actors.likes.UserLikesActor.{UserDisLikeRecord, UserLikeRecord}
import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Created by sebastian on 7/25/17.
  */
class PostLikesMasterActor(bus: LikesBus) extends Actor {
  import PostLikesMasterActor._
  val postsLikes = mutable.Map[String, Int]()

  override def receive: Receive = {

    case UserPostLike(user, post, like) =>
      val likeIncr = like match {
        case UserLikeRecord()    => 1
        case UserDisLikeRecord() => -1
      }

      postsLikes += post -> (postsLikes.getOrElse(post, 0) + likeIncr)

    case GetLikesPost(postSlug: String) => sender() ! postsLikes.getOrElse(postSlug, 0)

  }
}

object PostLikesMasterActor {
  import akka.pattern.ask

  import scala.concurrent.duration._
  implicit val timeout = Timeout(5 seconds)
  /**
    * get number of likesfor post
    * @param slug
    */
  case class GetLike(slug: String)

  def askGetLikes(lActor: PostLikesMasterActorRef, gLike: GetLikesPost): Future[Int] = (lActor.ref ? gLike).mapTo[Int]

  case class PostLikesMasterActorRef(ref: ActorRef)
  def props(bus: LikesBus): Props = Props(new PostLikesMasterActor(bus: LikesBus))

  case class GetLikesPost(postSlug: String)
}
