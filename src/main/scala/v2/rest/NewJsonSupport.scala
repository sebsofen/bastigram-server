package v2.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import post.PostCompiler.VariableMemory
import post.postentities._
import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsString, JsValue, RootJsonWriter}
import v2.actors.{HashTagAndStats, HashTagStats}
import v2.model.CompiledPost

trait NewJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val hashTagStatsFormat = jsonFormat1(HashTagStats)
  implicit val hashTagAndStatsFormat = jsonFormat2(HashTagAndStats)

  implicit val postBodyEntityFormat = jsonFormat(PostBodyEntity.apply(_), "body")
  implicit val imagePostEntityFormat = jsonFormat(ImagePostEntity.apply(_), "imgs")
  implicit val latLonFormat = jsonFormat2(LatLon)
  implicit val mapPostEntityFormat = jsonFormat3(MapPostEntity.apply)
  implicit val listPostEntityFormat = jsonFormat(ListPostEntity.apply(_), "list")
  implicit val datePostEntityFormat = jsonFormat(DatePostEntity.apply(_), "timestamp")

  implicit object PostEntityFormat extends RootJsonWriter[PostEntityTrait] {

    override def write(obj: PostEntityTrait): JsValue = {

      val a = obj match {
        case postBodyEntity: PostBodyEntity   => postBodyEntityFormat.write(postBodyEntity)
        case imagePostEntity: ImagePostEntity => imagePostEntityFormat.write(imagePostEntity)
        case listPostentity: ListPostEntity   => listPostEntityFormat.write(listPostentity)
        case datePostEntity: DatePostEntity   => datePostEntityFormat.write(datePostEntity)
        case mapPostEntity: MapPostEntity   => mapPostEntityFormat.write(mapPostEntity)
        case _                                => ??? //TODO: ADD MORE
      }

      JsObject(Map("cnt" -> a, "type" -> JsString(obj.typeDesc())))
    }
  }

  implicit object VariableMemoryFormat extends RootJsonWriter[VariableMemory] {
    override def write(obj: VariableMemory): JsValue =
      JsObject(obj.map { case (a, b) => a -> PostEntityFormat.write(b) })

  }


  implicit object CompiledPostFormat extends RootJsonWriter[CompiledPost] {

    override def write(obj: CompiledPost): JsValue =
      JsObject(Map("slug" -> JsString(obj.slug), "memory" -> VariableMemoryFormat.write(obj.memory)))
  }

  implicit object PostListFormat extends RootJsonWriter[List[CompiledPost]] {
    override def write(obj: List[CompiledPost]): JsValue =
      JsArray(obj.map(cpost => CompiledPostFormat.write(cpost)).toVector)

  }


}
