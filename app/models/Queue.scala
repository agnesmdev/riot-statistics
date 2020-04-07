package models

import play.api.libs.json._

case class Queue(queueId: Int,
                 description: Option[String],
                 map: String) {
  lazy val name: String = description.getOrElse(map)
}

object Queue {
  implicit val jsonReads: Reads[Queue] = json => for {
    id <- (json \ "queueId").validate[Int]
    description <- (json \ "description").validateOpt[String]
    map <- (json \ "map").validate[String]
  } yield  {
    Queue(id, description, map)
  }
}