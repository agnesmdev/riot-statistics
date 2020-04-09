package models

import java.time.LocalDateTime

import play.api.libs.json.{Json, Reads}


case class MatchList(totalGames: Int,
                     matches: Seq[MatchInfo],
                     endIndex: Int) {
  lazy val isLast: Boolean = totalGames == endIndex
}

case class MatchInfo(gameId: Long,
                     champion: Int,
                     role: Option[String],
                     lane: Option[String],
                     timestamp: Long)

case class Match(gameId: Long,
                 queueId: Int,
                 gameDuration: Long,
                 gameCreation: Long,
                 seasonId: Int)

case class MatchFull(champion: Champion,
                     queue: Queue,
                     role: String,
                     lane: String,
                     duration: Long,
                     loading: Long,
                     season: Season,
                     date: LocalDateTime)

object MatchInfo {
  implicit val jsonReads: Reads[MatchInfo] = Json.reads[MatchInfo]
}

object Match {
  implicit val jsonReads: Reads[Match] = Json.reads[Match]
}

object MatchList {
  implicit val jsonReads: Reads[MatchList] = Json.reads[MatchList]
}
