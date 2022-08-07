package com.gameserver.game

object Game extends Enumeration {
  type Game = Value

  val SingleCardGame = Value("single-card-game")
  val DoubleCardGame = Value("double-card-game")

  val SingleCardGameS = SingleCardGame.toString
  val DoubleCardGameS = DoubleCardGame.toString

  val all = List(SingleCardGame, DoubleCardGame)
  val allS = all.map(_.toString)

}

object PlayerDecision extends Enumeration {
  type PlayerDecision = Value

  val fold  = Value("fold")
  val play  = Value("play")

  val foldS  = fold.toString
  val playS  = play.toString

  val all = List(fold, play)
  val allS = all.map(_.toString)
}