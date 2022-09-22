package com.dounine.catwechat.store

import com.dounine.catwechat.model.models.{ConsumModel, SpeakModel}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.{LocalDate, LocalDateTime}

object ConsumTable {
  def apply(): TableQuery[ConsumTable] = TableQuery[ConsumTable]
}
class ConsumTable(tag: Tag)
    extends Table[ConsumModel.ConsumInfo](
      tag,
      _tableName = "wechat_listener_consum"
    )
    with EnumMappers {

  override def * : ProvenShape[ConsumModel.ConsumInfo] =
    (
      wxid,
      nickName,
      coin,
      createTime
    ).mapTo[ConsumModel.ConsumInfo]

  def wxid: Rep[String] =
    column[String]("wxid", O.Length(50))

  def nickName: Rep[String] =
    column[String]("nickName", O.Length(50))

  def coin: Rep[Int] =
    column[Int]("coin")

  def createTime: Rep[LocalDateTime] =
    column[LocalDateTime]("createTime", O.SqlType(timestampOnUpdate))(
      localDateTime2timestamp
    )

  def idx_consum_group =
    index(
      "wechat_listener_consum_group_wxid_uindx",
      ( wxid),
      unique = false
    )
}
