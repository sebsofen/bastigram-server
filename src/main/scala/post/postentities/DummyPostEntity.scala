package post.postentities

import post.PostCompiler
import post.PostCompiler.VariableDeclaration
import v2.model.CompiledPost

import scala.concurrent.Future

case object DummyPostEntity extends PostEntityTraitMatcher {
  override def matchPost(matchInstruction: PostCompiler.Instruction): Boolean = {
    matchInstruction match {
      case VariableDeclaration(variable, statement) if statement.startsWith("[dummy") => true
      case _                                                                          => false
    }
  }

  override def postEntityFromInstruction(
      matchInstruction: PostCompiler.Instruction,
      postCache: (String) => Option[CompiledPost],postSlug: String): Future[(String, PostEntityTrait)] = {

    val inst = matchInstruction.asInstanceOf[VariableDeclaration]

    PostEntity.strToArgMap(inst.statement).get("text") match {
      case None       => Future.failed(new MissingStatementException)
      case Some(text) => Future.successful((inst.variable, DummyPostEntity(text)))
    }

  }
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

  override def typeDesc(): String = "DUMMY"
}
