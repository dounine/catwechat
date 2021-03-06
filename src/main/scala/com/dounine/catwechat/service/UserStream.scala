package com.dounine.catwechat.service

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Flow, Source}
import com.dounine.catwechat.model.models.UserModel
import com.dounine.catwechat.store.{OrderTable, UserTable}
import com.dounine.catwechat.tools.akka.db.DataSource
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContextExecutor

object UserStream {

  def source(
      apiKey: String
  )(implicit system: ActorSystem[_]): Source[UserModel.DbInfo, NotUsed] = {
    val db: JdbcBackend.DatabaseDef = DataSource(system).source().db
    implicit val ec: ExecutionContextExecutor = system.executionContext
    implicit val slickSession: SlickSession =
      SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)
    import slickSession.profile.api._

    Slick.source(
      UserTable().filter(_.apiKey === apiKey).result
    )
  }

  def queryFlow()(implicit
      system: ActorSystem[_]
  ): Flow[String, Either[Throwable, UserModel.DbInfo], NotUsed] = {
    Flow[String]
      .flatMapConcat(source(_: String))
      .map(Right.apply)
      .recover {
        case ee => Left(ee)
      }

  }

}
