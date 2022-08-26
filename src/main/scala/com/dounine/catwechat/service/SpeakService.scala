package com.dounine.catwechat.service

import akka.actor.typed.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.dounine.catwechat.model.models.{MessageModel, SpeakModel}
import com.dounine.catwechat.store.{EnumMappers, MessageTable, SpeakTable}
import com.dounine.catwechat.tools.akka.cache.CacheSource
import com.dounine.catwechat.tools.akka.db.DataSource
import com.dounine.catwechat.tools.util.Request
import slick.ast.BaseTypedType
import slick.jdbc.{GetResult, JdbcType}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future
import scala.concurrent.duration._

class SpeakService(implicit system: ActorSystem[_]) extends EnumMappers {
  private val db = DataSource(system).source().db
  private val dict: TableQuery[SpeakTable] = SpeakTable()

  implicit val ec = system.executionContext
  implicit val materializer = SystemMaterializer(system).materializer
  implicit val slickSession =
    SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)

  implicit val localDate2Date2 = MappedColumnType.base[LocalDate, Date](
      { instant: LocalDate =>
        if (instant == null) null else Date.valueOf(instant)
      },
      { date: Date =>
        if (date == null) null else date.toLocalDate
      }
    )


  def insertOrUpdate(info: SpeakModel.SpeakInfo): Future[Int] = {
    db.run(
      sqlu"""INSERT INTO wechat_listener_speak(time,`group`,wxid,nickName,sendMsg) VALUE(${info.time
        .toString()},${info.group},${info.wxid},${info.nickName},${info.sendMsg}) ON DUPLICATE KEY UPDATE sendMsg=sendMsg+${info.sendMsg},nickName=${info.nickName}"""
    )
  }

  implicit val getSpeakInfoResult = GetResult(r => SpeakModel.SpeakInfo(r.nextDate().toLocalDate, r.<<, r.<<, r.<<, r.<<,r.nextTimestamp().toLocalDateTime))
  def allDate(group: String, time: String): Future[Seq[SpeakModel.SpeakInfo]] = {
    db.run(
      sql"""select * from wechat_listener_speak where `group` = ${group} and time = ${time}""".as[SpeakModel.SpeakInfo]
    )
  }

  def all(group: String): Future[Seq[SpeakModel.SpeakInfo]] = {
    db.run(
      sql"""select min(time) as time,`group`,wxid,min(nickName) as nickName,sum(sendMsg) as sendMsg,min(createTime) as createTime from wechat_listener_speak where `group` = ${group} group by wxid""".as[SpeakModel.SpeakInfo]
    )
  }

}
