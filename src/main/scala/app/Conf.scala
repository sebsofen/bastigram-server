package app

import com.typesafe.config.ConfigFactory

/**
  * Created by sebastian on 20.06.17.
  */
object Conf {
  val config = ConfigFactory.load()

  lazy val INTERFACE = SrvInterface(config.getInt("interface.port"),config.getString("interface.interface"))
  lazy val ENTRYDIR = EntryDir(config.getString("repo.entries"))
  lazy val REBUILDINTERVAL = config.getInt("cache.rebuildInterval")
  lazy val WEBDIR = config.getString("repo.webdir")
  case class SrvInterface(port: Int, interface: String)
  case class EntryDir(dir: String)
}
