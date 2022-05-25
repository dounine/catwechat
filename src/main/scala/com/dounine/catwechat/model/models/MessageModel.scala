package com.dounine.catwechat.model.models

import java.time.LocalDateTime

object MessageModel {

  case class MessageDbInfo(
      id: String,
      text: String,
      `match`: String,
      listen: Boolean,
      send: Boolean,
      sendMessage: String,
      createTime: LocalDateTime
  ) extends BaseSerializer

  case class MessageBody(
      text: String,
      `match`: String,
      listen: Boolean,
      send: Boolean,
      sendMessage: String
  ) extends BaseSerializer

  case class MessageInfo(
      userName: String,
      nickName: String,
      smallHead: String,
      v1: String
  ) extends BaseSerializer

  case class MessageData(
      fromUser: String,
      fromGroup: Option[String] = None,
      toUser: String,
      msgId: Long,
      newMsgId: Long,
      timestamp: Long,
      content: String,
      self: Boolean
  ) extends BaseSerializer

  case class Message(
      wcId: String,
      account: String,
      messageType: String,
      data: MessageData
  ) extends BaseSerializer

}
