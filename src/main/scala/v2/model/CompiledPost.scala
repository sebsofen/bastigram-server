package v2.model

import post.PostCompiler.VariableMemory
import post.postentities.{DatePostEntity, PostBodyEntity}

import scala.util.Try

case class CompiledPost(slug: String, checkSum: String, memory: VariableMemory) {
  def getBody(): Option[PostBodyEntity] = {
    Try {
      memory("postBody").asInstanceOf[PostBodyEntity]
    }.toOption
  }

  def getCreated(): Long =
    Try {
      memory("created").asInstanceOf[DatePostEntity].timestamp
    }.toOption.getOrElse(0L)

}
