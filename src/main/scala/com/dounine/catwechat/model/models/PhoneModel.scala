package com.dounine.catwechat.model.models

import java.time.LocalDateTime

object PhoneModel {

  case class PhoneNotification(
      pck: String,
      text: String,
      time: Long,
      title: String,
      number: Int
  ) extends BaseSerializer
}
