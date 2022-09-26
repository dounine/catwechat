package com.dounine.catwechat.router.routers

import akka.actor.Cancellable
import akka.actor.typed.ActorSystem
import akka.cluster.{Cluster, MemberStatus}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.dounine.catwechat.model.models.MsgLevelModel.{CoinCyUserInfo, CoinUserInfo}
import com.dounine.catwechat.model.models.{CheckModel, ConsumModel, MessageDing, MessageModel, MsgLevelModel, RouterModel, SpeakModel}
import com.dounine.catwechat.service.{CheckService, ConsumService, MessageService, MsgLevelService, SpeakService}
import com.dounine.catwechat.tools.util.DingDing.MessageData
import com.dounine.catwechat.tools.util.{DingDing, IpUtils, LikeUtil, Request, ServiceSingleton, UUIDUtil}
import org.slf4j.LoggerFactory

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime}
import scala.concurrent
import scala.concurrent.{Await, Future, duration}
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Random

class MessageRouter()(implicit system: ActorSystem[_]) extends SuportRouter {
  val cluster: Cluster = Cluster.get(system)
  private val logger = LoggerFactory.getLogger(classOf[MessageRouter])
  private val messageService = ServiceSingleton.get(classOf[MessageService])
  private val speakService = ServiceSingleton.get(classOf[SpeakService])
  private val checkService = ServiceSingleton.get(classOf[CheckService])
  private val msgLevelService = ServiceSingleton.get(classOf[MsgLevelService])
  private val consumService = ServiceSingleton.get(classOf[ConsumService])
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
  var charts: Seq[MessageModel.ContactData] = Await.result(
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

  type GroupId = String
  var coinMaps: Map[GroupId, MsgLevelModel.CoinInfo] = Map[GroupId, MsgLevelModel.CoinInfo]()
  var cyMaps: Map[GroupId, MsgLevelModel.CYInfo] = Map[GroupId, MsgLevelModel.CYInfo]()
  val cyWrods = Source
    .fromInputStream(
      IpUtils.getClass.getResourceAsStream("/成语词典数据库.sql")
    )
    .getLines()
    .filter(_.startsWith("INSERT INTO"))
    .map(line => {
      line
        .split("""VALUES \(""")
        .last
        .split(",")(1)
        .split("'")(1)
    })
    .filter(_.length==4)
    .toList

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
              "<sourcedisplayname>养猫专用</sourcedisplayname>",
              s"<sourcedisplayname>${displayName}</sourcedisplayname>"
            )
        } else msgBody
      } else body
    }

  val msgLevelRequires = Seq(
    MsgLevelModel.LevelRequire(
      level = 1,
      name = "话唠",
      des = "喵群消息999数你有一半功劳",
      msg = 500,
      coin = 1
    ),
    MsgLevelModel.LevelRequire(
      level = 2,
      name = "话仙",
      des = "恭喜获得今天社交达人称号",
      msg = 1000,
      coin = 1
    ),
    MsgLevelModel.LevelRequire(
      level = 3,
      name = "吧唧嘴",
      des = "今日喵群最佳主持人、无人可挡",
      msg = 1500,
      coin = 1
    ),
    MsgLevelModel.LevelRequire(
      level = 4,
      name = "666",
      des = "不知道怎么夸你了、多奖励你",
      msg = 2000,
      coin = 1
    )
  )

  def sendText(wcId:String,content:String): Unit ={
    Request
      .post[String](
        s"${messageUrl}/sendText",
        Map(
          "wId" -> wId,
          "wcId" -> wcId,
          "content" ->content
        ),
        Map(
          "Authorization" -> authorization
        )
      )
      .foreach(result => {
      })
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
          } ~ path("charts"){
            ok(charts)
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
          path("coin" / "down"){
            entity(as[MsgLevelModel.DownInfo]) {
              data => {
                val des = data.des match {
                  case Some(value) => value
                  case None => "掉落"
                }
                sendText(
                  data.groupId,
                  s"""
                     |北京时间${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}、${des}${data.coin/10D} 💰喵币
                     |获取方法：发送【 捡 】、或者【 抢 】关键字
                     |- - - - - - - - - - -- - - - - - - - - - -- - - - - - - - - - -
                     |规则一：10秒内捡到的人没人抢可归第一个捡到的人所有
                     |规则二：如果发生争抢、归20秒内最后抢到的一个人所有
                     |规则三：这是博弈游戏、每人单次仅可以发送一次抢或捡
                     |""".stripMargin
                )
                coinMaps += data.groupId -> MsgLevelModel.CoinInfo(
                  coin = data.coin,
                  createTime = LocalDateTime.now(),
                  settle = false,
                  result = None,
                  isPick = true,
                  pick = None,
                  pickSchedule = None,
                  robs = Array.empty,
                  robSchedule = None
                )
                ok
              }
            }
          } ~ path("coin" / "cy"){
            entity(as[MsgLevelModel.DownInfo]) {
              data => {
                val des = data.des match {
                  case Some(value) => value
                  case None => "成语游戏胜利者可得"
                }
                if(cyMaps.get(data.groupId).isDefined && !cyMaps(data.groupId).settle){
                 fail("目前有成语接龙游戏正在进行当中")
                }else{
                  var word = cyWrods(Random.nextInt(cyWrods.length-1))
                  while(!cyWrods.exists(_.startsWith(word.takeRight(1)))){
                    word = cyWrods(Random.nextInt(cyWrods.length-1))
                  }
                  sendText(
                    data.groupId,
                    s"""
                       |北京时间${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}、${des}${data.coin/10D} 💰喵币
                       |本次游戏成语：${word}
                       |- - - - - - - - - - -- - - - - - - - - - -- - - - - - - - - - -
                       |规则一：发送4个字的成语即可参与、正确会有判定、错误没有
                       |规则二：15秒内如果没有人接得上下一个成语、赢家为上一人
                       |""".stripMargin
                  )
                  cyMaps += data.groupId -> MsgLevelModel.CYInfo(
                    coin = data.coin,
                    world = word,
                    createTime = LocalDateTime.now(),
                    settle = false,
                    result = None,
                    cyList = Array.empty,
                    finishSchedule = None
                  )
                  system.scheduler.scheduleOnce(1.minutes,()=>{
                    if(cyMaps(data.groupId).cyList.isEmpty){
                      sendText(
                        data.groupId,
                        s"""
                           |本次成语接龙游戏无人参与、已结束。
                           |""".stripMargin
                      )
                      cyMaps = cyMaps.filterNot(_._1==data.groupId)
                    }
                  })
                  ok
                }
              }
            }
          } ~path("listen" / Segment) {
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
                          .flatMap((nickName: Option[String]) => {
                            speakService
                              .insertOrUpdate(
                                SpeakModel.SpeakInfo(
                                  time = LocalDate.now(),
                                  group = data.data.fromGroup.get,
                                  wxid = data.data.fromUser,
                                  nickName = nickName.getOrElse(""),
                                  sendMsg =
                                    (if (
                                       Array("签到", "摸鱼").contains(
                                         data.data.content
                                       ) && data.messageType.toInt == 80001
                                     ) 0
                                     else 1),
                                  createTime = LocalDateTime.now()
                                )
                              )
                              .zip(
                                (if (
                                   Array("签到", "摸鱼").contains(
                                     data.data.content
                                   ) && data.messageType.toInt == 80001
                                 ) {
                                   consumService
                                     .accountCoin(
                                       data.data.fromUser
                                     )
                                     .flatMap(coin => {
                                       if (
                                         (coin._1 + coin._2 - coin._3) >= 50
                                       ) {
                                         Request
                                           .post[String](
                                             s"${messageUrl}/sendText",
                                             Map(
                                               "wId" -> wId,
                                               "wcId" -> data.data.fromGroup,
                                               "content" ->
                                                s"""
                                                   |「${nickName.getOrElse("")} 喵币已经达上限、请兑换后再重新${data.data.content}积累」
                                                   |- - - - - - - - - - -
                                                   |当前可用喵币 ${(coin._1 + coin._2 - coin._3) / 10d}💰
                                                   |使用方法：
                                                   |1、发送"喵币"两个关键字、由骚骚的群主扣掉后
                                                   |2、在养猫专用小程序上下单、然后由小程序客服改价即可
                                                   |""".stripMargin
                                             ),
                                             Map(
                                               "Authorization" -> authorization
                                             )
                                           )
                                           .map(i => {
                                             messageService
                                               .all()
                                               .map(ii => {
                                                 ii.filter(_.listen)
                                                   .filter(_.assistant)
                                                   .find(
                                                     _.text == "助理，小程序"
                                                   )
                                               })
                                               .foreach(opt => {
                                                 if (opt.isDefined) {
                                                   val value = opt.get
                                                   Request
                                                     .post[String](
                                                       s"${messageUrl}/${value.messageType}",
                                                       Map(
                                                         "wId" -> wId,
                                                         "wcId" -> data.data.fromGroup
                                                       ) ++ (value.messageType match {
                                                         case "sendEmoji" |
                                                              "sendNameCard" |
                                                              "sendUrl" |
                                                              "sendVideo" |
                                                              "sendVoice" |
                                                              "sendFile" =>
                                                           value.sendMessage
                                                             .split(",")
                                                             .map(i => {
                                                               i.split(
                                                                 ":"
                                                               )
                                                             })
                                                             .map {
                                                               case Array(
                                                               f1,
                                                               f2
                                                               ) =>
                                                                 (f1, f2)
                                                             }
                                                             .toMap[
                                                             String,
                                                             String
                                                           ]
                                                         case _ =>
                                                           Map(
                                                             "content" -> value.sendMessage.trim
                                                           )
                                                       }),
                                                       Map(
                                                         "Authorization" -> authorization
                                                       )
                                                     )
                                                     .foreach(
                                                       result => {}
                                                     )
                                                 }
                                               })
                                             (
                                               nickName,
                                               coin._1 + coin._2 - coin._3
                                             )
                                           })
                                       } else {
                                         checkService
                                           .check(
                                             CheckModel.CheckInfo(
                                               time = LocalDate.now(),
                                               wxid = data.data.fromUser,
                                               nickName =
                                                 nickName.getOrElse(""),
                                               createTime = LocalDateTime.now()
                                             )
                                           )
                                           .map(_ -> coin)
                                           .flatMap(
                                             (tp2: (
                                                 (Boolean, Int),
                                                 (Int, Int, Int)
                                             )) => {
                                               Request
                                                 .post[String](
                                                   s"${messageUrl}/sendText",
                                                   Map(
                                                     "wId" -> wId,
                                                     "wcId" -> data.data.fromGroup,
                                                     "content" -> (((if (
                                                                       tp2._1._1
                                                                     )
                                                                       s"「${nickName.getOrElse("")} ${data.data.content}成功、喵币奖励 +0.2💰」"
                                                                     else
                                                                       s"「${nickName.getOrElse("")} 重复${data.data.content}、喵币无奖励」") + "\n" + s"当前可用喵币 ${(tp2._1._2 + tp2._2._2 - tp2._2._3) / 10d}💰") + "\n- - - - - - - - - - -\n喵币可兑换下面小程序中的所有产品")
                                                   ),
                                                   Map(
                                                     "Authorization" -> authorization
                                                   )
                                                 )
                                                 .map(j => {
                                                   messageService
                                                     .all()
                                                     .map(ii => {
                                                       ii.filter(_.listen)
                                                         .filter(_.assistant)
                                                         .find(
                                                           _.text == "助理，小程序"
                                                         )
                                                     })
                                                     .foreach(opt => {
                                                       if (opt.isDefined) {
                                                         val value = opt.get
                                                         Request
                                                           .post[String](
                                                             s"${messageUrl}/${value.messageType}",
                                                             Map(
                                                               "wId" -> wId,
                                                               "wcId" -> data.data.fromGroup
                                                             ) ++ (value.messageType match {
                                                               case "sendEmoji" |
                                                                   "sendNameCard" |
                                                                   "sendUrl" |
                                                                   "sendVideo" |
                                                                   "sendVoice" |
                                                                   "sendFile" =>
                                                                 value.sendMessage
                                                                   .split(",")
                                                                   .map(i => {
                                                                     i.split(
                                                                       ":"
                                                                     )
                                                                   })
                                                                   .map {
                                                                     case Array(
                                                                           f1,
                                                                           f2
                                                                         ) =>
                                                                       (f1, f2)
                                                                   }
                                                                   .toMap[
                                                                     String,
                                                                     String
                                                                   ]
                                                               case _ =>
                                                                 Map(
                                                                   "content" -> value.sendMessage.trim
                                                                 )
                                                             }),
                                                             Map(
                                                               "Authorization" -> authorization
                                                             )
                                                           )
                                                           .foreach(
                                                             result => {}
                                                           )
                                                       }
                                                     })
                                                   j
                                                 })
                                                 .map(_ =>
                                                   (
                                                     nickName,
                                                     tp2._1._2 + tp2._2._2 - tp2._2._3
                                                   )
                                                 )
                                             }
                                           )
                                       }
                                     })
                                 } else if (
                                   Array("喵币查询", "查询喵币", "喵币", "喵币余额").contains(
                                     data.data.content
                                   ) && data.messageType.toInt == 80001
                                 ) {
                                   consumService
                                     .accountCoin(
                                       data.data.fromUser
                                     )
                                     .map(tp3 => {
                                       Request
                                         .post[String](
                                           s"${messageUrl}/sendText",
                                           Map(
                                             "wId" -> wId,
                                             "wcId" -> data.data.fromGroup,
                                             "content" -> s"@${nickName
                                               .getOrElse("")} 喵币余额：${(tp3._1 + tp3._2 - tp3._3) / 10d}💰\n喵币帐号：${data.data.fromUser}".stripMargin
                                           ),
                                           Map(
                                             "Authorization" -> authorization
                                           )
                                         )
                                       (
                                         nickName,
                                         tp3._1 + tp3._2 - tp3._3
                                       )
                                     })
                                 } else {
                                   consumService
                                     .accountCoin(
                                       data.data.fromUser
                                     )
                                     .map(tp3 =>
                                       (
                                         nickName,
                                         tp3._1 + tp3._2 - tp3._3
                                       )
                                     )
                                 })
                              )
                          })
                          .foreach(tp2 => {
                            val nickNameAndCoin = tp2._2
                            tp2._1.find(
                              _.time == LocalDate
                                .now() && data.data.fromUser != "wxid_lvwrpaxcrm5a22"
                            ) match {
                              case Some(value) => {
//                                msgLevelRequires.find(p => {
//                                  value.sendMsg == p.msg
//                                }) match {
//                                  case Some(level) => {
//                                    msgLevelService
//                                      .insertOrUpdate(
//                                        MsgLevelModel.MsgLevelInfo(
//                                          time = LocalDate.now(),
//                                          group = value.group,
//                                          wxid = value.wxid,
//                                          nickName = value.nickName,
//                                          coin = level.coin,
//                                          level = level.coin,
//                                          createTime = LocalDateTime.now()
//                                        )
//                                      )
//                                      .flatMap(_ => {
//                                        Request
//                                          .post[String](
//                                            s"${messageUrl}/sendText",
//                                            Map(
//                                              "wId" -> wId,
//                                              "wcId" -> data.data.fromGroup,
//                                              "content" -> (s"""💥 恭喜@${nickNameAndCoin._1
//                                                .getOrElse(
//                                                  ""
//                                                )} 成为${level.name} 💥\n${level.des}\n喵币额外奖励 +${level.coin / 10d}💰""" + "\n" + s"当前可用喵币 ${(nickNameAndCoin._2 + level.coin) / 10d}💰")
//                                            ),
//                                            Map(
//                                              "Authorization" -> authorization
//                                            )
//                                          )
//                                      })
//                                      .foreach(result => {})
//                                  }
//                                  case None =>
//                                }
                              }
                              case None =>
                            }
                          })
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
                            ("所有消息排行榜", Option.empty),
                            ("聊天排行榜", Some(LocalDate.now())),
                            ("今天聊天排行榜", Some(LocalDate.now())),
                            ("昨天聊天排行榜", Some(LocalDate.now().minusDays(1))),
                            ("前天聊天排行榜", Some(LocalDate.now().minusDays(2))),
                            ("所有聊天排行榜", Option.empty),
                            ("活跃排行榜", Some(LocalDate.now())),
                            ("今天活跃排行榜", Some(LocalDate.now())),
                            ("昨天活跃排行榜", Some(LocalDate.now().minusDays(1))),
                            ("前天活跃排行榜", Some(LocalDate.now().minusDays(2))),
                            ("所有活跃排行榜", Option.empty)
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
                                         else s" ${tp._2 + 1}. ")
                                    }
                                    no + s"${tp._1.nickName}"
                                  })
                                  .take(3)

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

                          val userId = data.data.fromUser
                          val groupId = data.data.fromGroup.get

                          if(data.data.content=="发起成语接龙游戏" && cyMaps.get(groupId).isDefined){
                            sendText(
                              groupId,
                              s"""
                                 |成语接龙游戏正在进行中、无法重复发起。
                                 |""".stripMargin
                            )
                          }else if(data.data.content=="发起成语接龙游戏" && (cyMaps.get(groupId).isEmpty || !cyMaps(groupId).settle)){
                            system.scheduler.scheduleOnce(1.minutes,()=>{
                              if(cyMaps(groupId).cyList.isEmpty){
                                sendText(
                                  groupId,
                                  s"""
                                     |本次成语接龙游戏无人参与、已结束。
                                     |""".stripMargin
                                )
                                cyMaps = cyMaps.filterNot(_._1==groupId)
                              }
                            })
                            messageService
                              .roomMembers(groupId)
                              .map((member: MessageModel.ChatRoomMember) => {
                                member.data
                                  .find(
                                    _.userName == data.data.fromUser
                                  )
                                  .map(i => i.displayName.getOrElse(i.nickName))
                              })
                              .map(_.getOrElse(""))
                              .foreach(nickName=>{
                                var word = cyWrods(Random.nextInt(cyWrods.length-1))
                                while(!cyWrods.exists(_.startsWith(word.takeRight(1)))){
                                  word = cyWrods(Random.nextInt(cyWrods.length-1))
                                }
                                sendText(
                                  groupId,
                                  s"""
                                     |${nickName} 发起成语接龙游戏、奖励空气喵币
                                     |本次游戏成语：${word}
                                     |- - - - - - - - - - -- - - - - - - - - - -- - - - - - - - - - -
                                     |规则一：发送4个字的成语即可参与、正确会有判定、错误没有
                                     |规则二：15秒内如果没有人接得上下一个成语、赢家为上一人
                                     |""".stripMargin
                                )
                                cyMaps += groupId -> MsgLevelModel.CYInfo(
                                  coin = 0,
                                  world = word,
                                  createTime = LocalDateTime.now(),
                                  settle = false,
                                  result = None,
                                  cyList = Array.empty,
                                  finishSchedule = None
                                )
                              })
                          }


                          if(cyMaps.contains(groupId) && !cyMaps(groupId).settle && data.data.content.length==4){
                            val cyInfo = cyMaps(groupId)
                            val cyWord = data.data.content.trim
                            val issCy: String => Boolean = (cy:String) => {
                              cyWrods.contains(cy)
                            }
                            val scheduleCreate: String => Option[Cancellable] = (userId:String) => {
                              Some(system.scheduler.scheduleOnce(15*1000.milliseconds, () => {
                                var latestInfo = cyMaps(groupId)
                                latestInfo = latestInfo.copy(
                                  settle = true,
                                  result = Some(latestInfo.cyList.last)
                                )
                                cyMaps = cyMaps.filterNot(_._1==groupId)

                                msgLevelService.insertOrUpdate(MsgLevelModel.MsgLevelInfo(
                                  time = LocalDate.now(),
                                  wxid = userId,
                                  nickName = latestInfo.result.get.nickName,
                                  level = 0,
                                  coin = latestInfo.coin,
                                  createTime = LocalDateTime.now()
                                ))
                                  .foreach(_=>{
                                    consumService
                                      .accountCoin(
                                        data.data.fromUser
                                      )
                                      .foreach(tp3=>{
                                        if(latestInfo.coin==0){
                                          sendText(
                                            groupId,
                                            s"""
                                               |💥 恭喜${latestInfo.result.get.nickName} 💥
                                               |你是本次成语接龙获胜者、空气喵币0💰是你的了
                                               |""".stripMargin
                                          )
                                        }else{
                                          sendText(
                                            groupId,
                                            s"""
                                               |💥 恭喜${latestInfo.result.get.nickName} 💥
                                               |你是本次成语接龙获胜者、喵币${latestInfo.coin/10D}💰是你的了
                                               |- - - - - - - - - - -
                                               |喵币余额：${(tp3._1 + tp3._2 - tp3._3) / 10d}💰
                                               |""".stripMargin
                                          )
                                        }
                                      })
                                  })
                              }))
                            }
                            messageService
                              .roomMembers(groupId)
                              .map((member: MessageModel.ChatRoomMember) => {
                                member.data
                                  .find(
                                    _.userName == data.data.fromUser
                                  )
                                  .map(i => i.displayName.getOrElse(i.nickName))
                              })
                              .map(_.getOrElse(""))
                              .foreach(nickName=>{
                                if(cyInfo.cyList.isEmpty && cyWord.take(1) == cyInfo.world.takeRight(1)){//第一位成语接龙成员
                                  if(issCy(cyWord)){
                                    if(cyInfo.coin==0){
                                      sendText(
                                        groupId,
                                        s"""
                                           |${nickName} 接的「${cyInfo.world}」下一个成语「${cyWord}」判定有效
                                           |你是第1位接得上成语的铲屎官
                                           |- - - - - - - - - - -
                                           |15秒内无人接得上、空气喵币0💰即可归你
                                           |""".stripMargin
                                      )
                                    }else{
                                      sendText(
                                        groupId,
                                        s"""
                                           |${nickName} 接的「${cyInfo.world}」下一个成语「${cyWord}」判定有效
                                           |你是第1位接得上成语的铲屎官
                                           |- - - - - - - - - - -
                                           |15秒内无人接得上、喵币${cyInfo.coin/10D}💰即可归你
                                           |""".stripMargin
                                      )
                                    }
                                    cyMaps += groupId -> cyInfo.copy(
                                      cyList = cyInfo.cyList ++ Array(MsgLevelModel.CoinCyUserInfo(
                                        word = cyWord,
                                        wxid = userId,
                                        nickName = nickName
                                      )),
                                      finishSchedule = scheduleCreate(userId)
                                    )
                                  }
                                }else if(cyInfo.cyList.nonEmpty && cyWord.take(1) == cyInfo.cyList.last.word.takeRight(1)){//第N位成语接龙人员
                                  if(issCy(data.data.content)){
                                    if(cyInfo.coin==0){
                                      sendText(
                                        groupId,
                                        s"""
                                           |${nickName} 接的「${cyInfo.cyList.last.word}」下一个成语「${cyWord}」判定有效
                                           |你是第${cyInfo.cyList.length+1}位接得上成语的铲屎官
                                           |- - - - - - - - - - -
                                           |15秒内无人接得上、空气喵币0💰可归你
                                           |""".stripMargin
                                      )
                                    }else{
                                      sendText(
                                        groupId,
                                        s"""
                                           |${nickName} 接的「${cyInfo.cyList.last.word}」下一个成语「${cyWord}」判定有效
                                           |你是第${cyInfo.cyList.length+1}位接得上成语的铲屎官
                                           |- - - - - - - - - - -
                                           |15秒内无人接得上、喵币${cyInfo.coin/10D}💰可归你
                                           |""".stripMargin
                                      )
                                    }
                                    cyInfo.finishSchedule.foreach(_.cancel())
                                    cyMaps += groupId -> cyInfo.copy(
                                      cyList = cyInfo.cyList ++ Array(MsgLevelModel.CoinCyUserInfo(
                                        word = cyWord,
                                        wxid = userId,
                                        nickName = nickName
                                      )),
                                      finishSchedule = scheduleCreate(userId)
                                    )
                                  }
                                }
                              })
                          }

                          if(Array("捡","抢").contains(data.data.content)){
                            messageService
                              .roomMembers(groupId)
                              .map((member: MessageModel.ChatRoomMember) => {
                                member.data
                                  .find(
                                    _.userName == data.data.fromUser
                                  )
                                  .map(i => i.displayName.getOrElse(i.nickName))
                              }).foreach(nickName=>{
                                coinMaps.get(groupId) match {
                                  case Some(info: MsgLevelModel.CoinInfo) =>
                                  {
                                    if(info.settle){
                                      sendText(
                                        data.data.fromGroup.get,
                                        s"""
                                           |喵币${info.coin/10D} 💰、已经被${info.result.get.nickName}放到小金库了
                                           |- - - - - - - - - - -
                                           |下次手速要快一点噢~~
                                           |""".stripMargin
                                      )
                                    }else if(data.data.content=="捡"){
                                      info.pick match {
                                        case Some(pickInfo) => {
                                          sendText(
                                            groupId,
                                            s"""
                                               |喵币${info.coin/10D} 💰、被${pickInfo.nickName}捡到了、你还有时间抢过来
                                               |""".stripMargin
                                          )
                                        }
                                        case None => {
                                          sendText(
                                            groupId,
                                            s"""
                                               |喵币${info.coin/10D} 💰、被${nickName.get}捡了、10秒没人抢可就归我了
                                               |""".stripMargin
                                          )
                                          coinMaps += data.data.fromGroup.get -> info.copy(
                                            pick = Some(MsgLevelModel.CoinUserInfo(
                                                nickName = nickName.get,
                                                wxid = data.data.fromUser
                                            )),
                                            pickSchedule = if(info.pickSchedule.isDefined) info.pickSchedule else {
                                              Some(system.scheduler.scheduleOnce(10*1000.milliseconds, () => {
                                                var latestInfo = coinMaps(groupId)
                                                coinMaps +=  groupId -> latestInfo.copy(
                                                  result = latestInfo.pick,
                                                  settle = true,
                                                  isPick = true
                                                )
                                                latestInfo = coinMaps(groupId)

                                                msgLevelService.insertOrUpdate(MsgLevelModel.MsgLevelInfo(
                                                  time = LocalDate.now(),
                                                  wxid = userId,
                                                  nickName = latestInfo.result.get.nickName,
                                                  level = 0,
                                                  coin = latestInfo.coin,
                                                  createTime = LocalDateTime.now()
                                                ))
                                                  .foreach(_=>{
                                                    consumService
                                                      .accountCoin(
                                                        data.data.fromUser
                                                      )
                                                      .foreach(tp3=>{
                                                        sendText(
                                                          groupId,
                                                          s"""
                                                             |💥 恭喜${latestInfo.result.get.nickName} 💥
                                                             |掉落的喵币${latestInfo.coin/10D}💰是你的了
                                                             |- - - - - - - - - - -
                                                             |喵币余额：${(tp3._1 + tp3._2 - tp3._3) / 10d}💰
                                                             |""".stripMargin
                                                        )
                                                      })
                                                  })

                                              }))
                                            }
                                          )
                                        }
                                      }
                                    } else if(data.data.content=="抢"){
                                      info.pick match {
                                        case Some(value) => {
                                          info.pickSchedule.foreach(_.cancel())
                                          coinMaps += groupId -> info.copy(
                                            robs = if(info.robs.exists(_.wxid==userId)) info.robs else info.robs ++ Array(MsgLevelModel.CoinUserInfo(
                                              nickName = nickName.get,
                                              wxid = data.data.fromUser
                                            )),
                                            robSchedule = if(info.robSchedule.isDefined) info.robSchedule else {
                                              Some(system.scheduler.scheduleOnce(20*1000.milliseconds, () => {
                                                var latestInfo = coinMaps(groupId)
                                                coinMaps +=  groupId -> latestInfo.copy(
                                                  result = Some(
                                                    if(latestInfo.robs.isEmpty) {
                                                      latestInfo.pick.get
                                                    }else{
                                                      latestInfo.robs.last
                                                    }
                                                  ),
                                                  settle = true,
                                                  isPick = latestInfo.robs.isEmpty
                                                )
                                                latestInfo = coinMaps(groupId)

                                                msgLevelService.insertOrUpdate(MsgLevelModel.MsgLevelInfo(
                                                  time = LocalDate.now(),
                                                  wxid = userId,
                                                  nickName = latestInfo.result.get.nickName,
                                                  level = 0,
                                                  coin = latestInfo.coin,
                                                  createTime = LocalDateTime.now()
                                                ))
                                                  .foreach(_=>{
                                                    consumService
                                                      .accountCoin(
                                                        data.data.fromUser
                                                      )
                                                      .foreach(tp3=>{
                                                        sendText(
                                                          groupId,
                                                          s"""
                                                             |💥 恭喜${latestInfo.result.get.nickName} 💥
                                                             |掉落的喵币${latestInfo.coin/10D}💰是你的了
                                                             |- - - - - - - - - - -
                                                             |喵币余额：${(tp3._1 + tp3._2 - tp3._3) / 10d}💰
                                                             |""".stripMargin
                                                        )
                                                      })
                                                  })
                                              }))
                                            }
                                          )
                                        }
                                        case None => sendText(
                                          groupId,
                                          s"""
                                             |喵币${info.coin/10D} 💰、还没人捡呢、不能抢过来
                                             |""".stripMargin
                                        )
                                      }
                                    }
                                  }
                                  case None =>
                                    sendText(
                                      groupId,
                                      s"""
                                         |目前没有喵币💰可${data.data.content}
                                         |""".stripMargin
                                    )
                                }
                            })
                          }

                          if (
                            data.data.fromUser == wcId && data.data.content
                              .contains("喵币帐号")
                          ) {
                            val consumCoin = (data.data.content
                              .split("\n")
                              .head
                              .split("-")
                              .last
                              .trim
                              .toFloat * 10).toInt
                            val consumWxid =
                              data.data.content
                                .split("\n")
                                .last
                                .split("：")
                                .last
                                .trim

                            messageService
                              .roomMembers(data.data.fromGroup.get)
                              .map((member: MessageModel.ChatRoomMember) => {
                                member.data
                                  .find(
                                    _.userName == consumWxid
                                  )
                                  .map(i => i.displayName.getOrElse(i.nickName))
                              })
                              .zip(
                                consumService.accountCoin(
                                  consumWxid
                                )
                              )
                              .foreach(tp2 => {
                                val (checkCoin, msgCoin, dbConsumCoin) = tp2._2
                                val nickName = tp2._1
                                if (
                                  consumCoin > (checkCoin + msgCoin - dbConsumCoin)
                                ) {
                                  Request
                                    .post[String](
                                      s"${messageUrl}/sendText",
                                      Map(
                                        "wId" -> wId,
                                        "wcId" -> data.data.fromGroup,
                                        "content" -> s"${nickName.getOrElse("")} 喵币余额不足、无法扣除\n喵币余额：${(checkCoin + msgCoin - dbConsumCoin) / 10d}💰"
                                      ),
                                      Map(
                                        "Authorization" -> authorization
                                      )
                                    )
                                    .foreach(result => {})

                                } else {
                                  consumService
                                    .insert(
                                      ConsumModel.ConsumInfo(
                                        wxid = consumWxid,
                                        nickName = nickName.getOrElse(""),
                                        coin = consumCoin,
                                        createTime = LocalDateTime.now()
                                      )
                                    )
                                    .foreach(_ => {
                                      Request
                                        .post[String](
                                          s"${messageUrl}/sendText",
                                          Map(
                                            "wId" -> wId,
                                            "wcId" -> data.data.fromGroup,
                                            "content" -> s"「${nickName
                                              .getOrElse("")} 喵币-${consumCoin / 10d}扣除成功」\n喵币余额：${(checkCoin + msgCoin - dbConsumCoin - consumCoin) / 10d}💰\n- - - - - - - - - - -\n小程序产品提交不要付款、等待客服改价即可"
                                          ),
                                          Map(
                                            "Authorization" -> authorization
                                          )
                                        )
                                        .foreach(result => {})
                                    })
                                }
                              })

                          }

                          if (
                            "助理，关键字/助理，关键词/功能菜单/菜单功能/功能列表"
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
                                    "喵币查询",
                                    "消息排行榜",
                                    "发起成语接龙游戏",
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
