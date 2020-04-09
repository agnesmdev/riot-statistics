package models

import play.api.libs.json.{Format, Json}

case class Summoner(accountId: String, name: String, puuid: String)

object Summoner {
  implicit val jsonFormat: Format[Summoner] = Json.format[Summoner]
}
