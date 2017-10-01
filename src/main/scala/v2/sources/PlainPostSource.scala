package v2.sources

import akka.NotUsed
import akka.stream.scaladsl.Source

trait PlainPostSource {
  import PlainPostSource._

  def postSource(): Source[PlainPost, NotUsed]




}

object PlainPostSource {
  //todo: Future[IOResult] should be replaced with NotUsed
  case class PlainPost(slug: String, postBody: Source[String,Any],listed: Boolean = true)
}
