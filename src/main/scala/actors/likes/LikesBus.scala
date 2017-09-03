package actors.likes

import actors.likes.LikesBus.UserPostLike
import actors.likes.UserLikesActor.LikeRecord
import akka.actor.ActorRef
import akka.event.{EventBus, ScanningClassification}

/**
  * Created by sebastian on 7/28/17.
  */
class LikesBus extends EventBus with ScanningClassification {
  override type Event = UserPostLike
  override type Classifier = LikesClassifier
  override type Subscriber = ActorRef

  override protected def compareClassifiers(a: LikesClassifier, b: LikesClassifier): Int =
    a.toString compareTo b.toString

  override protected def compareSubscribers(a: ActorRef, b: ActorRef): Int = a.toString compareTo b.toString

  override protected def matches(classifier: LikesClassifier, event: UserPostLike): Boolean =
    classifier.classify(event)

  override protected def publish(event: UserPostLike, subscriber: ActorRef): Unit = subscriber ! event
}

object LikesBus {
  case class UserPostLike(userId: String, postSlug: String, like: LikeRecord)
}

trait LikesClassifier {
  def classify(ulp: UserPostLike): Boolean
}

class AllLikesClassifier extends LikesClassifier {
  override def classify(ulp: UserPostLike): Boolean = true
}

class UserLikesClassifier(userId: String) extends LikesClassifier {
  override def classify(ulp: UserPostLike): Boolean = ulp.userId == userId
}