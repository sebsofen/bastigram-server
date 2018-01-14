package v2.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.bastigram.model.CompiledPost
import de.bastigram.post.PostCompiler.VariableMemory
import de.bastigram.post.postentities._
import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsString, JsValue, RootJsonFormat, RootJsonWriter}
import v2.actors.{HashTagAndStats, HashTagStats}


trait NewJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val hashTagStatsFormat: RootJsonFormat[HashTagStats] = jsonFormat1(HashTagStats)
  implicit val hashTagAndStatsFormat: RootJsonFormat[HashTagAndStats] = jsonFormat2(HashTagAndStats)

  implicit val postBodyEntityFormat: RootJsonFormat[PostBodyEntity] = jsonFormat(PostBodyEntity.apply(_), "body")
  implicit val imagePostEntityFormat: RootJsonFormat[ImagePostEntity] = jsonFormat(ImagePostEntity.apply(_), "imgs")
  implicit val labelPostEntityFormat: RootJsonFormat[LabelPostEntity] = jsonFormat(LabelPostEntity.apply(_), "label")
  implicit val latLonFormat: RootJsonFormat[LatLon] = jsonFormat2(LatLon)
  implicit val mapPostEntityFormat: RootJsonFormat[MapPostEntity] = jsonFormat6(MapPostEntity.apply)
  implicit val youtubePostEntityFormat: RootJsonFormat[YoutubePostEntity] = jsonFormat1(YoutubePostEntity.apply)
  implicit val listPostEntityFormat: RootJsonFormat[ListPostEntity] = jsonFormat(ListPostEntity.apply(_), "list")
  implicit val datePostEntityFormat: RootJsonFormat[DatePostEntity] = jsonFormat(DatePostEntity.apply(_), "timestamp")

  implicit object PostEntityFormat extends RootJsonWriter[PostEntityTrait] {

    override def write(obj: PostEntityTrait): JsValue = {

      val a = obj match {
        case et: PostBodyEntity         => postBodyEntityFormat.write(et)
        case et: ImagePostEntity       => imagePostEntityFormat.write(et)
        case et: ListPostEntity         => listPostEntityFormat.write(et)
        case et: DatePostEntity         => datePostEntityFormat.write(et)
        case et: MapPostEntity           => mapPostEntityFormat.write(et)
        case et: YoutubePostEntity   => youtubePostEntityFormat.write(et)
        case et: LabelPostEntity       => labelPostEntityFormat.write(et)
        case _                                      => ??? //TODO: ADD MORE
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
