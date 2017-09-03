package post.postentities

 case class GpsPostEntity(pois: List[GpsPOI] = List(), tracks: List[GpsTrack] = List() )  extends PostEntityTrait {

   /**
     *
     * @param pet
     * @return
     */
   override def +(pet: PostEntityTrait): PostEntityTrait = pet match {
     case p : GpsPostEntity =>
       GpsPostEntity(this.pois ++ p.pois, this.tracks ++ p.tracks)
     case _  =>
       throw new InvalidOperandExeption
   }

 }

case class LatLon(lat: Double, lon: Double)
case class GpsPOI(latLon: LatLon, title: String, icon: String)
case class GpsTrack(latLons: List[LatLon], title: String)