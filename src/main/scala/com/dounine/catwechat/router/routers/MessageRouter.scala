package com.dounine.catwechat.router.routers

import akka.actor.typed.ActorSystem
import akka.cluster.{Cluster, MemberStatus}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, _}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.dounine.catwechat.model.models.{
  MessageDing,
  MessageModel,
  RouterModel
}
import com.dounine.catwechat.service.MessageService
import com.dounine.catwechat.tools.util.DingDing.MessageData
import com.dounine.catwechat.tools.util.{
  DingDing,
  IpUtils,
  Request,
  ServiceSingleton,
  UUIDUtil
}
import org.slf4j.LoggerFactory

import java.time.LocalDateTime
import scala.concurrent
import scala.concurrent.{Await, duration}
import scala.concurrent.duration._

class MessageRouter()(implicit system: ActorSystem[_]) extends SuportRouter {
  val cluster: Cluster = Cluster.get(system)
  private val logger = LoggerFactory.getLogger(classOf[MessageRouter])
  private val messageService = ServiceSingleton.get(classOf[MessageService])
  var listenerSwitch = true

  implicit val ec = system.executionContext
  val words = Await.result(
    ServiceSingleton
      .get(classOf[MessageService])
      .all(),
    Duration.Inf
  )
  val messageUrl = system.settings.config.getString("app.messageUrl")
  val wId = system.settings.config.getString("app.wId") //实例id
  val wcId = system.settings.config.getString("app.wcId") //群主微信
  val authorization = system.settings.config.getString("app.authorization")
  var charts = Await.result(
    messageService
      .chatrooms()
      .flatMap(rooms => {
        messageService.contacts(rooms)
      }),
    Duration.Inf
  )

  val route =
    cors() {
      concat(
        get {
          path("info" / Segment) { id =>
            val result = messageService
              .queryById(id)
              .map(result => RouterModel.Data(Option(result)))
            complete(result)
          } ~ path("infos") {
            val result = messageService
              .all()
              .map(result =>
                RouterModel.Data(
                  Option(
                    Map(
                      "listen" -> listenerSwitch,
                      "list" -> result
                    )
                  )
                )
              )
            complete(result)
          }
        },
        post {
          path("listen" / Segment) { listen =>
            listenerSwitch = listen.toBoolean
            if (listenerSwitch) {
              messageService
                .chatrooms()
                .flatMap((rooms: Seq[String]) => {
                  messageService.contacts(rooms)
                })
                .foreach((_charts: Seq[MessageModel.ContactData]) => {
                  charts = _charts
                })
            }
            ok
          } ~ path("info" / "delete" / Segment) { id =>
            val result = messageService
              .deleteById(id)
              .map(result => RouterModel.Data(Option(result)))
            complete(result)
          } ~ path("info" / "add") {
            entity(as[MessageModel.MessageBody]) {
              data =>
                val result = messageService
                  .insertOrUpdate(
                    MessageModel.MessageDbInfo(
                      id = UUIDUtil.uuid(),
                      text = data.text,
                      `match` = data.`match`,
                      listen = data.listen,
                      send = data.send,
                      sendMessage = data.sendMessage,
                      createTime = LocalDateTime.now()
                    )
                  )
                  .map(result => RouterModel.Data(Option(result)))
                complete(result)
            }
          } ~ path("info" / Segment) {
            id =>
              entity(as[MessageModel.MessageBody]) {
                data =>
                  val result = messageService
                    .insertOrUpdate(
                      MessageModel.MessageDbInfo(
                        id = id,
                        text = data.text,
                        `match` = data.`match`,
                        listen = data.listen,
                        send = data.send,
                        sendMessage = data.sendMessage,
                        createTime = LocalDateTime.now()
                      )
                    )
                    .map(result => RouterModel.Data(Option(result)))
                  complete(result)
              }
          } ~
            path("message" / "yun") {
              entity(as[Map[String, Any]]) {
                _data =>
                  {
                    val data = _data.toJson.jsonTo[MessageModel.Message]
                    if (data.messageType.toInt == 80001 && listenerSwitch) {
                      charts.find(item =>
                        data.data.fromGroup.contains(
                          item.v1
                        ) && !data.data.self && data.data.fromUser != wcId
                      ) match {
                        case Some(group) =>
                          messageService
                            .all()
                            .map(words => {
                              words
                                .filter(_.listen)
                                .find(word => {
                                  if (word.`match` == "EQ") {
                                    word.text == data.data.content
                                  } else if (word.`match` == "IN") {
                                    data.data.content.contains(word.text)
                                  } else false
                                })
                            })
                            .foreach {
                              case Some(value) =>
                                messageService
                                  .roomMembers(group.v1)
                                  .map(
                                    (member: MessageModel.ChatRoomMember) => {
                                      member.data
                                        .find(_.userName == data.data.fromUser)
                                        .map(_.nickName)
                                    }
                                  )
                                  .foreach(nickName => {
                                    logger.info(
                                      "匹配到关键字 {} -> {}:{} : {} from {}",
                                      group.nickName,
                                      value.`match`,
                                      value.text,
                                      data.data.content,
                                      nickName
                                        .getOrElse(
                                          ""
                                        )
                                    )
                                    Request
                                      .post[String](
                                        s"${messageUrl}/sendText",
                                        Map(
                                          "wId" -> wId,
                                          "wcId" -> wcId,
                                          "content" -> (group.nickName + "：" + nickName
                                            .getOrElse(
                                              ""
                                            ) + " : " + data.data.content)
                                        ),
                                        Map(
                                          "Authorization" -> authorization
                                        )
                                      )
                                      .foreach(result => {})

                                    if (value.send) {
                                      Request
                                        .post[String](
                                          s"${messageUrl}/sendText",
                                          Map(
                                            "wId" -> wId,
                                            "wcId" -> data.data.fromGroup,
                                            "content" -> value.sendMessage
                                          ),
                                          Map(
                                            "Authorization" -> authorization
                                          )
                                        )
                                        .foreach(result => {})
                                    }
                                  })

                              case None =>
                            }
                        case None =>
                      }
                    }
                    ok(
                      Map(
                        "message" -> "成功",
                        "code" -> "1000"
                      )
                    )
                  }
              }
            }
        }
      )
    }
}
