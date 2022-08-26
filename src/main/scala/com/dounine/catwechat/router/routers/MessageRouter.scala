package com.dounine.catwechat.router.routers

import akka.actor.typed.ActorSystem
import akka.cluster.{Cluster, MemberStatus}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, _}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.dounine.catwechat.model.models.{
  CheckModel,
  MessageDing,
  MessageModel,
  RouterModel,
  SpeakModel
}
import com.dounine.catwechat.service.{
  CheckService,
  MessageService,
  SpeakService
}
import com.dounine.catwechat.tools.util.DingDing.MessageData
import com.dounine.catwechat.tools.util.{
  DingDing,
  IpUtils,
  LikeUtil,
  Request,
  ServiceSingleton,
  UUIDUtil
}
import org.slf4j.LoggerFactory

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent
import scala.concurrent.{Await, Future, duration}
import scala.concurrent.duration._

class MessageRouter()(implicit system: ActorSystem[_]) extends SuportRouter {
  val cluster: Cluster = Cluster.get(system)
  private val logger = LoggerFactory.getLogger(classOf[MessageRouter])
  private val messageService = ServiceSingleton.get(classOf[MessageService])
  private val speakService = ServiceSingleton.get(classOf[SpeakService])
  private val checkService = ServiceSingleton.get(classOf[CheckService])
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
  val testGroupName =
    system.settings.config.getString("app.testGroupName") //测试群名
  val displayName = system.settings.config.getString("app.displayName") //测试群名
  val wcId = system.settings.config.getString("app.wcId") //群主微信
  val authorization = system.settings.config.getString("app.authorization")
  var charts = Await.result(
    messageService
      .initAddress()
      .flatMap(_ => {
        messageService.chatrooms()
      })
      .flatMap(rooms => {
        messageService.contacts(rooms)
      }),
    Duration.Inf
  )

  Request
    .post[String](
      s"${messageUrl}/sendText",
      Map(
        "wId" -> wId,
        "wcId" -> wcId,
        "content" -> ("监控群：" + charts
          .map(_.nickName)
          .mkString(","))
      ),
      Map(
        "Authorization" -> authorization
      )
    )
    .foreach(result => {})

  val getAppMsgBody: (String, Boolean) => String =
    (body: String, isCatApp: Boolean) => {
      if (isCatApp) {
        val msgBody = "<appmsg[\\s\\S]*</appmsg>".r.findFirstIn(body).get
        if (msgBody.contains("gh_059d93061ba1@app")) {
          msgBody
            .replace(
              "<sourcedisplayname/>",
              "<sourcedisplayname />"
            )
            .replace(
              "<sourcedisplayname />",
              s"<sourcedisplayname>${displayName}</sourcedisplayname>"
            )
            .replace(
              "<sourcedisplayname>猫车群专用</sourcedisplayname>",
              s"<sourcedisplayname>${displayName}</sourcedisplayname>"
            )
        } else msgBody
      } else body
    }

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
          path("listen" / Segment) {
            listen =>
              listenerSwitch = listen.toBoolean
              if (listenerSwitch) {
                messageService
                  .chatrooms()
                  .flatMap((rooms: Seq[String]) => {
                    messageService.contacts(rooms)
                  })
                  .foreach((_charts: Seq[MessageModel.ContactData]) => {
                    Request
                      .post[String](
                        s"${messageUrl}/sendText",
                        Map(
                          "wId" -> wId,
                          "wcId" -> wcId,
                          "content" -> ("监控群：" + _charts
                            .map(_.nickName)
                            .mkString(","))
                        ),
                        Map(
                          "Authorization" -> authorization
                        )
                      )
                      .foreach(result => {})
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
                      like = data.like.toDouble,
                      useLike = data.useLike,
                      messageType = data.messageType,
                      assistant = data.assistant,
                      sendMessage = getAppMsgBody(
                        data.sendMessage,
                        data.messageType == "sendApp"
                      ),
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
                        like = data.like.toDouble,
                        useLike = data.useLike,
                        messageType = data.messageType,
                        sendMessage = getAppMsgBody(
                          data.sendMessage,
                          data.messageType == "sendApp"
                        ),
                        assistant = data.assistant,
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
                    if (
                      charts
                        .find(item => item.nickName == testGroupName)
                        .map(_.v1)
                        .contains(data.data.fromGroup.getOrElse(""))
                    ) {
                      Request
                        .post[String](
                          s"${messageUrl}/sendText",
                          Map(
                            "wId" -> wId,
                            "wcId" -> data.data.fromGroup,
                            "content" -> data.data.content
                          ),
                          Map(
                            "Authorization" -> authorization
                          )
                        )
                        .foreach(result => {})
                    }
                    charts.find(item =>
                      data.data.fromGroup.contains(
                        item.v1
                      )
                    ) match {
                      case Some(value) =>
                        messageService
                          .roomMembers(value.v1)
                          .map((member: MessageModel.ChatRoomMember) => {
                            member.data
                              .find(
                                _.userName == data.data.fromUser
                              )
                              .map(i => i.displayName.getOrElse(i.nickName))
                          })
                          .flatMap(nickName => {
                            speakService
                              .insertOrUpdate(
                                SpeakModel.SpeakInfo(
                                  time = LocalDate.now(),
                                  group = data.data.fromGroup.get,
                                  wxid = data.data.fromUser,
                                  nickName = nickName.getOrElse(""),
                                  sendMsg = 1,
                                  createTime = LocalDateTime.now()
                                )
                              )
                              .zip(
                                (if (
                                   "签到" == data.data.content && data.messageType.toInt == 80001
                                 ) {
                                   checkService
                                     .check(
                                       CheckModel.CheckInfo(
                                         time = LocalDate.now(),
                                         group =
                                           data.data.fromGroup.getOrElse(""),
                                         wxid = data.data.fromUser,
                                         nickName = nickName.getOrElse(""),
                                         createTime = LocalDateTime.now()
                                       )
                                     )
                                     .map(tp2 => {
                                       Request
                                         .post[String](
                                           s"${messageUrl}/sendText",
                                           Map(
                                             "wId" -> wId,
                                             "wcId" -> data.data.fromGroup,
                                             "content" -> (((if (tp2._1)
                                                               s"${nickName.getOrElse("")} 签到成功、喵币+1💰"
                                                             else
                                                               s"${nickName.getOrElse("")} 今日已签到、喵币+0💰") + "\n" + s"当前可用喵币：${tp2._2}💰") + "\n喵币可用于兑换小程序上的所有产品\n10喵币=1元、每天活跃也能增加喵币噢\n例如：#小程序://养猫专用/巅峰猫罐头/0sflgQSpCHxQxHD")
                                           ),
                                           Map(
                                             "Authorization" -> authorization
                                           )
                                         )
                                         .foreach(result => {})
                                       tp2._2
                                     })
                                 } else Future.successful(-1))
                              )
                          })
                          .foreach(tp2 => {})
                      case None =>
                    }
                    if (data.messageType.toInt == 80001 && listenerSwitch) {
                      charts.find(item =>
                        data.data.fromGroup.contains(
                          item.v1
                        ) || charts
                          .find(item => item.nickName == testGroupName)
                          .map(_.v1)
                          .contains(data.data.fromGroup.getOrElse(""))
                      ) match {
                        case Some(group) =>
                          Array(
                            ("消息排行榜", Some(LocalDate.now())),
                            ("今天消息排行榜", Some(LocalDate.now())),
                            ("昨天消息排行榜", Some(LocalDate.now().minusDays(1))),
                            ("前天消息排行榜", Some(LocalDate.now().minusDays(2))),
                            ("所有消息排行榜", Option.empty)
                          ).filter(_._1 == data.data.content)
                            .foreach(info => {
                              (info._2 match {
                                case Some(value) =>
                                  speakService
                                    .allDate(
                                      data.data.fromGroup.get,
                                      value.toString
                                    )
                                case None =>
                                  speakService.all(data.data.fromGroup.get)
                              }).foreach(msgs => {
                                val nos = Map(
                                  1 -> """🥇""",
                                  2 -> """🥈""",
                                  3 -> """🥉"""
                                )
                                val users = msgs
                                  .sortBy(_.sendMsg)(
                                    Ordering.Int.reverse
                                  )
                                  .zipWithIndex
                                  .map(tp => {
                                    val no = nos.get(tp._2 + 1) match {
                                      case Some(value) => value
                                      case None =>
                                        (if (tp._1.wxid == data.data.fromUser)
                                           """🎖"""
                                         else s"${tp._2 + 1}.")
                                    }
                                    no + s"${tp._1.nickName} - ${tp._1.sendMsg}条"
                                  })

                                Request
                                  .post[String](
                                    s"${messageUrl}/sendText",
                                    Map(
                                      "wId" -> wId,
                                      "wcId" -> data.data.fromGroup,
                                      "content" -> (s"💥 ${info._1}💥 \n" + (if (
                                                                               users.isEmpty
                                                                             ) "很冷静，没人说话，空空如也!!"
                                                                             else
                                                                               users.mkString(
                                                                                 "\n"
                                                                               )))
                                    ),
                                    Map(
                                      "Authorization" -> authorization
                                    )
                                  )
                                  .foreach(result => {
                                    logger.info(
                                      "send message result {}",
                                      result
                                    )
                                  })
                              })
                            })

                          if (
                            "助理，关键字/助理，关键词"
                              .split("/")
                              .exists(
                                _.split("[,，]")
                                  .forall(data.data.content.contains)
                              )
                          ) {
                            Request
                              .post[String](
                                s"${messageUrl}/sendText",
                                Map(
                                  "wId" -> wId,
                                  "wcId" -> data.data.fromGroup,
                                  "content" -> (Seq(
                                    "签到",
                                    "消息排行榜",
                                    "今天消息排行榜",
                                    "昨天消息排行榜",
                                    "前天消息排行榜",
                                    "所有消息排行榜"
                                  ) ++ words
                                    .filter(_.listen)
                                    .filter(_.assistant)
                                    .map(_.text)).zipWithIndex
                                    .map(tp => s"${tp._2 + 1}. ${tp._1}")
                                    .mkString("\n")
                                ),
                                Map(
                                  "Authorization" -> authorization
                                )
                              )
                              .foreach(result => {
                                logger.info(
                                  "send message result {}",
                                  result
                                )
                              })
                          }

                          messageService
                            .all()
                            .map(words => {
                              words
                                .filter(_.listen)
                                .filter(_.assistant)
                                .find(word => {
                                  if (word.useLike) {
                                    LikeUtil.textCosine(
                                      data.data.content,
                                      word.text
                                    ) >= word.like
                                  } else {
                                    if (word.`match` == "EQ") {
                                      word.text == data.data.content
                                    } else if (word.`match` == "IN") {
                                      data.data.content.contains(word.text)
                                    } else if (word.`match` == "ALL") {
                                      word.text
                                        .split("/")
                                        .exists(
                                          _.split("[,，]")
                                            .forall(data.data.content.contains)
                                        )
                                    } else false
                                  }
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
                                        .map(i =>
                                          i.displayName.getOrElse(i.nickName)
                                        )
                                    }
                                  )
                                  .foreach(nickName => {
                                    logger.info("message {}", _data)
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

                                    if (value.send) {
                                      Request
                                        .post[String](
                                          s"${messageUrl}/${value.messageType}",
                                          Map(
                                            "wId" -> wId,
                                            "wcId" -> data.data.fromGroup
                                          ) ++ (value.messageType match {
                                            case "sendEmoji" | "sendNameCard" |
                                                "sendUrl" | "sendVideo" |
                                                "sendVoice" | "sendFile" =>
                                              value.sendMessage
                                                .split(",")
                                                .map(i => {
                                                  i.split(":")
                                                })
                                                .map {
                                                  case Array(f1, f2) => (f1, f2)
                                                }
                                                .toMap[String, String]
                                            case _ =>
                                              Map(
                                                "content" -> value.sendMessage.trim
                                              )
                                          }),
                                          Map(
                                            "Authorization" -> authorization
                                          )
                                        )
                                        .foreach(result => {
                                          logger.info(
                                            "send message result {}",
                                            result
                                          )
                                        })
                                    }
                                  })
                              case None =>
                            }
                        case None =>
                      }

                      if (!data.data.self && data.data.fromUser != wcId) {
                        charts.find(item =>
                          data.data.fromGroup.contains(
                            item.v1
                          ) || charts
                            .find(item => item.nickName == testGroupName)
                            .map(_.v1)
                            .contains(data.data.fromGroup.getOrElse(""))
                        ) match {
                          case Some(group) =>
                            messageService
                              .all()
                              .map(words => {
                                words
                                  .filter(_.listen)
                                  .filter(!_.assistant)
                                  .find(word => {
                                    if (word.useLike) {
                                      LikeUtil.textCosine(
                                        data.data.content,
                                        word.text
                                      ) >= word.like
                                    } else {
                                      if (word.`match` == "EQ") {
                                        word.text == data.data.content
                                      } else if (word.`match` == "IN") {
                                        data.data.content.contains(word.text)
                                      } else if (word.`match` == "ALL") {
                                        word.text
                                          .split("/")
                                          .exists(
                                            _.split("[,，]")
                                              .forall(
                                                data.data.content.contains
                                              )
                                          )
                                      } else false
                                    }
                                  })
                              })
                              .foreach {
                                case Some(value) =>
                                  messageService
                                    .roomMembers(group.v1)
                                    .map(
                                      (member: MessageModel.ChatRoomMember) => {
                                        member.data
                                          .find(
                                            _.userName == data.data.fromUser
                                          )
                                          .map(i =>
                                            i.displayName.getOrElse(i.nickName)
                                          )
                                      }
                                    )
                                    .foreach(nickName => {
                                      logger.info("message {}", _data)
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
                                        .foreach(result => {
                                          logger.info(
                                            "send message result {}",
                                            result
                                          )
                                        })
                                      if (value.send) {
                                        Request
                                          .post[String](
                                            s"${messageUrl}/${value.messageType}",
                                            Map(
                                              "wId" -> wId,
                                              "wcId" -> data.data.fromGroup,
                                              "content" -> value.sendMessage.trim
                                            ),
                                            Map(
                                              "Authorization" -> authorization
                                            )
                                          )
                                          .foreach(result => {
                                            logger.info(
                                              "send message result {}",
                                              result
                                            )
                                          })
                                      }
                                    })

                                case None =>
                              }
                          case None =>
                        }
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
