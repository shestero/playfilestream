package multipart

import helpers.streamsugar.dropLast
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Framing, Sink, Source}
import org.apache.pekko.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc.{Action, BodyParser, MessagesRequest}
import play.api.mvc.MultipartFormData.{FilePart, Part}

import cats.syntax.traverse.given
import scala.concurrent.{ExecutionContext, Future}

trait MultipartBodyParser[Result] {
  val maxStringLen: Int = 4096
  def process(file: FileBodyPart, fields: Map[String, String]): Future[Result]

  private def parseContentDisposition(implicit ec: ExecutionContext, mat: Materializer):
    PartialFunction[(String, Source[ByteString, NotUsed]), BodyPart] =
      // Note: the order does matter:
      case (s"""Content-Disposition: form-data; name="$name"; filename="$fileName"${"\r"}""", content)  =>
        FileBodyPart(name, Some(fileName), content.dropLast.map(_ ++ ByteString("\n")))
      case (s"""Content-Disposition: form-data; name="$name"${"\r"}""", content) =>
        FieldBodyPart(name, content)

  protected def verbatimBodyParser(implicit ec: ExecutionContext): BodyParser[Source[ByteString, _]] = BodyParser { _ =>
    // Return the source directly. We need to return
    // an Accumulator[Either[Result, T]], so if we were
    // handling any errors we could map to something like
    // a Left(BadRequest("error")). Since we're not
    // we just wrap the source in a Right(...)
    Accumulator.source[ByteString].map(Right.apply)
  }

  /*
  def multipartAction = Action(verbatimBodyParser).async {
    parseMultipartRequest(_).map(_.fold(NotFound)(Ok.apply))
  }
  */
  def parseMultipartRequest(r: MessagesRequest[Source[ByteString, _]])
                           (implicit ec: ExecutionContext, mat: Materializer): Future[Option[Result]] =
    parseMultipartRequest(
      r.headers
        .get("Content-Type")
        .collect { case s"$contentType; boundary=$boundary" =>
          assert(contentType == "multipart/form-data")
          boundary
        },
      r.body
    )

  def parseMultipartRequest(boundary: Option[String], body: Source[ByteString, _])
                           (implicit ec: ExecutionContext, mat: Materializer): Future[Option[Result]] = {
    println(s"boundary=$boundary")
    val partStream: Source[BodyPart, _] =
      body
        .via(Framing.delimiter(
          ByteString("\n"), maximumFrameLength = maxStringLen, allowTruncation = true)) // ByteString("\r\n")
        .takeWhile(line => !boundary.map("--" ++ _ ++ "--").contains(line.utf8String.trim))
        .splitWhen(line => boundary.map("--" ++ _).contains(line.utf8String.trim))
        .drop(1)
        .prefixAndTail(1)
        .collect {
          case (Seq(firstLine), others) =>
            println(s"first (Content-Disposition?)=${firstLine.utf8String}")
            firstLine.utf8String -> others.dropWhile(_  != ByteString("\r")).drop(1)
        }
        .collect(parseContentDisposition)
        .mergeSubstreams
        .takeWhile(_.isInstanceOf[FieldBodyPart], inclusive = true) // util file field

    for {
      parts <- partStream.runWith(Sink.seq)
      fields <- Future.sequence(
        parts.collect { case field: FieldBodyPart => field.content.map(field.name -> _) }
      ).map(_.toMap)
      file = parts.collectFirst { case file: FileBodyPart => file }
      result <- file.traverse(process(_, fields))
    } yield result
  }
}
