package repo

import java.nio.file.{Files, Path, Paths}

import actors.CacheActor.{UpdateEntryList, UpdateHashTags}
import akka.NotUsed
import akka.actor.ActorRef
import akka.stream._
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, Framing, GraphDSL, Merge, Sink, Source}
import akka.util.ByteString
import app.Conf

import scala.concurrent.ExecutionContext

object EntriesReadRepo {
  case class HashTag(tag: String)
  case class HashTagStats(count: Int = 0)
  case class EntryImage(path: String)
  case class EntryBody(text: String)
  case class EntryLocation(location: String)
  case class Entry(slug: String,
                   hashTags: List[HashTag],
                   images: List[EntryImage],
                   created: Long,
                   body: EntryBody,
                   location: Option[EntryLocation],
                   likes: Int = 0)

  sealed case class EntryBuilder(slug: Option[String],
                                 created: Option[Long],
                                 body: Option[EntryBody],
                                 location: Option[EntryLocation],
                                 images: List[EntryImage] = List(),
                                 hashTags: List[HashTag] = List()) {
    def toEntry: Entry = {
      Entry(slug = this.slug.get,
            hashTags = this.hashTags,
            images = this.images,
            created = this.created.get,
            body = this.body.get,
            location = this.location)
    }
  }
  case object EntryBuilder {
    def empty = EntryBuilder(None, None, None, None)
  }

  import java.text.SimpleDateFormat

  val parser = new SimpleDateFormat("dd.mm.yyyy")
}

class EntriesReadRepo(cacheActor: ActorRef)(implicit ec: ExecutionContext, mat: ActorMaterializer) {
  import repo.EntriesReadRepo._

  //TODO: idk which type to use at this point
  val graphFlow = Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    val in = builder.add(Flow[Entry])

    val entriesBCast = builder.add(Broadcast[Entry](2))
    val merge = builder.add(Merge[Any](2))
    import GraphDSL.Implicits._

    //HERE: start processing pipeline for read posts
    in ~> entriesBCast ~> entriesSeq ~> merge
    entriesBCast ~> hashTagCounter ~> merge

    FlowShape(in.in, merge.out)

  })

  def entriesSeq = Flow[Entry].grouped(Int.MaxValue).map(entries => cacheActor ! UpdateEntryList(entries))

  def hashTagCounter =
    Flow[Entry]
      .fold(Map[HashTag, HashTagStats]()) {
        case (statsMap, entry) =>
          entry.hashTags.foldLeft(statsMap) {
            case (map, hashTag) =>
              val stats = map.getOrElse(hashTag, HashTagStats())
              map + (hashTag -> stats.copy(count = stats.count + 1))
          }
      }
      .map(cacheActor ! UpdateHashTags(_))

  //TODO: THIS IS MISSING AN IMPLEMENTATION
  val locationCounter = Flow[Entry].map(f => f).to(Sink.seq)

  val entriesSource = Directory
    .ls(Paths.get(Conf.ENTRYDIR.dir))
    .filter(Files.isDirectory(_))
    .mapAsync(1) { p =>
      val readmeFile = Paths.get(p.toString, "readme")
      val imgs = pathToImages(p).map(f => f.map(f => EntryImage(f)))
      imgs.flatMap(f => {
        val entryBuilder = EntryBuilder.empty.copy(slug = Some(p.getFileName.toString)).copy(images = f.toList)

        fileToEntry(readmeFile, entryBuilder).runWith(Sink.head)
      })

    }

  def pathToImages(file: Path) =
    Directory
      .ls(file)
      .filter(Files.isRegularFile(_))
      .filter(f =>
        f.toFile.getName.toLowerCase.endsWith(".png") || f.toFile.getName.toLowerCase
          .endsWith(".jpg") || f.toFile.getName.toLowerCase.endsWith(".jpeg"))
      .runWith(Sink.seq)
      .map(f => f.sortBy(_.getFileName))
      .map(_.map(f => f.getFileName.toString))

  def likeFileToLikeCount(file: Path) =
    FileIO
      .fromPath(file)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024, allowTruncation = true))
      .map(_.utf8String)
      .fold(0) { //count number of likes
        case (likeCount, line) =>
          likeCount + line.split(" ")(1).toInt
      }

  def fileToEntry(file: Path, entryBuilder: EntryBuilder = EntryBuilder.empty) = {
    FileIO
      .fromPath(file)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024, allowTruncation = true))
      .map(_.utf8String)
      .fold(entryBuilder) {
        case (eB, line) =>
          line match {

            case line if line.startsWith("created=") && eB.created.isEmpty =>
              eB.copy(created = Some(parser.parse(line.replace("created=", "")).getTime))

            case line if line.startsWith("location=") && eB.location.isEmpty =>
              eB.copy(location = Some(EntryLocation(line.replace("location=", ""))))

            case line =>
              val hashTags =
                line.split(" ").filter(_.startsWith("#")).map(HashTag(_))
              eB.copy(hashTags = eB.hashTags ++ hashTags, body = Some({
                val body = eB.body.getOrElse(EntryBody(""))
                body.copy(text = body.text + line + "\n")
              }))
          }

      }
      .map(_.toEntry)
  }

  /**
    * send any Message to this actor, and it will trigger rebuild process
    */
  val cacheRebuildActor = Source
    .actorRef[Any](Int.MaxValue, OverflowStrategy.dropTail)
    .flatMapConcat { _ => //do all streaming processing in here
      entriesSource.via(graphFlow)
    }
    .to(Sink.ignore)
    .run()

}
