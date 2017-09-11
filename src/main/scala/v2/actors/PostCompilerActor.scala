package v2.actors

import actors.likes.UserLikesActor.UserLikeRecord
import akka.actor.{Actor, ActorSystem}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import post.PostCompiler
import post.postentities.PostException
import v2.busses.CompiledPostBus
import v2.model.CompiledPost
import v2.sources.PlainPostSource.PlainPost

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

class PostCompilerActor(postCompiler: PostCompiler, compiledPostBus: CompiledPostBus)(implicit system: ActorSystem,
                                                                                      _ec: ExecutionContextExecutor,
                                                                                      materializer: ActorMaterializer)
  extends Actor {



  val decider: Supervision.Decider = {
    case _: PostException => Supervision.Resume
    case _ => Supervision.Stop
  }

  val bufferSize = 100

  //if the buffer fills up then this strategy drops the oldest elements
  //upon the arrival of a new element.
  val overflowStrategy = akka.stream.OverflowStrategy.dropHead

  val queue = Source
    .queue[PlainPost](bufferSize, overflowStrategy)
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
    .mapAsync(1)(plainPost => postCompiler.compile(plainPost, postCache))
    .to(Sink.foreach { p =>
      postsCacheMap += p.slug -> p
      compiledPostBus.publish(p)
    })
    .run()

  val postsCacheMap = new mutable.HashMap[String, CompiledPost]()

  def postCache(slug: String): Option[CompiledPost] = postsCacheMap.get(slug)

  override def receive: Receive = {
    case p: PlainPost =>
      val checkSum = p.postBody

      //first case: post exists in cache:
      postsCacheMap.get(p.slug) match {
        case None => queue offer p
        case Some(cPost) if cPost.checkSum != checkSum => queue offer p
        case _ =>
      }

  }

  def getPostBySlug(slug: String): Option[CompiledPost] = postsCacheMap.get(slug)


}
