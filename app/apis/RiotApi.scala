package apis

import akka.actor.{ActorSystem, Scheduler}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import exceptions.{ApiException, NotFoundException, TooManyRequestsException}
import helpers.RetryHelper
import models._
import play.api.Configuration
import play.api.http.Status

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[RiotApiImpl])
trait RiotApi {

  def getSummoner(name: String)(region: Region): Future[Summoner]

  def getSummonerMatches(accountId: String)(region: Region): Future[Seq[MatchInfo]]

  def getMatch(matchId: Long)(region: Region): Future[Match]
}

@Singleton
class RiotApiImpl @Inject()(apiClient: RiotApiClient, system: ActorSystem, configuration: Configuration)
                           (implicit ec: ExecutionContext) extends RiotApi with RetryHelper {

  override val maxRetries: Int = configuration.get[Int]("riot.api.max.retries")
  override val unavailableDelay: FiniteDuration = configuration.get[FiniteDuration]("riot.api.unavailable.delay")
  override val tooMuchRequestsDelay: FiniteDuration = configuration.get[FiniteDuration]("riot.api.too.much.delay")

  implicit val scheduler: Scheduler = system.scheduler

  override def getSummoner(name: String)(region: Region): Future[Summoner] = {
    logger.debug(s"Getting summoner with name $name in region ${region.value} from Riot")

    val action = () => apiClient.getSummoner(name)(region.host).get().flatMap { response =>
      response.status match {
        case Status.OK =>
          val summoner = response.json.as[Summoner]

          logger.debug(s"Successfully got summoner ${summoner.accountId} with name $name from Riot")
          Future.successful(summoner)
        case Status.NOT_FOUND => throw NotFoundException(s"Summoner $name")
        case Status.TOO_MANY_REQUESTS => throw TooManyRequestsException(response.body)
        case status => throw ApiException(status, response.body)
      }
    }

    retry(action)
  }

  override def getSummonerMatches(accountId: String)(region: Region): Future[Seq[MatchInfo]] = {
    logger.debug(s"Getting matches for account $accountId and region $region from Riot")

    getMatches(accountId, region)
  }

  override def getMatch(matchId: Long)(region: Region): Future[Match] = {
    logger.debug(s"Getting match $matchId info in region $region from Riot")

    val action = () => apiClient.getMatch(matchId)(region.host).get().flatMap { response =>
      response.status match {
        case Status.OK =>
          val matchResponse = response.json.as[Match]

          logger.debug(s"Successfully got match $matchId info in region $region from Riot")
          Future.successful(matchResponse)
        case Status.NOT_FOUND => throw NotFoundException(s"Match $matchId")
        case Status.TOO_MANY_REQUESTS => throw TooManyRequestsException(response.body)
        case status => throw ApiException(status, response.body)
      }
    }

    retry(action)
  }

  private def getMatches(accountId: String, region: Region, beginIndex: Int = 0, result: Seq[MatchInfo] = Nil): Future[Seq[MatchInfo]] = {
    val action = () => apiClient.getSummonerMatches(accountId, beginIndex)(region.host).get().flatMap { response =>
      response.status match {
        case Status.OK =>
          val matchList = response.json.as[MatchList]
          val matches = matchList.matches
          logger.debug(s"Successfully got ${matches.length} matches for account $accountId and region $region")
          if (matchList.isLast) {
            Future.successful(result ++ matchList.matches)
          } else {
            getMatches(accountId, region, beginIndex + 100, result ++ matches)
          }
        case Status.NOT_FOUND => Future.successful(result)
        case Status.TOO_MANY_REQUESTS => throw TooManyRequestsException(response.body)
        case status => throw ApiException(status, response.body)
      }
    }

    retry(action)
  }
}
