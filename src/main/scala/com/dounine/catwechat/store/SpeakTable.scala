package com.dounine.catwechat.store

import com.dounine.catwechat.model.models.{MessageModel, SpeakModel}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.{LocalDate, LocalDateTime}

object SpeakTable {
  def apply(): TableQuery[SpeakTable] = TableQuery[SpeakTable]
}
class SpeakTable(tag: Tag)
    extends Table[SpeakModel.SpeakInfo](
      tag,
      _tableName = "wechat_listener_speak"
    )
    with EnumMappers {

  override def * : ProvenShape[SpeakModel.SpeakInfo] =
    (
      date,
      group,
      wxid,
      nickName,
      sendMsg,
      createTime
    ).mapTo[SpeakModel.SpeakInfo]

  def date: Rep[LocalDate] =
    column[LocalDate]("date", O.SqlType("date"))

  def group: Rep[String] =
    column[String]("group", O.Length(50))

  def wxid: Rep[String] =
    column[String]("wxid", O.Length(50))

  def nickName: Rep[String] =
    column[String]("nickName", O.Length(50))

  def sendMsg: Rep[Int] =
    column[Int]("sendMsg")

  def createTime: Rep[LocalDateTime] =
    column[LocalDateTime]("createTime", O.SqlType(timestampOnUpdate))(
      localDateTime2timestamp
    )

  def idx =
    index(
      "wechat_listener_speak_uindx",
      (date, group, wxid),
      unique = true
    )
}
