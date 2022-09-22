package com.dounine.catwechat.store

import com.dounine.catwechat.model.models.{MsgLevelModel, SpeakModel}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.{LocalDate, LocalDateTime}


object MsgLevelTable {
  def apply(): TableQuery[MsgLevelTable] = TableQuery[MsgLevelTable]
}

class MsgLevelTable(tag: Tag)
  extends Table[MsgLevelModel.MsgLevelInfo](
    tag,
    _tableName = "wechat_listener_msg_level"
  )
    with EnumMappers {

  override def * : ProvenShape[MsgLevelModel.MsgLevelInfo] =
    (
      time,
      wxid,
      nickName,
      level,
      coin,
      createTime
      ).mapTo[MsgLevelModel.MsgLevelInfo]

  def time: Rep[LocalDate] =
    column[LocalDate]("time", O.SqlType("date"))

  def wxid: Rep[String] =
    column[String]("wxid", O.Length(50))

  def nickName: Rep[String] =
    column[String]("nickName", O.Length(50))

  def level: Rep[Int] =
    column[Int]("level")

  def coin: Rep[Int] =
    column[Int]("coin")

  def createTime: Rep[LocalDateTime] =
    column[LocalDateTime]("createTime", O.SqlType(timestampOnUpdate))(
      localDateTime2timestamp
    )

  def pk =
    primaryKey(
      "wechat_listener_msg_level_pk",
      (time, wxid, level)
    )

  def idx_time_group =
    index(
      "wechat_listener_msg_level_time_group_uindx",
      (time, wxid),
      unique = false
    )
}
