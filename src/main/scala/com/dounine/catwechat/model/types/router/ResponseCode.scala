package com.dounine.catwechat.model.types.router

import com.dounine.catwechat.model.types.router

object ResponseCode extends Enumeration {
  type ResponseCode = Value

  val ok: router.ResponseCode.Value = Value("ok")
  val fail: router.ResponseCode.Value = Value("fail")

}
