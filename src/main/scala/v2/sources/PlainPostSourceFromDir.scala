package v2.sources

import java.nio.file.{Files, Paths}

import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import com.typesafe.config.Config

class PlainPostSourceFromDir(settings: PlainPostSourceFromDirSettings) extends PlainPostSource {
  override def postSource(): Source[PlainPostSource.PlainPost, NotUsed] =
    Directory
      .ls(Paths.get(settings.POSTS_DIR))
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

case class PlainPostSourceFromDirSettings(config: Config) {
  val M_CFG = config.getConfig("plainpostsource")
  val POSTS_DIR = M_CFG.getString("dir")
}
