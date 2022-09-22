package com.dounine.catwechat.model.models

import java.time.{LocalDate, LocalDateTime}

object CheckModel {

  case class CheckInfo(
      time: LocalDate,
      wxid: String,
      nickName: String = "",
      createTime: LocalDateTime
  ) extends BaseSerializer
}
