package post.postentities

import scala.concurrent.Future

case object DummyPostEntity extends PostEntityTraitMatcher {
  override def matchPost(matchString: String): Boolean = matchString.startsWith(prefix)

  override def postEntityFromInstruction(instruction: String): Future[PostEntityTrait] = {
    Future.successful(new DummyPostEntity(instruction))
  }

  override val prefix: String = "[dummy"
}

case class DummyPostEntity(text: String = "") extends PostEntityTrait {

  /**
    * merge two post entity traits
    *
    * @param pet
    * @return
    */
  override def +(pet: PostEntityTrait): PostEntityTrait = pet match {
    case p: DummyPostEntity =>
      DummyPostEntity(this.text + p.text)
    case _ =>
      throw new InvalidOperandExeption
  }
}
