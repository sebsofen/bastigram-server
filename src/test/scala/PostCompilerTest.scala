import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import post.PostCompiler._
import post.postentities.DummyPostEntity
import spec.BaseSpec
import v2.model.CompiledPost

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class PostCompilerTest extends BaseSpec {

  implicit val ec = ExecutionContext.global
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  "the lineToInstruction function " should {

    "parse from x import y as z statement" in {
      lineToInstruction("#from x import y as z") should equal(FromImportAsInst("x", "y", "z"))
    }

    "parse nop statement" in {
      lineToInstruction("blabla") should equal(NopInst("blabla"))
    }

    "parse variable declaration statement" in {
      lineToInstruction("#val x = [gps file='test']") should equal(VariableDeclaration("x", "[gps file='test']"))
    }

  }

  "the instructions to variables functions" should {
    "create a  variable from an instruction" in {

      def postCache(string: String): Option[CompiledPost] = None

      val dummyText = "[dummy text='bla']"
      val simpleVariableDeclaration = VariableDeclaration("dummy", dummyText)
      val simpleInstructionToPost = instructionToPostEntity(simpleVariableDeclaration, Map(), postCache, "")
      val result = Await.result(simpleInstructionToPost, Duration.Inf)

      result should equal(("dummy", DummyPostEntity("'bla'")))

    }

    "create a variable that is importet from another post" in {
      //read post
      val compiledPost = CompiledPost("dummy", "123", Map("dummy" -> DummyPostEntity("'bla'")))
      def postCache(string: String): Option[CompiledPost] = string match {
        case "dummy" => Some(compiledPost)
        case _       => None
      }

      val importStatement = FromImportAsInst("dummy", "dummy", "mydummy")

      val simpleInstructionToPost = instructionToPostEntity(importStatement, Map(), postCache, "")
      val result = Await.result(simpleInstructionToPost, Duration.Inf)
      result should equal(("mydummy", DummyPostEntity("'bla'")))

    }
  }


}
