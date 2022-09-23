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

  def insertOrUpdate(info: MsgLevelModel.MsgLevelInfo): Future[Int] = {
    if (info.coin != 0) {
      db.run(dict += info)
    } else Future.successful(1)
  }

  def all(wxid: String): Future[Seq[MsgLevelModel.MsgLevelInfo]] = {
    db.run(
      dict.filter(i => i.wxid === wxid).result
    )
  }

}
