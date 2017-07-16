package actors

import actors.CacheActor.CacheActorRef
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import repo.EntriesReadRepo.{Entry, HashTag}

import scala.concurrent.ExecutionContextExecutor
import scala.util.Success

/**
  * Created by sebastian on 14.07.17.
  */
class SearchActor(cacheActor: CacheActorRef)(implicit system: ActorSystem,
                                             _ec: ExecutionContextExecutor,
                                             materializer: ActorMaterializer)
    extends Actor {
  import SearchActor._

  override def receive: Receive = {

    case FindStuff(com, searchstring) =>
      val searchResultFut = for {
        posts <- CacheActor
          .askForEntries(cacheActor.ref)
          .map(f => f.filter(p => p.body.text.contains(searchstring)).take(10))
        tags <- CacheActor
          .askForHashTags(cacheActor.ref)
          .map(f => f.filter(tag => tag._1.tag.contains(searchstring)).take(10))
      } yield {
        SearchResults(tags.map(_._1).toSeq, posts)
      }
      searchResultFut.onComplete {
        case Success(searchResult) =>
          com(searchResult)
      }
  }

}

object SearchActor {

  case class SearchActorRef(ref: ActorRef)

  case class FindStuff(complete: SearchResults => Unit, searchString: String)

  case class SearchResults(hashTags: Seq[HashTag], posts: Seq[Entry])

  def props(cacheActor: CacheActorRef)(implicit system: ActorSystem,
                                       _ec: ExecutionContextExecutor,
                                       materializer: ActorMaterializer): Props = { Props(new SearchActor(cacheActor)) }
}
