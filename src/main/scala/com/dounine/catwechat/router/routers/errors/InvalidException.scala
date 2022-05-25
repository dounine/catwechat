package com.dounine.catwechat.router.routers.errors

case class InvalidException(msg: String) extends Exception(msg)
