package com.gameserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{handleExceptions, handleRejections}
import akka.http.scaladsl.server.{RejectionHandler, RouteResult}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import com.gameserver.route.GameServerRoute

import scala.concurrent.ExecutionContextExecutor

object GameServer {

  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.load()
  val apiHostName = "localhost"
  val apiPort = 8080

  implicit val system: ActorSystem = ActorSystem("app")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  log.info("Hello. This is the game server ...")

  def main(args: Array[String]): Unit = {

    // Creating http service
    val routeObj = new GameServerRoute()

    // Running akka http server
    val bindingFuture = {
      val handleErrors = handleRejections(RejectionHandler.default) & handleExceptions(GameServerExceptionHandler.apply())
      val finalRoute = handleErrors { routeObj.route  }
      Http().newServerAt(apiHostName, apiPort).bindFlow(RouteResult.routeToFlow(finalRoute))
    }
    log.info(s"Server online at http://$apiHostName:$apiPort")

    // register a shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      log.info("Inside shutdown hook of job")
      bindingFuture.flatMap(_.unbind()) // trigger unbinding from the port
    }))
  }
}


