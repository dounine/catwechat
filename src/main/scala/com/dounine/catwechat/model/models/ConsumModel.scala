package com.dounine.catwechat.model.models

import java.time.{LocalDate, LocalDateTime}

object ConsumModel {

  case class ConsumInfo(
      wxid: String,
      nickName: String,
      coin: Int,
      createTime: LocalDateTime
  ) extends BaseSerializer

}
