package com.dounine.catwechat.model.models

import java.time.{LocalDate, LocalDateTime}

object SpeakModel {

  case class SpeakInfo(
      date: LocalDate,
      group: String,
      wxid: String,
      nickName: String = "",
      sendMsg: Int = 0,
      createTime: LocalDateTime
  ) extends BaseSerializer
}
