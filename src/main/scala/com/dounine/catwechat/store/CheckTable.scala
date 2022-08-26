package com.dounine.catwechat.store

import com.dounine.catwechat.model.models.{CheckModel, SpeakModel}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.{LocalDate, LocalDateTime}


object CheckTable {
  def apply(): TableQuery[CheckTable] = TableQuery[CheckTable]
}
class CheckTable(tag: Tag)
    extends Table[CheckModel.CheckInfo](
      tag,
      _tableName = "wechat_listener_check"
    )
    with EnumMappers {

  override def * : ProvenShape[CheckModel.CheckInfo] =
    (
      time,
      group,
      wxid,
      nickName,
      createTime
    ).mapTo[CheckModel.CheckInfo]

  def time: Rep[LocalDate] =
    column[LocalDate]("date", O.SqlType("date"))(localDate2Date)

  def group: Rep[String] =
    column[String]("group", O.Length(50))

  def wxid: Rep[String] =
    column[String]("wxid", O.Length(50))

  def nickName: Rep[String] =
    column[String]("nickName", O.Length(50))

  def createTime: Rep[LocalDateTime] =
    column[LocalDateTime]("createTime", O.SqlType(timestampOnCreate))(
      localDateTime2timestamp
    )

  def pk =
    primaryKey(
      "wechat_listener_check_pk",
      (time, group, wxid)
    )

  def idx_date_group =
    index(
      "wechat_listener_check_time_group_uindx",
      (time, group),
      unique = false
    )
}
