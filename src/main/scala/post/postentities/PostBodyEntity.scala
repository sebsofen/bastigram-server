package post.postentities
import post.PostCompiler
import post.PostCompiler.NopInst
import v2.model.CompiledPost

import scala.concurrent.Future

case object PostBodyEntity extends PostEntityTraitMatcher {
  override def matchPost(matchInstruction: PostCompiler.Instruction): Boolean = matchInstruction match {
    case x: NopInst => true
    case _          => false
  }

  override def postEntityFromInstruction(
      matchInstruction: PostCompiler.Instruction,
      postCache: (String) => Option[CompiledPost]): Future[(String, PostEntityTrait)] = matchInstruction match {
    case NopInst(content) =>  Future.successful(("postBody", PostBodyEntity(content)))

  }
}

case class PostBodyEntity(body: String) extends PostEntityTrait {
  val hashTags = body.split(" ").filter(_.startsWith("#"))
  //TODO: Add hashtags?!

  override def memOverride(old: PostEntityTrait): PostEntityTrait = old match {
    case oldBody: PostBodyEntity => this + oldBody
  }

  /**
    * merge two post entity traits
    *
    * @param pet
    * @return
    */
  override def +(pet: PostEntityTrait): PostEntityTrait = pet match {
    case PostBodyEntity(body) => PostBodyEntity(body + "\n" + this.body)
  }
}
