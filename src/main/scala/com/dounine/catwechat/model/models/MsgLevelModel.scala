package com.dounine.catwechat.model.models

import akka.actor.Cancellable

import java.time.{LocalDate, LocalDateTime}

object MsgLevelModel {

  case class DownInfo(
      groupId:String,
      coin:Int,
      des:Option[String] = None
  ) extends BaseSerializer

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

  case class CoinUserInfo(
      wxid: String,
      nickName: String
  ) extends BaseSerializer

  case class CoinInfo(
      coin: Int,
      settle: Boolean = false,
      createTime: LocalDateTime,
      result: Option[CoinUserInfo] = None,
      isPick: Boolean = true,
      pick: Option[CoinUserInfo] = None,
      pickSchedule: Option[Cancellable] = None,
      robs: Array[CoinUserInfo] = Array.empty,
      robSchedule: Option[Cancellable] = None
  ) extends BaseSerializer

}
