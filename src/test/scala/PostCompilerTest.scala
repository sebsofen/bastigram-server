import post.PostCompiler._
import post.postentities.DummyPostEntity
import spec.BaseSpec

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class PostCompilerTest extends BaseSpec {

  implicit val ec = ExecutionContext.global
  "the lineToInstruction function " should {

    "parse from x import y as z statement" in {
      lineToInstruction("#from x import y as z") should equal(FromImportAsInst("x", "y", "z"))
    }

    "parse nop statement" in {
      lineToInstruction("blabla") should equal(NopInst("blabla"))
    }

    "parse variable declaration statement" in {
      lineToInstruction("#val x = [gps file='test']") should equal(VariableDeclaration("x", " [gps file='test']"))
    }

  }

  "the instructions to variables functions" should {
    "create a  variable from an instruction" in {

      val dummyText = "[dummy text='bla']"
      val simpleVariableDeclaration = VariableDeclaration("dummy", dummyText)
      val simpleInstructionToPost = instructionToPostEntity(simpleVariableDeclaration, Map())
      val result = Await.result(simpleInstructionToPost, Duration.Inf)

      result should equal(("dummy", DummyPostEntity(dummyText)))

    }

    "create a variable that is importet from another post" should {
      //read post

    }
  }

}
