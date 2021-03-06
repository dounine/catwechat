package com.dounine.catwechat.model.models

import com.dounine.catwechat.model.types.router.ResponseCode
import com.dounine.catwechat.model.types.router.ResponseCode.ResponseCode

object RouterModel {

  sealed trait JsonData

  case class Data(
      data: Option[Any] = None,
      msg: Option[String] = None,
      status: ResponseCode = ResponseCode.ok
  ) extends JsonData

  case class Ok(
      status: ResponseCode = ResponseCode.ok
  ) extends JsonData

  case class Fail(
      msg: Option[String] = None,
      status: ResponseCode = ResponseCode.fail
  ) extends JsonData

}
