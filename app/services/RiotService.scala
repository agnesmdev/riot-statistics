package services

import apis.RiotApi
import com.google.inject.{ImplementedBy, Inject, Singleton}
import helpers.FutureHelper
import models.{MatchInfo, Region, WastedTime}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[RiotServiceImpl])
trait RiotService {

  def getWastedTime(name: String, region: Region, email: String): Future[Duration]
}

@Singleton
class RiotServiceImpl @Inject()(riotApi: RiotApi, mailJetApi: MailjetService)(implicit ec: ExecutionContext) extends RiotService with FutureHelper {

  private val durationByMatch: FiniteDuration = 1.seconds

  override def getWastedTime(name: String, region: Region, email: String): Future[Duration] = {
    logger.debug(s"Getting wasted time for summoner $name in region $region from Riot")

    val result = for {
      summoner <- riotApi.getSummoner(name)(region)
      matchesInfo <- riotApi.getSummonerMatches(summoner.accountId)(region)
    } yield {
      matchesInfo
    }

    result.map(computeWastedTime(_, name, region, email))
    result.map(m => (m.length * durationByMatch.toSeconds).seconds)
  }

  private def computeWastedTime(matchesInfo: Seq[MatchInfo], name: String, region: Region, email: String): Future[WastedTime] = {
    val result = for {
      matches <- oneByOne(matchesInfo)(info => riotApi.getMatch(info.gameId)(region))
      wastedTime = WastedTime(name, matches)
      _ <- mailJetApi.sendWastedTime(wastedTime, email)
    } yield {
      wastedTime
    }

    result.recover {
      case e =>
        logger.warn(s"Failed to get wasted time for summoner $name in region $region from Riot", e)
        throw e
    }
  }
}