package post.postentities
import com.typesafe.scalalogging.Logger
import post.PostCompiler
import post.PostCompiler.VariableDeclaration
import v2.model.CompiledPost

import scala.concurrent.Future
import scala.util.Try

case object DatePostEntity extends PostEntityTraitMatcher {
  val logger = Logger(classOf[DatePostEntity])
  override def matchPost(matchInstruction: PostCompiler.Instruction): Boolean = matchInstruction match {
    case VariableDeclaration(variable, statement) => statement.startsWith("[date")
    case _                                        => false
  }

  override def postEntityFromInstruction(
      matchInstruction: PostCompiler.Instruction,
      postCache: (String) => Option[CompiledPost],postSlug: String): Future[(String, PostEntityTrait)] = matchInstruction match {
    case VariableDeclaration(variable, statement) =>
      val datestring = statement.stripPrefix("[date").stripSuffix("]")
      parseDate(datestring) match {
        case Some(date) => Future.successful(variable, DatePostEntity(date))
        case None =>
          logger.debug("could not parse date in string |" + datestring + "|")
          Future.failed(new StatementNotSupportedException)
      }

    case _ =>
      Future.failed(new StatementNotSupportedException)

  }

  def parseDate(dateString: String): Option[Long] = {
    import java.text.SimpleDateFormat
    Try {
      val pattern = "dd.MM.yyyy"
      val simpleDateFormat = new SimpleDateFormat(pattern)

      val date = simpleDateFormat.parse(dateString)
      date.getTime
    }.toOption

  }
}

case class DatePostEntity(timestamp: Long) extends PostEntityTrait {
  override def typeDesc(): String = "DATE"

  /**
    * merge two post entity traits
    *
    * @param pet
    * @return
    */
  override def +(pet: PostEntityTrait): PostEntityTrait = pet match {
    case DatePostEntity(oldTs) => DatePostEntity(timestamp + oldTs)
  }
}
