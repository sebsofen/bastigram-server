package actors

import akka.actor.{Actor, ActorRef}
import repo.EntriesReadRepo
import repo.EntriesReadRepo.{Entry, HashTag, HashTagStats}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future

object CacheActor {
  case class CacheActorRef(ref:ActorRef)

  implicit val timeout : Timeout = Timeout(4 seconds)
  case object EntriesList
  case object RebuildCache

  case class UpdateHashTags(tags: Map[EntriesReadRepo.HashTag, EntriesReadRepo.HashTagStats])
  case class UpdateEntryList(entries: Seq[Entry])

  case class GetEntryList()
  case class GetHashTagList()


  def askForEntries(actorRef: ActorRef) = (actorRef ? GetEntryList()).asInstanceOf[Future[Seq[Entry]]]

  def askForHashTags(actorRef: ActorRef) = (actorRef ? GetHashTagList()).asInstanceOf[Future[Map[HashTag, HashTagStats]]]


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
