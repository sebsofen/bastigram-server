package v2.filters

import post.postentities.MapPostEntity

trait LocationFilter {
  def filter(mapPostEntity: MapPostEntity) : Boolean
}

case class AllLocationsFilter() extends LocationFilter {
  override def filter(mapPostEntity: MapPostEntity): Boolean = true
}
