package com.dounine.catwechat.model.models

import java.time.{LocalDate, LocalDateTime}

object MsgLevelModel {

  case class MsgLevelInfo(
                           time: LocalDate,
                           wxid: String,
                           nickName: String,
                           level: Int,
                           coin: Int,
                           createTime: LocalDateTime
                         ) extends BaseSerializer

  case class LevelRequire(
                           level: Int,
                           name: String,
                           des: String,
                           msg: Int,
                           coin: Int
                         ) extends BaseSerializer

}
