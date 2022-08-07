package com.gameserver.route

import javax.ws.rs.Path

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.gameserver.ErrorResponse
import com.gameserver.player.{ActivePlayers, Player, PlayerStatus}
import com.gameserver.single_card_game._
import io.swagger.annotations._
import org.slf4j.LoggerFactory
import spray.json._
import java.util.UUID

import com.gameserver.deck.Deck
import com.gameserver.double_card_game.{DoubleCardGameDealer, DoubleCardGamePlayer}
import com.gameserver.game.{DealCards, Game}
import com.gameserver.game.Game.Game

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

case class DummyResponse(respose: String)

trait GameServerJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val dummyResponseFormat = jsonFormat1(DummyResponse)
  implicit val errorResponseFormat = jsonFormat1(ErrorResponse)

}

@Api(value = "Game Server", produces = "application/json")
@Path("")
class GameServerRoute(implicit sys: ActorSystem, mat: ActorMaterializer, ex: ExecutionContextExecutor) extends GameServerJsonSupport {
  val logger = LoggerFactory.getLogger(this.getClass)

  val route: Route = pathPrefix("game-server" / "v1") {
    dummy ~ getWebSocket
  }

  def dummy = path("dummy"){
    get {
      complete(StatusCodes.OK, DummyResponse("Hi. The server is running..."))
    }
  }


  def getWebSocket = path("ws"){
    extractWebSocketUpgrade {
      upgrade =>

        val playerId = UUID.randomUUID().toString
        logger.info(s"New player connected: $playerId")

        // Creating out source
        val outSource: Source[TextMessage, ActorRef] = {
          Source.actorRef[TextMessage](1000000, OverflowStrategy.dropHead) map {
            payload: TextMessage => payload
          } mapMaterializedValue {
            ref =>
              val player = Player(playerId, ref)
              ActivePlayers.add(player)
              player.sendWelcomeMesssage
              player.sendChooseGameMessage

              ref
          }
        }

        // Creating in sink
        val inSink: Sink[Any, Future[Done]] = Sink.foreach[Any]{
          case input: TextMessage if Game.allS.contains(input.getStrictText.trim) =>
            logger.info(s"Chosen game by player ${playerId} is ${input.getStrictText}")
            val playerOpt = ActivePlayers.getPlayerById(playerId)
            val game: Game = Game.withName(input.getStrictText)

            playerOpt.foreach(_.gameBeingPlayed = Some(game))
            playerOpt.foreach(_.sendMessage(s"Game chosen is $game"))
            playerOpt.foreach(_.sendMessage(s"Please wait while we find you an apponent"))

            val activePlayersQueue = ActivePlayers.getPlayerQueue(game)

            activePlayersQueue.dequeueFirst(_.status == PlayerStatus.IDLE).foreach{
              player1 =>
                activePlayersQueue.dequeueFirst(_.status == PlayerStatus.IDLE).foreach{
                  player2 =>

                    // calulating game actor player1, game actor player2 & dealer actor
                    val (dealerActorRef, player1ActorRef, player2ActorRef) = game match {
                      case Game.SingleCardGame =>
                        val dealerActorRef = SingleCardGameDealer.createActor(new Deck, player1, player2)
                        (
                          dealerActorRef,
                          SingleCardGamePlayer.createActor(player1, dealerActorRef),
                          SingleCardGamePlayer.createActor(player2, dealerActorRef)
                        )
                      case Game.DoubleCardGame =>
                        val dealerActorRef = DoubleCardGameDealer.createActor(new Deck, player1, player2)
                        (
                          dealerActorRef,
                          DoubleCardGamePlayer.createActor(player1, dealerActorRef),
                          DoubleCardGamePlayer.createActor(player2, dealerActorRef)
                        )
                    }

                    // Setting up game actor references and player status
                    player1.gameActorRef = Some(player1ActorRef)
                    player2.gameActorRef = Some(player2ActorRef)

                    player1.status = PlayerStatus.PLAYING
                    player2.status = PlayerStatus.PLAYING

                    dealerActorRef ! DealCards
                }
            }

          case input: TextMessage =>
            val playerOpt = ActivePlayers.getPlayerById(playerId)
            // Forwarding the input player choices to the game actor if the game has begun
            playerOpt.flatMap(_.gameActorRef).map(_ ! input).getOrElse {
              logger.info(s"Unknown input ${input.getStrictText}")
              playerOpt.foreach(_.socketActorRef ! input)
            }
        }

        complete(upgrade.handleMessagesWithSinkSource(inSink, outSource))
    }
  }
}