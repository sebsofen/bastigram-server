package v2.actors

import akka.actor.Actor
import post.PostCompiler
import v2.model.CompiledPost
import v2.sources.PlainPostSource.PlainPost

import scala.collection.mutable
import scala.concurrent.Future

class PostCompilerActor(postCompiler: PostCompiler) extends Actor {

  //todo : add queue for post compilation
  //todo: remember to implement fail case handling

  val postsCache = new mutable.HashMap[String, CompiledPost]()
  override def receive: Receive = {
    case p: PlainPost =>
      val checkSum = p.postBody

      //first case: post exists in cache:
      postsCache.get(p.slug) match {
        case None                                      => //TODO: trigger post compilation
        case Some(cPost) if cPost.checkSum != checkSum =>
        //TODO: trigger post compilation

      }

  }

  def getPostBySlug(slug: String): Option[CompiledPost] = {
    postsCache.get(slug)
  }

  def compilePost(plainPost: PlainPost): Future[CompiledPost] = {

    ???
  }

}
