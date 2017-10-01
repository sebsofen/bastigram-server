package v2.sources

import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Date

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.util.ByteString
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}

class PlainPostSourceFromDir(settings: PlainPostSourceFromDirSettings)(implicit system: ActorSystem,
                                                                       _ec: ExecutionContextExecutor,
                                                                       materializer: ActorMaterializer) extends PlainPostSource {

  val decider: Supervision.Decider = {
    case _ => Supervision.Resume

  }

  override def postSource(): Source[PlainPostSource.PlainPost, NotUsed] =
    Directory
      .ls(Paths.get(settings.POSTS_DIR))
      .filter(Files.isDirectory(_))
      .mapAsync(1) { p =>
        val releaseFile = Paths.get(p.toString, "release")

        if (releaseFile.toFile.exists()) {
          FileIO
            .fromPath(releaseFile)
            .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024, allowTruncation = true))
            .map(_.utf8String.toString)
            .runWith(Sink.head)
            .map { datestring =>
              val a = new SimpleDateFormat("dd.MM.yyyy") //TODO: not sure, if this is correct
              val now = new Date()
              val isReleased = a.parse(datestring).before(now)

              (isReleased, p)
            }

        } else {
          Future.successful((true, p));
        }

      }
      .filter(_._1)
      .map(_._2)
      .map { p =>
        val readmeFile = Paths.get(p.toString, "readme")
        val postBodySource = FileIO
          .fromPath(readmeFile)
          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024, allowTruncation = true))
          .map(_.utf8String.toString)
          .withAttributes(ActorAttributes.supervisionStrategy(decider))

        val slug = p.getFileName.toString

        val hiddenFile = Paths.get(p.toString, "hidden")

        PlainPostSource.PlainPost(slug, postBodySource, !hiddenFile.toFile.exists())
      }
}

case class PlainPostSourceFromDirSettings(config: Config) {
  val M_CFG = config.getConfig("plainpostsource")
  val POSTS_DIR = M_CFG.getString("dir")
}
