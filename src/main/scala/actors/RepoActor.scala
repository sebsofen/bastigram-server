package actors

import actors.likes.PostLikesMasterActor
import actors.likes.PostLikesMasterActor.{GetLikesPost, PostLikesMasterActorRef}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import repo.EntriesReadRepo.{Entry, EntryLocation, HashTag}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

trait EntryEnrichment {
  val _likesActor: PostLikesMasterActorRef

  def enrichEntryWithLikes(entry: Entry)(implicit _ec: ExecutionContext): Future[Entry] = {
    PostLikesMasterActor.askGetLikes(_likesActor, GetLikesPost(entry.slug)).map { likes =>
      entry.copy(likes = likes)
    }
  }

  def enrichEntryWithLikes(entryOpt: Option[Entry])(implicit _ec: ExecutionContext): Future[Option[Entry]] = {
    entryOpt match {
      case None    => Future.successful(None)
      case Some(a) => enrichEntryWithLikes(a).map(Some(_))
    }
  }

  def enrichEntriesWithLikes(seq: Seq[Entry])(implicit _ec: ExecutionContext): Future[Seq[Entry]] = {
    Future.sequence(seq.map(enrichEntryWithLikes))
  }
}

object RepoActor {

  case class RepoActorRef(ref: ActorRef)

  //possible msgs:
  case class EntryBySlug(slug: String)
  case class EntryListSliceByDate(complete: List[Entry] => Unit, startPost: Int, limit: Int)
  case class HashTagsBySearchString(complete: List[HashTag] => Unit, searchString: String)
  case class EntriesByLocation(complete: List[Entry] => Unit, location: EntryLocation)
  case class EntriesByTag(complete: List[Entry] => Unit, tag: String)

  case class EntriesBySearchString(complete: List[Entry] => Unit, searchString: String)

  case class GetAllLocations(complete: List[EntryLocation] => Unit)

  def props(cacheActorRef: ActorRef, likesActor: PostLikesMasterActorRef)(implicit system: ActorSystem,
                                                           _ec: ExecutionContextExecutor,
                                                           materializer: ActorMaterializer): Props = {
    Props(new RepoActor(cacheActorRef, likesActor))
  }

}

class RepoActor(cacheActorRef: ActorRef, likesActor: PostLikesMasterActorRef)(implicit system: ActorSystem,
                                                               _ec: ExecutionContextExecutor,
                                                               materializer: ActorMaterializer)
    extends Actor
    with EntryEnrichment {
  import RepoActor._
  implicit private val timeout: Timeout = 10 seconds

  override val _likesActor: PostLikesMasterActorRef = likesActor

  override def receive: Receive = {

    case EntryBySlug(slug) =>
      val routeSender = sender()
      CacheActor
        .askForEntries(cacheActorRef)
        .map(f => f.find(_.slug == slug))
        .flatMap(enrichEntryWithLikes)
        .onComplete {
          case Success(a) =>
            routeSender ! a
          case _ =>
            routeSender ! None

        }

    case EntriesBySearchString(complete, searchString) =>
      CacheActor.askForEntries(cacheActorRef).map(f => f.filter(p => p.body.text.contains(searchString))).onComplete {
        case Success(a) => complete(a.toList)
      }

    case EntryListSliceByDate(com, start, limit) =>
      CacheActor
        .askForEntries(cacheActorRef)
        .map(f => f.slice(start, start + limit))
        .flatMap(enrichEntriesWithLikes)
        .onComplete {
          case Success(a) => com(a.toList)
        }

    case EntriesByLocation(com, loc) =>
      CacheActor
        .askForEntries(cacheActorRef)
        .map(f => f.filter(_.location == loc))
        .flatMap(enrichEntriesWithLikes)
        .onComplete {
          case Success(a) =>
            println(a)
            com(a.toList)
        }

    case EntriesByTag(com, tag) =>
      println(tag)
      CacheActor
        .askForEntries(cacheActorRef)
        .map(f => {
          val hTag = HashTag(tag)
          f.filter(_.hashTags.contains(hTag))
        })
        .flatMap(enrichEntriesWithLikes)
        .onComplete {
          case Success(a) =>
            println(a)
            com(a.toList)
        }

    case HashTagsBySearchString(com, searchString) =>
      val filteredTagsFut = CacheActor
        .askForHashTags(cacheActorRef)
        .map(
          tagSeqs =>
            tagSeqs
              .map(f => f._1)
              .toSeq
              .map(tag => (levensthein(tag.tag, searchString).toDouble / (tag.tag.length + searchString.length), tag))
              .sortBy(_._1)
              .take(10))

      filteredTagsFut.onComplete {
        case Success(filteredTags) => com(filteredTags.map(f => f._2).toList)
        case Failure(s)            => s.printStackTrace()
      }

  }

  def levensthein(a: String, b: String): Int = {
    import scala.math.min
    ((0 to b.size).toList /: a)((prev, x) =>
      (prev zip prev.tail zip b).scanLeft(prev.head + 1) {
        case (h, ((d, v), y)) => min(min(h + 1, v + 1), d + (if (x == y) 0 else 1))
    }) last

  }

}
