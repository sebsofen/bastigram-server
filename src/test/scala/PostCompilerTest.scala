import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import post.PostCompiler
import post.PostCompiler._
import post.postentities.{DummyPostEntity, PostBodyEntity}
import spec.BaseSpec
import v2.model.CompiledPost
import v2.sources.PlainPostSource.PlainPost

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
      val simpleInstructionToPost = instructionToPostEntity(simpleVariableDeclaration, Map(), postCache)
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

      val simpleInstructionToPost = instructionToPostEntity(importStatement, Map(), postCache)
      val result = Await.result(simpleInstructionToPost, Duration.Inf)
      result should equal(("mydummy", DummyPostEntity("'bla'")))

    }
  }

  "the postcompiler class " should {
    "compile a smple post" in {
      def postCache(string: String): Option[CompiledPost] = None
      val postCompiler = new PostCompiler()

      val simplePostString = Seq("#val dum = [dummy text='bla']", "Normaler post Text und #hashtags", "Nochne Zeile")

      val plainPost = PlainPost("dummy", Source.fromIterator(() => simplePostString.toIterator))
      val result =
        Await.result(postCompiler.compile(plainPost, postCache), Duration.Inf)

      val resMem =
        Map("dum" -> DummyPostEntity("'bla'"),
            "postBody" -> PostBodyEntity("Normaler post Text und #hashtags\nNochne Zeile"))
      val resPost = CompiledPost(plainPost.slug, simplePostString.mkString("\n"), resMem)
      result should equal(resPost)
    }
  }
}
