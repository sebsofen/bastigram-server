package post.postentities

trait PostEntityTrait {
  /**
    * merge two post entity traits
    * @param pet
    * @return
    */
  def +(pet: PostEntityTrait) : PostEntityTrait
}

class InvalidOperandExeption extends Exception