package com.dounine.catwechat.router.routers

import akka.actor.typed.ActorSystem
import akka.cluster.{Cluster, MemberStatus}
import akka.http.scaladsl.model.{
  ContentType,
  HttpEntity,
  HttpResponse,
  MediaTypes,
  StatusCodes
}
import akka.http.scaladsl.server.Directives._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.dounine.catwechat.model.models.MessageDing
import com.dounine.catwechat.tools.util.DingDing.{MessageData, MessageType}
import com.dounine.catwechat.tools.util.DingDing.MessageType.MessageType
import com.dounine.catwechat.tools.util.{DingDing, IpUtils, Request}

import scala.concurrent.duration._

class HealthRouter()(implicit system: ActorSystem[_]) extends SuportRouter {
  val cluster: Cluster = Cluster.get(system)

  val route =
    cors() {
      concat(
        get {
          path("ip") {
            parameter("type".optional) {
              `type` =>
                extractClientIP {
                  ip =>
                    {
                      `type`.getOrElse("") match {
                        case "ip" =>
                          complete(
                            HttpResponse(entity =
                              HttpEntity(
                                ip.getIp()
                              )
                            )
                          )
                        case "ipJson" => ok(ip.getIp())
                        case _ =>
                          val oip = ip.getIp()
                          val (province, city) =
                            IpUtils.convertIpToProvinceCity(oip)
                          ok(
                            Map(
                              "ip" -> oip,
                              "city" -> city,
                              "province" -> province
                            )
                          )
                      }
                    }
                }
            }
          } ~ path("health") {
            ok
          } ~ path("ready") {
            withRequestTimeout(1.seconds, request => timeoutResponse) {
              ok
            }
          } ~ path("alive") {
            if (
              cluster.selfMember.status == MemberStatus.Up || cluster.selfMember.status == MemberStatus.WeaklyUp
            ) {
              ok
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        },
        post {
          path("msg") {
            entity(as[MessageDing.Data]) {
              data =>
                val url = data.code match {
                  case Some(value) =>
                    s"https://oapi.dingtalk.com/robot/send?access_token=${value}"
                  case None =>
                    "https://oapi.dingtalk.com/robot/send?access_token=29fe753d3106786b4a8171f32d4fc228af709a3b54be1fd2dfa2e7962b56192b"
                }
                val result = Request
                  .post[String](
                    url,
                    MessageData(
                      markdown = DingDing.Markdown(
                        title = data.title,
                        text = data.text
                      )
                    )
                  )
                  .map(r => Map("result" -> r))(system.executionContext)
                ok(result)
            }
          } ~
            path("msg2") {
              entity(as[MessageDing.Data]) {
                data =>
                  val result = Request
                    .post[String](
                      "https://oapi.dingtalk.com/robot/send?access_token=c5ccf5a653fae07ebe6a148e9cf973026d8a1d45b3d0cceb9f2556ce5842743d",
                      MessageData(
                        markdown = DingDing.Markdown(
                          title = data.title,
                          text = data.text
                        )
                      )
                    )
                    .map(r => Map("result" -> r))(system.executionContext)
                  ok(result)
              }
            }
        }
      )
    }
}
