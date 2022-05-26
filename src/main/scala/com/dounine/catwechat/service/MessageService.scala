package com.dounine.catwechat.service

import akka.actor.typed.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.dounine.catwechat.model.models.{
  MessageModel,
  OrderModel,
  PayUserInfoModel
}
import com.dounine.catwechat.model.types.service.PayPlatform.PayPlatform
import com.dounine.catwechat.store.{EnumMappers, MessageTable, OrderTable}
import com.dounine.catwechat.tools.akka.cache.CacheSource
import com.dounine.catwechat.tools.akka.db.DataSource
import com.dounine.catwechat.tools.util.Request
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import scala.concurrent.duration._
import java.time.LocalDateTime
import scala.concurrent.Future

class MessageService(implicit system: ActorSystem[_]) extends EnumMappers {
  private val db = DataSource(system).source().db
  private val dict: TableQuery[MessageTable] = MessageTable()

  implicit val ec = system.executionContext
  implicit val materializer = SystemMaterializer(system).materializer
  implicit val slickSession =
    SlickSession.forDbAndProfile(db, slick.jdbc.MySQLProfile)

  private val cacheKey = "messages"
  private val messageUrl = system.settings.config.getString("app.messageUrl")
  private val wId = system.settings.config.getString("app.wId")
  private val authorization =
    system.settings.config.getString("app.authorization")

  def queryById(id: String): Future[Option[MessageModel.MessageDbInfo]] = {
    db.run(dict.filter(db => db.id === id).result.headOption)
  }

  def insertOrUpdate(data: MessageModel.MessageDbInfo): Future[Int] = {
    db.run(
        dict.insertOrUpdate(
          data.copy(
            createTime = LocalDateTime.now()
          )
        )
      )
      .flatMap((result: Int) => {
        CacheSource(system)
          .cache()
          .remove(cacheKey)
          .map(_ => result)
      })
  }

  def deleteById(id: String): Future[Int] = {
    db.run(
        dict.filter(_.id === id).delete
      )
      .flatMap((result: Int) => {
        CacheSource(system)
          .cache()
          .remove(cacheKey)
          .map(_ => result)
      })

  }

  def all(): Future[Seq[MessageModel.MessageDbInfo]] = {
    CacheSource(system)
      .cache()
      .orElse[Seq[MessageModel.MessageDbInfo]](
        key = cacheKey,
        ttl = 1.days,
        value = () =>
          db.run(
            dict.result
          )
      )
  }

  /**
    * 获取群列表
    * @return
    */
  def chatrooms(): Future[Seq[String]] = {
    CacheSource(system)
      .cache()
      .orElse[Seq[String]](
        key = "chatrooms",
        ttl = 1.days,
        value = () => {
          Request
            .post[MessageModel.AddressList](
              s"${messageUrl}/getAddressList",
              Map(
                "wId" -> wId
              ),
              Map(
                "Authorization" -> authorization
              )
            )
            .map(_.data.chatrooms)
        }
      )
  }

  /**
    * 通讯录信息
    * @return
    */
  def contacts(wcIds: Seq[String]): Future[Seq[MessageModel.ContactData]] = {
    CacheSource(system)
      .cache()
      .orElse[Seq[MessageModel.ContactData]](
        key = "contacts_" + wcIds.mkString(","),
        ttl = 1.days,
        value = () => {
          Request
            .post[MessageModel.Contact](
              s"${messageUrl}/getContact",
              Map(
                "wId" -> wId,
                "wcId" -> wcIds.mkString(",")
              ),
              Map(
                "Authorization" -> authorization
              )
            )
            .map(_.data)
        }
      )
  }

}
