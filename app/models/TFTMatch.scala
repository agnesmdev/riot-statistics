package models

import play.api.libs.json.{Json, Reads}

case class TFTMatch(info: TFTMatchInfo)

case class TFTMatchInfo(game_datetime: Long,
                        game_length: Double)

object TFTMatchInfo {
  implicit val jsonReads: Reads[TFTMatchInfo] = Json.reads[TFTMatchInfo]
}

object TFTMatch {
  implicit val jsonReads: Reads[TFTMatch] = Json.reads[TFTMatch]
}
