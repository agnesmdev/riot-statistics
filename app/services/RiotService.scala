package services

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

import apis.{RiotApi, RiotTFTApi}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import helpers.FutureHelper
import models._

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[RiotServiceImpl])
trait RiotService {

  def getWastedTime(name: String, region: Region, email: String): Future[Duration]
}

@Singleton
class RiotServiceImpl @Inject()(riotApi: RiotApi, riotTFTApi: RiotTFTApi, mailjetService: MailjetService)
                               (implicit ec: ExecutionContext) extends RiotService with FutureHelper {

  private val durationByMatch: FiniteDuration = 1.seconds

  override def getWastedTime(name: String, region: Region, email: String): Future[Duration] = {
    logger.debug(s"Getting wasted time for summoner $name in region $region from Riot")

    val result = for {
      summoner <- riotApi.getSummoner(name)(region)
      matchesInfo <- riotApi.getSummonerMatches(summoner.accountId)(region)
      tftMatches <- riotTFTApi.getSummonerMatches(summoner.puuid)(region)
    } yield {
      (matchesInfo, tftMatches)
    }

    result.map { case (matchesInfo, tftMatches) =>
      computeWastedTime(matchesInfo, tftMatches, name, region, email)
      ((matchesInfo.length + tftMatches.length) * durationByMatch.toSeconds).seconds
    }
  }

  private def computeWastedTime(matchesInfo: Seq[MatchInfo], tftMatches: Seq[String], name: String, region: Region, email: String): Future[WastedTime] = {
    val result = for {
      matches <- oneByOne(matchesInfo)(info => riotApi.getMatch(info.gameId)(region))
      tftMatches <- oneByOne(tftMatches)(id => riotTFTApi.getMatch(id)(region))
      wastedTime = extractWastedTime(name, matches)
      tftWastedTime = extractTFTWastedTime(name, tftMatches.map(_.info))
      _ <- mailjetService.sendWastedTime(wastedTime, tftWastedTime, email)
    } yield {
      wastedTime
    }

    result.recover {
      case e =>
        logger.warn(s"Failed to get wasted time for summoner $name in region $region from Riot", e)
        throw e
    }
  }

  private def extractWastedTime(summonerName: String, matches: Seq[Match]): WastedTime = matches match {
    case Nil => WastedTime.empty(summonerName, NormalType)
    case _ =>
      val gameTotal = matches.map(_.gameDuration).sum
      val firstGame = matches.minBy(_.gameCreation).gameCreation.milliseconds
      val begin = LocalDateTime.ofEpochSecond(firstGame.toSeconds, 0, UTC)

      WastedTime(summonerName, matches.length, TimeData(gameTotal), begin, NormalType)
  }

  private def extractTFTWastedTime(summonerName: String, matches: Seq[TFTMatchInfo]): WastedTime = matches match {
    case Nil => WastedTime.empty(summonerName, TFTType)
    case _ =>
      val gameTotal = matches.map(_.game_length).sum.toLong
      val firstGame = matches.minBy(_.game_datetime).game_datetime.milliseconds
      val begin = LocalDateTime.ofEpochSecond(firstGame.toSeconds, 0, UTC)

      WastedTime(summonerName, matches.length, TimeData(gameTotal), begin, TFTType)
  }
}