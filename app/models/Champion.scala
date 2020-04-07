package models

import helpers.JsonHelper
import play.api.libs.json._

case class Champion(id: String,
                    key: String,
                    image: ChampionImage,
                    tags: Seq[String])

case class ChampionImage(full: String)

object ChampionImage {
  implicit val jsonFormat: Format[ChampionImage] = Json.format[ChampionImage]
}

object Champion extends JsonHelper {
  implicit val jsonFormat: Format[Champion] = Json.format[Champion]

  def parse(json: JsValue): Seq[Champion] = {
    (json \ "data").as[JsObject].fields.map { case (_, value) => value.as[Champion] }
  }
}