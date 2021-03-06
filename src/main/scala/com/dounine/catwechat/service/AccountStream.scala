package com.dounine.catwechat.service

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.scaladsl.Flow
import com.dounine.catwechat.model.models.AccountModel
import com.dounine.catwechat.store.AccountTable
import com.dounine.catwechat.tools.akka.db.DataSource
import slick.jdbc.JdbcBackend

object AccountStream {

  def query()(implicit
      system: ActorSystem[_]
  ): Flow[String, Option[AccountModel.AccountInfo], NotUsed] = {
    val db: JdbcBackend.DatabaseDef = DataSource(system).source().db
    implicit val ec = system.executionContext
    implicit val slickSession: SlickSession =
      SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)
    import slickSession.profile.api._
    val accountTable = AccountTable()
    Flow[String]
      .mapAsync(1) { openid =>
        db.run(accountTable.filter(_.openid === openid).result.headOption)
      }
  }

  def incrmentMoneyToAccount()(implicit
      system: ActorSystem[_]
  ): Flow[AccountModel.AccountInfo, Boolean, NotUsed] = {
    val db: JdbcBackend.DatabaseDef = DataSource(system).source().db
    implicit val ec = system.executionContext
    implicit val slickSession: SlickSession =
      SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)
    import slickSession.profile.api._
    implicit val materializer = SystemMaterializer(system).materializer
    Flow[AccountModel.AccountInfo]
      .mapAsync(1) { info =>
        db.run(
            sqlu"""INSERT INTO douyinpay_account VALUE(${info.openid},${info.money}) ON DUPLICATE KEY UPDATE money=money+${info.money}"""
          )
          .map(_ == 1)
      }
  }

  def decrmentMoneyToAccount()(implicit
      system: ActorSystem[_]
  ): Flow[AccountModel.AccountInfo, Boolean, NotUsed] = {
    val db: JdbcBackend.DatabaseDef = DataSource(system).source().db
    implicit val ec = system.executionContext
    implicit val slickSession: SlickSession =
      SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)
    import slickSession.profile.api._
    implicit val materializer = SystemMaterializer(system).materializer
    Flow[AccountModel.AccountInfo]
      .mapAsync(1) { info =>
        db.run(
            sqlu"""update douyinpay_account set money=money-${info.money} where openid=${info.openid}"""
          )
          .map(_ == 1)
      }
  }

}
