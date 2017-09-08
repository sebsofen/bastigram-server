package v2.sources

import java.nio.file.{Files, Path, Paths}

import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString

class PlainPostSourceFromDir(folder: Path) extends PlainPostSource {
  override def postSource(): Source[PlainPostSource.PlainPost, NotUsed] =
    Directory
      .ls(folder)
      .filter(Files.isDirectory(_))
      .map { p =>
        val readmeFile = Paths.get(p.toString, "readme")

        val postBodySource = FileIO
          .fromPath(readmeFile)
          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024, allowTruncation = true))
          .map(_.utf8String.toString)

        val slug = p.getFileName.toString

        PlainPostSource.PlainPost(slug, postBodySource)
      }
}
