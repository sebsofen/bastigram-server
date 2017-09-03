import post.PostCompiler
import spec.BaseSpec

class PostCompilerTest extends BaseSpec{

  "the lineToInstruction function " should {

    "parse from x import y as z statement" in {
      PostCompiler.lineToInstruction("#from x import y as z") should equal(PostCompiler.FromImportAsInst("x","y","z"))
    }

    "parse nop statement" in {
      PostCompiler.lineToInstruction("blabla") should equal(PostCompiler.NopInst("blabla"))
    }

  }

}
