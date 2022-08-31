package com.dounine.catwechat.service

import akka.actor.typed.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.dounine.catwechat.model.models.{ConsumModel, MsgLevelModel}
import com.dounine.catwechat.store.{ConsumTable, EnumMappers, MsgLevelTable}
import com.dounine.catwechat.tools.akka.db.DataSource
import com.dounine.catwechat.tools.util.ServiceSingleton
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

class ConsumService(implicit system: ActorSystem[_]) extends EnumMappers {
  private val db = DataSource(system).source().db
  private val dict: TableQuery[ConsumTable] = ConsumTable()

  implicit val ec = system.executionContext
  implicit val materializer = SystemMaterializer(system).materializer
  implicit val slickSession =
    SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)

  private val checkService = ServiceSingleton.get(classOf[CheckService])
  private val msgLevelService = ServiceSingleton.get(classOf[MsgLevelService])

  def insert(info: ConsumModel.ConsumInfo): Future[Int] =
    db.run(
      dict += info
    )

  def all(
      group: String,
      wxid: String
  ): Future[Seq[ConsumModel.ConsumInfo]] = {
    db.run(
      dict.filter(i => i.group === group && i.wxid === wxid).result
    )
  }

  def accountCoin(group: String, wxid: String): Future[(Int, Int, Int)] = {
    checkService
      .all(group, wxid)
      .zip(msgLevelService.all(group, wxid))
      .flatMap(tp2 => {
        all(group, wxid)
          .map(i => {
            //签到 + 聊天奖励 - 消费 = 余额
            (tp2._1.length * 2, tp2._2.map(_.coin).sum, i.map(_.coin).sum)
          })
      })
  }

}
