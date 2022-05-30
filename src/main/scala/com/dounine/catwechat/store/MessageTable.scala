package com.dounine.catwechat.store

import com.dounine.catwechat.model.models.{MessageModel, PayModel}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.LocalDateTime

object MessageTable {
  def apply(): TableQuery[MessageTable] = TableQuery[MessageTable]
}
class MessageTable(tag: Tag)
    extends Table[MessageModel.MessageDbInfo](
      tag,
      _tableName = "wechat_listener_message"
    )
    with EnumMappers {

  override def * : ProvenShape[MessageModel.MessageDbInfo] =
    (
      id,
      text,
      `match`,
      listen,
      send,
      like,
      sendMessage,
      createTime
    ).mapTo[MessageModel.MessageDbInfo]

  def id: Rep[String] = column[String]("id", O.PrimaryKey, O.Length(32))

  def text: Rep[String] =
    column[String]("text", O.SqlType("text"))

  def `match`: Rep[String] =
    column[String]("match", O.Length(100))

  def like: Rep[Double] =
    column[Double]("like")

  def listen: Rep[Boolean] =
    column[Boolean]("listen", O.Length(1))

  def send: Rep[Boolean] =
    column[Boolean]("send", O.Length(1))

  def sendMessage: Rep[String] =
    column[String]("sendMessage", O.SqlType("text"))

  def createTime: Rep[LocalDateTime] =
    column[LocalDateTime](
      "createTime",
      O.SqlType(timestampOnCreate)
    )(
      localDateTime2timestamp
    )

}
