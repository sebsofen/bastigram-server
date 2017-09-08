package v2.sources

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.Source

import scala.concurrent.Future

trait PlainPostSource {
  import PlainPostSource._

  def postSource(): Source[PlainPost, NotUsed]




}

object PlainPostSource {
  //todo: Future[IOResult] should be replaced with NotUsed
  case class PlainPost(slug: String, postBody: Source[String,Future[IOResult]])
}
