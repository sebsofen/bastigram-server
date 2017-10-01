package v2.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import post.PostCompiler
import post.postentities.PostException
import v2.busses.CompiledPostBus
import v2.busses.CompiledPostBus.CompiledPostClassifier
import v2.model.CompiledPost
import v2.sources.PlainPostSource.PlainPost

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

object PostCompilerActor {
  def props(postCompiler: PostCompiler, compiledPostBus: CompiledPostBus)(implicit system: ActorSystem,
                                                                          _ec: ExecutionContextExecutor,
                                                                          materializer: ActorMaterializer): Props =
    Props(new PostCompilerActor(postCompiler, compiledPostBus))
}

class PostCompilerActor(postCompiler: PostCompiler, compiledPostBus: CompiledPostBus)(implicit system: ActorSystem,
                                                                                      _ec: ExecutionContextExecutor,
                                                                                      materializer: ActorMaterializer)
    extends Actor {

  val decider: Supervision.Decider = {
    case _: PostException => Supervision.Resume
    case _                => Supervision.Stop
  }

  val bufferSize = 100

  //if the buffer fills up then this strategy drops the oldest elements
  //upon the arrival of a new element.
  val overflowStrategy = akka.stream.OverflowStrategy.dropHead

  val queue = Source
    .queue[PlainPost](bufferSize, overflowStrategy)
    .mapAsync(1)(plainPost => postCompiler.compile(plainPost, postCache))
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
    .to(Sink.foreach { p =>
      postsCacheMap += p.slug -> p
      compiledPostBus.publish(p)
    })
    .run()

  val postsCacheMap = new mutable.HashMap[String, CompiledPost]()

  def postCache(slug: String): Option[CompiledPost] = postsCacheMap.get(slug)

  override def receive: Receive = {
    case p: PlainPost =>
      queue offer p

    case GetPostBySlug(slug) => sender() ! getPostBySlug(slug)

    case GetPostListSorted(offset, limit, comparer) =>
      sender() ! getPostListSorted(comparer).drop(offset).take(limit)
    case GetPostListFilteredAndSorted(offset, limit, filter, comparer) =>
      sender() ! getPostListSorted(comparer).filter(filter.classify).drop(offset).take(limit)

  }

  def getPostBySlug(slug: String): Option[CompiledPost] = postsCacheMap.get(slug)

  def getPostListSorted(comparer: (CompiledPost, CompiledPost) => Boolean): List[CompiledPost] =
    postsCacheMap.values.filter(_.originPost.listed).toList.sortWith {
      case (p1, p2) => comparer(p1, p2)
    }
}

case class GetPostBySlug(slug: String)
case class GetPostListSorted(offet: Int, limit: Int, comparer: (CompiledPost, CompiledPost) => Boolean)
case class GetPostListFilteredAndSorted(offet: Int,
                                        limit: Int,
                                        filter: CompiledPostClassifier,
                                        comparer: (CompiledPost, CompiledPost) => Boolean)


