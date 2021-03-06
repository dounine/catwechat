package com.dounine.catwechat.store

import com.dounine.catwechat.model.models.{PayModel, OpenidModel}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.LocalDateTime

object OpenidTable {
  def apply(): TableQuery[OpenidTable] = TableQuery[OpenidTable]
}
class OpenidTable(tag: Tag)
    extends Table[OpenidModel.OpenidInfo](tag, _tableName = "douyinpay_openid")
    with EnumMappers {

  override def * : ProvenShape[OpenidModel.OpenidInfo] =
    (
      appid,
      openid,
      ccode,
      ip,
      locked,
      createTime
    ).mapTo[OpenidModel.OpenidInfo]

  def appid: Rep[String] =
    column[String]("appid")

  def openid: Rep[String] =
    column[String]("openid", O.PrimaryKey, O.Length(100))

  def ccode: Rep[String] =
    column[String]("ccode", O.Length(100))

  def ip: Rep[String] =
    column[String]("ip", O.Length(15))

  def locked: Rep[Boolean] =
    column[Boolean]("locked", O.Length(1))

  def createTime: Rep[LocalDateTime] =
    column[LocalDateTime](
      "createTime",
      O.SqlType(timestampOnCreate)
    )(
      localDateTime2timestamp
    )

}
