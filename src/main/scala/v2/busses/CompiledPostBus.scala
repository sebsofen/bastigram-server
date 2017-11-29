package v2.busses

import akka.actor.ActorRef
import akka.event.{EventBus, ScanningClassification}
import de.bastigram.model.CompiledPost
import de.bastigram.post.postentities.MapPostEntity
import v2.busses.CompiledPostBus.CompiledPostClassifier

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

  case class PostByLocationClassifier(mapPostEntity: MapPostEntity) extends CompiledPostClassifier {
    override def classify(plainPost: CompiledPost): Boolean =
      plainPost.getLocation().isDefined && plainPost.getLocation().get.equals(mapPostEntity)
  }

  case class PostByLocationNameClassifier(name: String) extends CompiledPostClassifier {
    override def classify(compiledPost: CompiledPost): Boolean = {
      compiledPost.getLocation().isDefined && compiledPost.getLocation().get.name.get == name

    }

  }

  case class PostSlugSetClassifier(set: Set[String]) extends CompiledPostClassifier {
    override def classify(plainPost: CompiledPost): Boolean = set.contains(plainPost.slug)
  }
}
