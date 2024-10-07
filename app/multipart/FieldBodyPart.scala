package multipart

import helpers.pipeIf
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.*

case class FieldBodyPart(
                      override val name: String,
                      content: Future[String]
                    ) extends BodyPart 
                    
object FieldBodyPart:
  def apply(name: String, content: Source[ByteString, NotUsed])
           (implicit ec: ExecutionContext, mat: Materializer): FieldBodyPart =
    content
      .map(_.utf8String)
      .map(_.pipeIf(_.endsWith("\r"))(_.dropRight(1)))
      .runWith(Sink.reduce(_ ++ "\n" ++ _))
      .recover { case _: NoSuchElementException => "" }
      .pipe(FieldBodyPart(name, _))
