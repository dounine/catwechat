package com.dounine.catwechat.service

import akka.actor.typed.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.dounine.catwechat.model.models.{CheckModel, MsgLevelModel}
import com.dounine.catwechat.model.models.MsgLevelModel.MsgLevelInfo
import com.dounine.catwechat.store.{CheckTable, EnumMappers, MsgLevelTable}
import com.dounine.catwechat.tools.akka.db.DataSource
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import java.time.LocalDate
import scala.concurrent.Future

class MsgLevelService(implicit system: ActorSystem[_]) extends EnumMappers {
  private val db = DataSource(system).source().db
  private val dict: TableQuery[MsgLevelTable] = MsgLevelTable()

  implicit val ec = system.executionContext
  implicit val materializer = SystemMaterializer(system).materializer
  implicit val slickSession =
    SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)

  def insertOrUpdate(info: MsgLevelModel.MsgLevelInfo): Future[Int] =
    db.run(
      sqlu"""INSERT INTO wechat_listener_msg_level(time,`group`,wxid,level,nickName,coin) VALUE(${
        info.time
          .toString()
      },${info.group},${info.wxid},${info.level},${info.nickName},${info.coin}) ON DUPLICATE KEY UPDATE coin=${info.coin},nickName=${info.nickName}"""
    )

  def all(group: String, wxid: String): Future[Seq[MsgLevelModel.MsgLevelInfo]] = {
    db.run(
      dict.filter(i => i.group === group && i.wxid === wxid).result
    )
  }

}
