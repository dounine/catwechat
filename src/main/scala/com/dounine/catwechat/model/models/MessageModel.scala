package com.dounine.catwechat.model.models

import java.time.LocalDateTime

object MessageModel {

  case class MessageDbInfo(
      id: String,
      text: String,
      `match`: String,
      listen: Boolean,
      send: Boolean,
      like: Double,
      useLike: Boolean,
      assistant: Boolean,
      messageType: String,
      sendMessage: String,
      createTime: LocalDateTime
  ) extends BaseSerializer

  case class MessageBody(
      text: String,
      `match`: String,
      listen: Boolean,
      send: Boolean,
      like: String,
      useLike: Boolean,
      assistant: Boolean,
      messageType: String,
      sendMessage: String
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

  case class AddressListData(
      friends: Seq[String],
      chatrooms: Seq[String],
      ghs: Seq[String],
      others: Seq[String]
  ) extends BaseSerializer

  case class AddressList(
      code: String,
      message: String,
      data: AddressListData
  ) extends BaseSerializer

  case class ContactData(
      userName: String,
      nickName: String,
      smallHead: String,
      v1: String
  ) extends BaseSerializer

  case class Contact(
      code: String,
      message: String,
      data: Seq[ContactData]
  ) extends BaseSerializer

  case class ChatRoomMemberData(
      chatRoomId: String,
      userName: String,
      nickName: String,
      inviterUserName: String,
      bigHeadImgUrl: String,
      displayName: Option[String],
      smallHeadImgUrl: String
  ) extends BaseSerializer

  case class ChatRoomMember(
      code: String,
      message: String,
      data: Seq[ChatRoomMemberData]
  ) extends BaseSerializer

}
