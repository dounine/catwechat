package com.dounine.catwechat.router.routers.errors

case class AuthException(msg: String) extends Exception(msg)
