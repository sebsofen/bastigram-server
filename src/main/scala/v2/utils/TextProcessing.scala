package v2.utils

trait TextProcessing {
  def levensthein(a: String, b: String): Double = {
    import scala.math.min
    val c = ((0 to b.size).toList /: a)((prev, x) =>
      (prev zip prev.tail zip b).scanLeft(prev.head + 1) {
        case (h, ((d, v), y)) => min(min(h + 1, v + 1), d + (if (x == y) 0 else 1))
    }).last

    c.toDouble / (b.length + a.length)

  }
}
