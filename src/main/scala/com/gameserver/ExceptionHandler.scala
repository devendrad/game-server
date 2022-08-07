package com.gameserver

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import com.gameserver.route.GameServerJsonSupport
import org.slf4j.LoggerFactory

case class BadRequestException(message: String) extends Exception(message)
case class ErrorResponse(message: String)


object GameServerExceptionHandler extends GameServerJsonSupport {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  def apply() = ExceptionHandler {
    case e@BadRequestException(message) =>
        log.error(s"Bad Request exception caused by " + e.getMessage)
        complete(StatusCodes.BadRequest, ErrorResponse(message))
  }

}
