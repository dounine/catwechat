package com.dounine.catwechat.tools.util

import java.util.UUID

object UUIDUtil {

  def uuid(): String = {
    UUID.randomUUID().toString.replaceAll("-", "")
  }

}
