package models

import play.api.libs.json.{Format, Json}

case class Season(id: Int, season: String)

object Season {
  implicit val jsonFormat: Format[Season] = Json.format[Season]
}
