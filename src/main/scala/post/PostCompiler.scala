package post

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import post.PostCompiler.VariableMemory
import post.postentities.{PostEntity, PostEntityTrait}
import v2.model.CompiledPost
import v2.sources.PlainPostSource.PlainPost

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object PostCompiler {
  type VariableMemory = Map[String, PostEntityTrait]

  abstract class Instruction
  case class FromImportAsInst(from: String, imp: String, as: String) extends Instruction
  case class NopInst(content: String) extends Instruction

  case class VariableDeclaration(variable: String, statement: String) extends Instruction

  abstract class NextInstruction
  case object NextUnset extends NextInstruction
  case object NextFrom extends NextInstruction
  case object NextImport extends NextInstruction
  case object NextAs extends NextInstruction

  case class FromImportAsInstBuilder(nextInst: NextInstruction = NextUnset,
                                     from: String = "",
                                     imp: String = "",
                                     as: String = "") {
    def build(): FromImportAsInst = FromImportAsInst(from, imp, as)
  }

  case class InstructionsAndBody(instructions: Seq[Instruction], body: String)

  /**
    * parse line to instruction
    * @param f
    * @return
    */
  def lineToInstruction(f: String): Instruction = f match {
    case f if f.startsWith("#from") =>
      val seperated = f.tail.split(" ")
      seperated
        .foldLeft(FromImportAsInstBuilder()) {
          case (builder, fragment) =>
            fragment match {
              case "from"   => builder.copy(nextInst = NextFrom)
              case "import" => builder.copy(nextInst = NextImport)
              case "as"     => builder.copy(nextInst = NextAs)
              case _ =>
                builder.nextInst match {
                  case NextFrom   => builder.copy(from = fragment)
                  case NextImport => builder.copy(imp = fragment)
                  case NextAs     => builder.copy(as = fragment)
                }
            }
        }
        .build()

    case f if !f.startsWith("#") =>
      NopInst(f)

    case f if f.startsWith("#val") =>
      val variableAndDeclaration = f.stripPrefix("#val").split("=")

      val variable = variableAndDeclaration.head.replace(" ", "")
      val declaration = variableAndDeclaration.tail.mkString("=").split(" ").filter(_ != "").mkString(" ")
      VariableDeclaration(variable, declaration)
    case f if f.startsWith("#") =>
      ??? //TODO : IMPLEMENT FUNCTION CALL
    // abc(...
  }

  /**
    * convert an instruction to a variable
    * @param instruction
    * @param memory
    * @param ec
    * @return
    */
  def instructionToPostEntity(
      instruction: Instruction,
      memory: VariableMemory,
      postCache: String => Option[CompiledPost])(implicit ec: ExecutionContext): Future[(String, PostEntityTrait)] = {
    println(instruction)
    val entityMatcher = PostEntity.entityMatcherList.filter(_.matchPost(instruction)).head
    entityMatcher.postEntityFromInstruction(instruction, postCache)

  }

}

class PostCompiler()(implicit system: ActorSystem, ec: ExecutionContextExecutor, materializer: ActorMaterializer) {

  def compile(post: PlainPost, postCache: String => Option[CompiledPost]): Future[CompiledPost] = {

    val a: (VariableMemory, String) = (Map(), "")

    post.postBody
      .foldAsync(a) {
        case ((varMem, postBody), line) =>
          val newPostBody = postBody + "\n" + line
          val instruction = PostCompiler.lineToInstruction(line)
          PostCompiler.instructionToPostEntity(instruction, varMem, postCache).map {
            case (varName, postEntity) =>
              if (varMem.contains(varName)) {
                (varMem + (varName -> postEntity.memOverride(varMem(varName))), newPostBody)
              } else {
                (varMem + (varName -> postEntity), newPostBody)
              }

          }
      }
      .runWith(Sink.head)
      .map { case (mem, postBody) => CompiledPost(post.slug, postBody, mem) }

  }

}
