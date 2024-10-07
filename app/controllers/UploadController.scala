package controllers

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.ByteString

import javax.inject.*
import play.api.*
import play.api.i18n.I18nSupport
import play.api.mvc.*
import helpers.pipeIf
import multipart.{FileBodyPart, MultipartBodyParser}

import java.math.BigInteger
import java.security.MessageDigest
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.*

@Singleton
class UploadController @Inject()(
                                  cc: MessagesControllerComponents /* val controllerComponents: ControllerComponents */
                                )
                                (implicit ec: ExecutionContext, mat: Materializer)
  extends MessagesAbstractController(cc)
    with I18nSupport
    with MultipartBodyParser[String] {

  def form: Action[_] = Action { implicit request =>
    Ok(views.html.form.apply)
  }

  extension (source: Source[ByteString, _])
    def md5(prefix: String = ""): Future[String] =
      val digest: MessageDigest = MessageDigest.getInstance("MD5")
      source
        .runWith(Sink.fold(0L) { (size, data) =>
          digest.update(data.asByteBuffer)
          size + data.size
        })
        .map { size =>
          val hash = String.format("%032X", new BigInteger(1, digest.digest())).toLowerCase
          s"${prefix.pipeIf(_.nonEmpty)(_ + "\t: ")}size=$size\tMD5=$hash"
        }

  override def process(file: FileBodyPart, fields: Map[String, String]): Future[String] = {
    file.content.md5(s"fields=$fields")
  }

  def upload: Action[_] = Action(verbatimBodyParser) { request =>
    parseMultipartRequest(request)
      .map(_.getOrElse("ERROR: No file!"))
      .pipe(Source.future)
      .keepAlive(10.seconds, () => "... [keep alive message each 10 seconds!] ...")
      .map(_ + "\n")
      .map(ByteString.apply)
      .pipe(Ok.chunked(_, Some("text/plain")))
  }

}
