package app

import actors.likes.UserLikesActor.{LikeRecord, UserDisLikeRecord, UserLikeRecord}
import actors.likes.{AllLikesClassifier, LikesBus}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import post.PostCompiler
import v2.actors.HashTagsActor.SearchHashTag
import v2.actors.PostLikesReadActor.{GetPostLikeCounts, GetUserLikePost}
import v2.actors.PostLikesWriteActor.{DisLikePost, LikePost, LikePostIfNotExists}
import v2.actors._
import v2.busses.CompiledPostBus.AllCompiledPostClassifier
import v2.busses.PlainPostBus.AllPlainPostClassifier
import v2.busses.{CompiledPostBus, PlainPostBus}
import v2.model.CompiledPost
import v2.sources.{PlainPostSourceFromDir, PlainPostSourceFromDirSettings}

import scala.concurrent.Future
import scala.concurrent.duration._

object NewApp extends App with NewRoute {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = Timeout(4 seconds)

  val config = ConfigFactory.load()

  val applicationConfig = ApplicationConfig(config)

  override val webdir = applicationConfig.WEBDIR
  override val entrydir = applicationConfig.ENTRYDIR
  val compiledPostBus = new CompiledPostBus
  val plainPostBus = new PlainPostBus

  //compiled post actor
  val postCompiler = new PostCompiler()
  val postCompilerActor =
    system.actorOf(PostCompilerActor.props(postCompiler, compiledPostBus), name = "postCompilerActor")
  plainPostBus.subscribe(postCompilerActor, AllPlainPostClassifier())

  //likes actor
  val likesBus = new LikesBus()

  val postLikesReadActor = system.actorOf(PostLikesReadActor.props(), name = "postLikesReadActor")
  likesBus.subscribe(postLikesReadActor, new AllLikesClassifier())
  val postLikesWriteActor =
    system.actorOf(PostLikesWriteActor.props(likesBus, postLikesReadActor), name = "postLikesWriteActor")

  val persistentPostLikesActorSettings = PersistentPostLikesActorSettings(config)
  val persistentPostLikesActor = system.actorOf(
    PersistentPostLikesActor.props(likesBus, persistentPostLikesActorSettings),
    name = "persistentPostLikesActor")

  compiledPostBus.subscribe(persistentPostLikesActor, AllCompiledPostClassifier())

  //hashtags actor
  val hashTagsActor = system.actorOf(HashTagsActor.props(), name = "hashTagsActor")
  compiledPostBus.subscribe(hashTagsActor, AllCompiledPostClassifier())

  //plain post source settings
  val plainPostSourceFromDirSettings = new PlainPostSourceFromDirSettings(config)
  val plainPostSource = new PlainPostSourceFromDir(plainPostSourceFromDirSettings)
  val plainPostActor = system.actorOf(PlainPostActor.props(plainPostBus, plainPostSource), name = "plainPostActor")

  Http().bindAndHandle(route, applicationConfig.INTERFACE, applicationConfig.PORT)

  override def searchHashTag(searchString: String): Future[List[HashTagAndStats]] =
    (hashTagsActor ? SearchHashTag(searchString)).mapTo[List[HashTagAndStats]]

  override def getPostBySlug(slug: String): Future[Option[CompiledPost]] = {
    (postCompilerActor ? GetPostBySlug(slug)).mapTo[Option[CompiledPost]]
  }

  override def getPostBySlice(offset: Int,
                              limit: Int,
                              sorter: (CompiledPost, CompiledPost) => Boolean): Future[List[CompiledPost]] = {
    (postCompilerActor ? GetPostListSorted(offset, limit, sorter)).mapTo[List[CompiledPost]]

  }

  override def getLikeRecord(userId: String, postSlug: String): Future[Any] = {
    (postLikesReadActor ? GetUserLikePost(userId, postSlug))
  }

  override def setLikeRecord(userId: String, postSlug: String, like: LikeRecord) = {
    like match {
      case UserDisLikeRecord() => postLikesWriteActor ! DisLikePost(userId, postSlug)
      case UserLikeRecord()    => postLikesWriteActor ! LikePost(userId, postSlug)
    }

  }
  override def setLikeRecordIfNotExists(userId: String, postSlug: String) = {
    postLikesWriteActor ! LikePostIfNotExists(userId, postSlug)
  }

  override def getLikeCountBySlug(postSlug: String): Future[Int] = {
    (postLikesReadActor ? GetPostLikeCounts(postSlug)).mapTo[Int]
  }
}

case class ApplicationConfig(config: Config) {
  val INTERFACE_CFG = config.getConfig("interface")
  val INTERFACE = INTERFACE_CFG.getString("interface")
  val PORT = INTERFACE_CFG.getInt("port")
  val WEBDIR = config.getString("html.webdir")
  val ENTRYDIR = config.getString("html.entries")
}
