package models

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

import play.api.libs.json.{Json, Writes}

import scala.concurrent.duration._

case class WastedTime(summonerName: String,
                      numberOfGames: Int,
                      gameTime: TimeData,
                      begin: LocalDateTime) {
  lazy val isEmpty: Boolean = numberOfGames == 0
}

case class TimeData(total: Long,
                    days: Long,
                    hours: Long,
                    minutes: Long,
                    seconds: Long)

object TimeData {
  val empty: TimeData = TimeData(0, 0, 0, 0, 0)

  implicit val jsonWrites: Writes[TimeData] = Json.writes[TimeData]

  def apply(total: Long): TimeData = {
    val duration = total.seconds

    val days = duration.toDays
    val hours = duration.minus(days.days).toHours
    val minutes = duration.minus(days.days).minus(hours.hours).toMinutes
    val seconds = duration.minus(days.days).minus(hours.hours).minus(minutes.minutes).toSeconds

    TimeData(total, days, hours, minutes, seconds)
  }
}

object WastedTime {
  def apply(summonerName: String, matches: Seq[Match]): WastedTime = matches match {
    case Nil => empty(summonerName)
    case _ =>
      val gameTotal = matches.map(_.gameDuration).sum
      val begin = LocalDateTime.ofEpochSecond(matches.minBy(_.gameCreation).gameCreation, 0, UTC)

      WastedTime(summonerName, matches.length, TimeData(gameTotal), begin)
  }

  def empty(summonerName: String): WastedTime = WastedTime(summonerName, 0, TimeData.empty, LocalDateTime.now(UTC))

  implicit val jsonWrites: Writes[WastedTime] = Json.writes[WastedTime]
}


