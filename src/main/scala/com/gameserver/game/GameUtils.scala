package com.gameserver.game

case object DealCards
case object BeginGame
case object EndGame
case class GameResult(isWinner: Option[Boolean], prize: Long)
