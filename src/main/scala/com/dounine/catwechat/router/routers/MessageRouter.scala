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
  val charts =
    """
      |[
      |        {
      |            "userName": "17643049836@chatroom",
      |            "nickName": "喵喵交流群(G)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/ZqDaDiccbgkiax7wtKAb2SUsicqzEIcFNVpialW26pHVRoAZF4CbmEXvicP7T00XdTS8KSeCTEp6ZVRwJ0JjYyQFldgKNzZKOSJBx/0",
      |            "labelList": null,
      |            "v1": "17643049836@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "18439649554@chatroom",
      |            "nickName": "喵喵交流群(i)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/PiajxSqBRaEIPHfQkgxM6ibCSqn90Rb51YTDD7uCF4jyktEtFqNLonsChnxsIdswYz0KZe2ercngjKoyoA9LB6bEtoLyU8cakd/0",
      |            "labelList": null,
      |            "v1": "18439649554@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "21759330332@chatroom",
      |            "nickName": "喵喵交流群(A)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/hFbbFt44dnmDqTzIBby047NrGiaibKuic5RTvslRUcznn68OAjA35y4P0DFrTAosCWWX6dcqYU3fKDawSiblq4ib5Siaj8vIWwzzAz/0",
      |            "labelList": null,
      |            "v1": "21759330332@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "17798251148@chatroom",
      |            "nickName": "人机测试",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/1CHHx9Yq4nFfpVtfFlnkUXwRXpicq0UKK8hfrj5EEr07X7rBofTr5ZGgNnqWibFRn21ZaJ9bV2nrDVC5ibmeNjRiauDcFFzm9xx9/0",
      |            "labelList": null,
      |            "v1": "17798251148@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "17465762197@chatroom",
      |            "nickName": "喵喵交流群(J)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/UOlZKghBxaagVicPWhfRN0Mjk87T2k8j45xicNgxo4kba0VkN6ibmoAyem4ZbdCpJ7eyF8oK7o1xQSF6LlIwiccDeZQ1qWazZE8m/0",
      |            "labelList": null,
      |            "v1": "17465762197@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "20338939758@chatroom",
      |            "nickName": "喵喵交流群(H)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/LIND77SSex9DjsuvkY9ictycmdU9JLlTPjy9ShViaIayVHT1sYib762XicEgLrbVph2vm5qqbEV8RdC8a7mOGYIRibHk0ve7eUHcJ/0",
      |            "labelList": null,
      |            "v1": "20338939758@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "21279401739@chatroom",
      |            "nickName": "喵喵交流群(D)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/PiajxSqBRaEKIQKUNEvdC0U4gQWCLSoXZkV0a8DdGHn6sHd4XDFwtqic02wOnkTHfM8HSzCy4C97kXP51kQd6cQAQ8C3TkFLgg/0",
      |            "labelList": null,
      |            "v1": "21279401739@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "17855762020@chatroom",
      |            "nickName": "喵喵交流群(K)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/qE9MKluetOkmtXBbukPP184x6x8d8aOCO9thzrPja45icDrMswXYXHlianicicJqtbAtQsNJZiaCGCoZIwaJoSho7Qn0wOj9iaC8iaq/0",
      |            "labelList": null,
      |            "v1": "17855762020@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "18765558183@chatroom",
      |            "nickName": "猫车姐妹群",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/98Nz5LFElxyasGAY6Udarnoj2ictg9fyUFw7vd8DgKIxQZoQDclOosmIRom2eeVehDxET5s2TnIHyZaB9ZCRKG2tdHGabSlL5/0",
      |            "labelList": null,
      |            "v1": "18765558183@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "21364907764@chatroom",
      |            "nickName": "喵喵交流群(C)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/9M0PhLTmTIfHNK6L4HibmbH71toZ5WoMbkef2dZTPeQRXGykD23p4t6BLibaMOy2LCN1TrINTxxULWbp8xnrcr9aXaJoxByWqG/0",
      |            "labelList": null,
      |            "v1": "21364907764@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "18112847508@chatroom",
      |            "nickName": "喵喵交流群(E)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/ps68icnpRvDU58UTLy9z9xAycViaQHQQDNxHU0TVTkCqQic5ibuP5ta5H6CS0Encc5kWfkRRsJ9CUe9hIfL2XniaLVfJv2oeXNibYs/0",
      |            "labelList": null,
      |            "v1": "18112847508@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "18193554468@chatroom",
      |            "nickName": "喵喵交流群(B)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/XFJ8HdGGwGC8ysj9WOicCFe3kN5Kx0Do74O5GkGh8zCt4C0TibvTwzBYib7728b8zehKueOibib0jl5GKD0ibJYajWrTW33z6wsCQL/0",
      |            "labelList": null,
      |            "v1": "18193554468@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "20383434573@chatroom",
      |            "nickName": "喵喵交流群(M)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/bNqVZcia7iaBw30RbJoGSFLufyBxkibqZibdKpuBwAdaTjkGDhadiaEibVh7apMaxia4bloibIwBL5OhrtOCibbl26br8tIibTXFNqnkh4/0",
      |            "labelList": null,
      |            "v1": "20383434573@chatroom",
      |            "province": null,
      |            "city": null
      |        },
      |        {
      |            "userName": "18637450794@chatroom",
      |            "nickName": "喵喵交流群(L)",
      |            "remark": null,
      |            "signature": null,
      |            "sex": 0,
      |            "aliasName": null,
      |            "country": null,
      |            "bigHead": null,
      |            "smallHead": "http://wx.qlogo.cn/mmcrhead/6XFhg7ldObz82WcWO4VjjJpeiaWW3QwgwZjIMzQygNHth9UvEb9rNMONia27x2GIDibvkhliaKWnebyWddPrvqjkXD6fA011bbXO/0",
      |            "labelList": null,
      |            "v1": "18637450794@chatroom",
      |            "province": null,
      |            "city": null
      |        }
      |    ]
      |""".stripMargin
      .replaceAll("\n", "")
      .replaceAll(" ", "")
      .jsonTo[Array[MessageModel.MessageInfo]]

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

  val route =
    cors() {
      concat(
        get {
          path("info" / Segment) { id =>
            val result = messageService
              .queryById(id)
              .map(result => RouterModel.Data(Option(result)))(
                system.executionContext
              )
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
              )(
                system.executionContext
              )
            complete(result)
          }
        },
        post {
          path("listen" / Segment) { listen =>
            listenerSwitch = listen.toBoolean
            ok
          } ~ path("info" / "delete" / Segment) { id =>
            val result = messageService
              .deleteById(id)
              .map(result => RouterModel.Data(Option(result)))(
                system.executionContext
              )
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
                  .map(result => RouterModel.Data(Option(result)))(
                    system.executionContext
                  )
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
                    .map(result => RouterModel.Data(Option(result)))(
                      system.executionContext
                    )
                  complete(result)
              }
          } ~
            path("message" / "yun") {
              entity(as[Map[String, Any]]) {
                _data =>
                  {
                    logger.info("info {}", _data)
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
                            })(system.executionContext)
                            .foreach {
                              case Some(value) =>
                                logger.info(
                                  "匹配到关键字 {} ->{}:{} : {}",
                                  group.nickName,
                                  value.`match`,
                                  value.text,
                                  data.data.content
                                )

                                Request
                                  .post[String](
                                    s"${messageUrl}/sendText",
                                    Map(
                                      "wId" -> wId,
                                      "wcId" -> wcId,
                                      "content" -> (group.nickName + " -> " + data.data.content)
                                    ),
                                    Map(
                                      "Authorization" -> authorization
                                    )
                                  )
                                  .foreach(result => {})(
                                    system.executionContext
                                  )
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
                                    .foreach(result => {})(
                                      system.executionContext
                                    )
                                }
                              case None =>
                            }(system.executionContext)
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
