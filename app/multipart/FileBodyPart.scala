package multipart

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

case class FileBodyPart(
                     override val name: String,
                     fileName: Option[String],
                     content: Source[ByteString, NotUsed]
                   ) extends BodyPart

