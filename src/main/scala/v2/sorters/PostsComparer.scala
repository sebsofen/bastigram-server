package v2.sorters

import de.bastigram.model.CompiledPost


trait PostsComparer {

  def comparePostsByDate(p1: CompiledPost, p2: CompiledPost): Boolean = {
    p1.getCreated() > p2.getCreated()
  }
}
