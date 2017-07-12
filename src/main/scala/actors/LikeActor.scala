package actors

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Created by sebastian on 07.07.17.
  */
class LikeActor extends Actor {
  import LikeActor._

  //TODO: Read and write likes from persistent storage
  //TODO: Add user authentication!
  val likes: mutable.Map[String, Int] = mutable.Map()

  override def receive: Receive = {

    case sLike: SendLike =>
      likes += sLike.slug -> (likes.getOrElse(sLike.slug, 0) + 1)

    case dLike: DisLike =>
      likes += dLike.slug -> (likes.getOrElse(dLike.slug, 1) - 1)

    case gLike: GetLike =>
      sender() ! likes.getOrElse(gLike.slug, 0)
  }
}

object LikeActor {
  import scala.concurrent.duration._
  import akka.pattern.ask
  implicit val timeout = Timeout(5 seconds)

  /**
    * put like to page
    * @param slug
    */
  case class SendLike(slug: String)

  /**
    * get number of likesfor post
    * @param slug
    */
  case class GetLike(slug: String)

  /**
    * remove like from post
    * @param slug
    */
  case class DisLike(slug: String)

  def askGetLikes(lActor: ActorRef, gLike: GetLike): Future[Int] = (lActor ? gLike).mapTo[Int]

}
