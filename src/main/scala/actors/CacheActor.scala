package actors

import akka.actor.Actor
import repo.EntriesReadRepo
import repo.EntriesReadRepo.Entry

object CacheActor {
  case object EntriesList
  case object RebuildCache

  case class UpdateHashTags(tags: Map[EntriesReadRepo.HashTag, EntriesReadRepo.HashTagStats])
  case class UpdateEntryList(entries: Seq[Entry])

  case class GetEntryList()
  case class GetHashTagList()
}

class CacheActor() extends Actor {
  import CacheActor._

  //cached objects
  var entriesList = Seq[Entry]()
  var hashTags = Map[EntriesReadRepo.HashTag, EntriesReadRepo.HashTagStats]()

  override def receive: Receive = {

    case UpdateEntryList(entries) =>
      entriesList = entries.sortBy(_.created)

    case UpdateHashTags(tags) =>
      hashTags = tags

    case GetEntryList()   => sender ! entriesList
    case GetHashTagList() => sender ! hashTags



  }

}
