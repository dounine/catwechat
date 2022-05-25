package com.dounine.catwechat.model.models

import com.dounine.catwechat.model.types.service.PayPlatform.PayPlatform
import com.dounine.catwechat.model.types.service.PayStatus.PayStatus
import slick.lifted.Rep

import java.time.LocalDateTime

object UserModel {

  final case class DbInfo(
      apiKey: String,
      apiSecret: String,
      balance: BigDecimal,
      margin: BigDecimal,
      callback: Option[String],
      createTime: LocalDateTime
  ) extends BaseSerializer

  final case class UpdateDbInfo(
      apiKey: Rep[String],
      balance: Rep[BigDecimal]
  ) extends BaseSerializer


}
