package com.dounine.catwechat.router.routers

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{
  ContentType,
  HttpEntity,
  HttpHeader,
  HttpResponse,
  MediaTypes
}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{concat, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{FileIO, Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.{CompletionStrategy, _}
import akka.{NotUsed, actor}
import com.dounine.catwechat.model.types.service.LogEventKey
import com.dounine.catwechat.tools.util.IpUtils
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.nio.file.{Files, Paths}
import java.time.LocalDate
import javax.imageio.ImageIO
import scala.concurrent.{ExecutionContextExecutor, Future}

object FileRouter
class FileRouter()(implicit system: ActorSystem[_]) extends SuportRouter {

  private final val logger: Logger =
    LoggerFactory.getLogger(classOf[FileRouter])
  implicit val materializer: Materializer = SystemMaterializer(
    system
  ).materializer
  implicit val actorSystem: actor.ActorSystem = materializer.system
  implicit val executionContext: ExecutionContextExecutor =
    materializer.executionContext

  val config = system.settings.config.getConfig("app")
  val fileSaveDirectory = config.getString("file.directory")
  val domain = config.getString("file.domain")
  val routerPrefix = config.getString("routerPrefix")

  def tempDestination(fileInfo: FileInfo): File = {
    val directory = new File(fileSaveDirectory + "/" + LocalDate.now())
    directory.mkdirs()
    File.createTempFile(
      "screen_",
      "_" + fileInfo.getFileName,
      directory
    )
  }

  val route: Route = {
    pathPrefix("file") {
      concat(
        post {
          path("image") {
            extractRequest {
              request =>
                storeUploadedFile("file", tempDestination) {
                  case (metadata, file) => {
                    val scheme: String = request.headers
                      .map(i => i.name() -> i.value())
                      .toMap
                      .getOrElse("X-Scheme", request.uri.scheme)
                    ok(
                      Map(
                        "domain" -> ((scheme + "://" + domain) + s"/${routerPrefix}/file/image?path="),
                        "url" -> file.getAbsolutePath
                      )
                    )
                  }
                }
            }
          } ~ path("watermark") {
            extractRequest {
              request =>
                storeUploadedFile("file", tempDestination) {
                  case (metadata, file) => {
                    val sourceImg = ImageIO.read(
                      file
                    )

                    val waterImg = ImageIO.read(
                      FileRouter.getClass.getResourceAsStream(
                        "/watermark.png"
                      )
                    )

                    val scale = request
                      .getHeader("scale")
                      .orElse(RawHeader("scale", "0.18"))
                      .value()
                      .toFloat

                    val opacity = request
                      .getHeader("opacity")
                      .orElse(RawHeader("opacity", "0.8"))
                      .value()
                      .toFloat

                    val insets = request
                      .getHeader("insets")
                      .orElse(RawHeader("insets", "10"))
                      .value()
                      .toInt

                    val waterWidth = (sourceImg.getWidth * scale).toInt

                    val watermark = Thumbnails
                      .of(waterImg)
                      .size(
                        waterWidth,
                        waterWidth * waterImg.getHeight / waterImg.getWidth
                      )
                      .keepAspectRatio(false)
                      .asBufferedImage()

                    val tmpFile =
                      File.createTempFile("watermark", metadata.fileName)

                    Thumbnails
                      .of(
                        sourceImg
                      )
                      .size(sourceImg.getWidth, sourceImg.getHeight)
                      .watermark(
                        Positions.BOTTOM_RIGHT,
                        watermark,
                        opacity,
                        insets
                      )
                      .outputQuality(1f)
                      .toFile(
                        tmpFile
                      )

                    complete(
                      HttpResponse(entity =
                        HttpEntity(
                          ContentType(MediaTypes.`image/png`),
                          Files.readAllBytes(Paths.get(tmpFile.getAbsolutePath))
                        )
                      )
                    )
                  }
                }
            }
          }
        },
        get {
          path("image") {
            parameter("path") {
              path =>
                extractClientIP {
                  ip =>
                    val byteArray: Array[Byte] =
                      Files.readAllBytes(Paths.get(path))
                    val (province, city) =
                      IpUtils.convertIpToProvinceCity(ip.getIp())
                    logger.info(
                      Map(
                        "time" -> System.currentTimeMillis(),
                        "data" -> Map(
                          "event" -> LogEventKey.payQrcodeAccess,
                          "payQrcodeUrl" -> path,
                          "ip" -> ip.getIp(),
                          "province" -> province,
                          "city" -> city
                        )
                      ).toJson
                    )
                    complete(
                      HttpResponse(entity =
                        HttpEntity(
                          ContentType(MediaTypes.`image/png`),
                          byteArray
                        )
                      )
                    )
                }
            }
          }
        }
      )
    }
  }

}
