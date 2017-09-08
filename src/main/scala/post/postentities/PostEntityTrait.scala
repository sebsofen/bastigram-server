package post.postentities

import v2.model.CompiledPost

import scala.concurrent.Future

trait PostEntityTrait {

  /**
    * merge two post entity traits
    * @param pet
    * @return
    */
  def +(pet: PostEntityTrait): PostEntityTrait
}

trait PostEntityTraitMatcher {

  /**
    * prefix should be everything up to first whitespace in declaration, like:
    * [dummy
    * [gps
    * [video
    */
  val prefix: String
  def matchPost(matchString: String): Boolean
  def postEntityFromInstruction(instruction: String, postCache: String => Option[CompiledPost]): Future[PostEntityTrait]
}

/**
  * listing of possible post entity variable declarations
  */
case object PostEntity {
  val entityMatcherList: Seq[PostEntityTraitMatcher] = Seq(DummyPostEntity)
  val entityMatcherMap: Map[String, PostEntityTraitMatcher] = entityMatcherList.map(f => f.prefix -> f).toMap
}

class InvalidOperandExeption extends Exception
