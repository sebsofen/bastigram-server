package post.postentities

import post.PostCompiler
import v2.model.CompiledPost

import scala.concurrent.Future

trait PostEntityTrait {
  def typeDesc(): String

  def memOverride(old: PostEntityTrait): PostEntityTrait = this

  /**
    * merge two post entity traits
    * @param pet
    * @return
    */
  def +(pet: PostEntityTrait): PostEntityTrait
}

trait PostEntityTraitMatcher {

  def matchPost(matchInstruction: PostCompiler.Instruction): Boolean
  def postEntityFromInstruction(matchInstruction: PostCompiler.Instruction,
                                postCache: String => Option[CompiledPost],postSlug: String): Future[(String, PostEntityTrait)]
}

/**
  * listing of possible post entity variable declarations
  */
case object PostEntity {
  val entityMatcherList: Seq[PostEntityTraitMatcher] =
    Seq(DummyPostEntity, ImportStatementPostEntity, PostBodyEntity, ListPostEntity, ImagePostEntity, DatePostEntity, MapPostEntity)

  /**
    * TODO: might be better placed in own trait
    * @param str
    * @return
    */
  def strToArgMap(str: String): Map[String, String] = {
    str
      .stripPrefix("[")
      .stripSuffix("]")
      .split(" ")
      .filter(p => p.split("=").length == 2)
      .map { s =>
        val argVal = s.split("=")
        argVal(0).toLowerCase() -> argVal(1)
      }
      .toMap
  }

  def strToArgList(str: String): Array[String] = {
    str
      .stripPrefix("[")
      .stripSuffix("]")
      .split(" ")
      .filter(_ != "")
  }

}

abstract class PostException extends Exception
class InvalidOperandExeption extends PostException
class PostNotFoundException extends PostException
class MissingStatementException extends PostException
class StatementNotSupportedException extends PostException
