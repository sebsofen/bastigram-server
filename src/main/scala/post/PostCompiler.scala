package post

import akka.NotUsed
import akka.stream.scaladsl.Source
import repo.EntriesReadRepo.EntryBody

import scala.concurrent.Future

object PostCompiler {

  abstract class Instruction
  case class FromImportAsInst(from: String, imp: String, as: String) extends Instruction
  case class NopInst(content: String) extends Instruction

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
  def lineToInstruction(f: String) : Instruction = f match {
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
  }
}

class PostCompiler() {




  def compile(body: Source[String, NotUsed]): Future[EntryBody] = {

    body.map(PostCompiler.lineToInstruction)
    ???
  }



}
