package v2.sorters

import v2.model.CompiledPost

trait PostsComparer {

  def comparePostsByDate(p1: CompiledPost, p2: CompiledPost): Boolean = {
    p1.getCreated() > p2.getCreated()
  }
}
