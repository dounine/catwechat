package com.dounine.catwechat.model.models

object MessageDing {

  case class Data(
      title: String,
      text: String,
      code: Option[String] = None
  )

}
