package com.dounine.catwechat.service

import akka.actor.typed.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.dounine.catwechat.model.models.{
  CheckModel,
  DictionaryModel,
  SpeakModel
}
import com.dounine.catwechat.store.{CheckTable, EnumMappers, SpeakTable}
import com.dounine.catwechat.tools.akka.db.DataSource
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class CheckService(implicit system: ActorSystem[_]) extends EnumMappers {
  private val db = DataSource(system).source().db
  private val dict: TableQuery[CheckTable] = CheckTable()

  implicit val ec = system.executionContext
  implicit val materializer = SystemMaterializer(system).materializer
  implicit val slickSession =
    SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)

  def insertOrUpdate(info: CheckModel.CheckInfo): Future[Int] =
    db.run(
      dict.insertOrUpdate(
        info
      )
    )

  def check(info: CheckModel.CheckInfo): Future[(Boolean, Int)] = {
    db.run(
      dict.filter(i => i.group === info.group && i.wxid === info.wxid).result
    )
      .flatMap((result: Seq[CheckModel.CheckInfo]) => {
        if (result.exists(_.time == LocalDate.now())) {
          Future.successful((false, result.length))
        } else {
          insertOrUpdate(info)
            .map(_ => (true, result.length + 1))
        }
      })
  }

  def all(group: String, wxid: String): Future[Seq[CheckModel.CheckInfo]] = {
    db.run(
      dict.filter(i => i.group === group && i.wxid === wxid).result
    )
  }

}
