package com.dounine.catwechat.router.routers.errors

case class LockedException(msg: String) extends Exception(msg)
