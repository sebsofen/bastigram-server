package v2.busses

import akka.actor.ActorRef
import akka.event.{EventBus, ScanningClassification}
import v2.busses.PlainPostBus.PlainPostClassifier
import v2.sources.PlainPostSource.PlainPost

class PlainPostBus extends EventBus with ScanningClassification {
  override type Event = PlainPost
  override type Classifier = PlainPostClassifier
  override type Subscriber = ActorRef

  override protected def compareClassifiers(a: Classifier, b: Classifier): Int = a.toString().compareTo(b.toString())

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.toString().compareTo(b.toString())

  override protected def matches(classifier: Classifier, event: Event): Boolean = classifier.classify(event)

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event
}

object PlainPostBus {
  trait PlainPostClassifier {
    def classify(plainPost: PlainPost): Boolean
  }

  case class AllPlainPostClassifier() extends PlainPostClassifier {
    override def classify(plainPost: PlainPost): Boolean = true
  }
}
