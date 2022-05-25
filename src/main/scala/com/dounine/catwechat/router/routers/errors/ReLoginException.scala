package com.dounine.catwechat.router.routers.errors

case class ReLoginException(msg: String, appid: Option[String] = None)
    extends Exception(msg)
