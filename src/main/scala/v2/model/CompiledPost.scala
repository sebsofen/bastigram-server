package v2.model

import post.PostCompiler.VariableMemory
import post.postentities.{DatePostEntity, MapPostEntity, PostBodyEntity}
import v2.sources.PlainPostSource.PlainPost

import scala.util.Try

case class CompiledPost(slug: String, checkSum: String, memory: VariableMemory, originPost: PlainPost) {
  def getBody(): Option[PostBodyEntity] = {
    Try {
      memory("postBody").asInstanceOf[PostBodyEntity]
    }.toOption
  }

  def getCreated(): Long =
    Try {
      memory("created").asInstanceOf[DatePostEntity].timestamp
    }.toOption.getOrElse(0L)

  def getLocation(): Option[MapPostEntity] = Try { memory("location").asInstanceOf[MapPostEntity] }.toOption

}
