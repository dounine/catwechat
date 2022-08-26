package com.dounine.catwechat.service

import akka.actor.typed.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.dounine.catwechat.model.models.{MessageModel, SpeakModel}
import com.dounine.catwechat.store.{EnumMappers, MessageTable, SpeakTable}
import com.dounine.catwechat.tools.akka.cache.CacheSource
import com.dounine.catwechat.tools.akka.db.DataSource
import com.dounine.catwechat.tools.util.Request
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.duration._

class SpeakService(implicit system: ActorSystem[_]) extends EnumMappers {
  private val db = DataSource(system).source().db
  private val dict: TableQuery[SpeakTable] = SpeakTable()

  implicit val ec = system.executionContext
  implicit val materializer = SystemMaterializer(system).materializer
  implicit val slickSession =
    SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)

  def insertOrUpdate(info: SpeakModel.SpeakInfo): Future[Int] = {
    db.run(
      sqlu"""INSERT INTO wechat_listener_speak(date,`group`,wxid,nickName,sendMsg) VALUE(${info.date.toString()},${info.group},${info.wxid},${info.nickName},${info.sendMsg}) ON DUPLICATE KEY UPDATE sendMsg=sendMsg+${info.sendMsg},nickName=${info.nickName}"""
    )
  }



}
