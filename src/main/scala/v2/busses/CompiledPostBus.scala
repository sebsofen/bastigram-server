package v2.busses

import akka.actor.ActorRef
import akka.event.{EventBus, ScanningClassification}
import v2.busses.CompiledPostBus.CompiledPostClassifier
import v2.model.CompiledPost

class CompiledPostBus extends EventBus with ScanningClassification {
  override type Event = CompiledPost
  override type Classifier = CompiledPostClassifier
  override type Subscriber = ActorRef

  override protected def compareClassifiers(a: Classifier, b: Classifier): Int = a.toString().compareTo(b.toString())

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.toString().compareTo(b.toString())

  override protected def matches(classifier: Classifier, event: Event): Boolean = classifier.classify(event)

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event
}

object CompiledPostBus {
  trait CompiledPostClassifier {
    def classify(plainPost: CompiledPost): Boolean
  }

  case class AllCompiledPostClassifier() extends CompiledPostClassifier {
    override def classify(plainPost: CompiledPost): Boolean = true
  }
}
