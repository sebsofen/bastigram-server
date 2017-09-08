package post

import akka.NotUsed
import akka.stream.scaladsl.Source
import post.PostCompiler.VariableMemory
import post.postentities.{PostEntity, PostEntityTrait}
import repo.EntriesReadRepo.EntryBody
import v2.model.CompiledPost

import scala.concurrent.{ExecutionContext, Future}

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
      val declaration = variableAndDeclaration.tail.mkString("=")
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
    instruction match {
      case vd: VariableDeclaration =>
        val entityMatcher = PostEntity.entityMatcherList.filter(_.matchPost(vd.statement)).head
        entityMatcher.postEntityFromInstruction(vd.statement, postCache).map(pet => (vd.variable, pet))
    }

  }

}

class PostCompiler() {

  def compile(body: Source[String, NotUsed], postCache: String => Option[CompiledPost]): Future[EntryBody] = {

    val a: VariableMemory = Map()

    body.map(PostCompiler.lineToInstruction).foldAsync(a) {
      case (varMem, instruction) =>
        PostCompiler.instructionToPostEntity(instruction, varMem, postCache).map {
          case (varName, postEntity) =>
            varMem + (varName -> postEntity)
        }

    }

    ???
  }

}
