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


@ImplementedBy(classOf[RiotTFTApiImpl])
trait RiotTFTApi {

  def getSummonerMatches(puuid: String)(region: Region): Future[Seq[String]]

  def getMatch(matchId: String)(region: Region): Future[TFTMatch]
}

@Singleton
class RiotTFTApiImpl @Inject()(apiClient: RiotTFTApiClient, system: ActorSystem, configuration: Configuration)
                              (implicit ec: ExecutionContext) extends RiotTFTApi with RetryHelper {

  override val maxRetries: Int = configuration.get[Int]("riot.api.max.retries")
  override val unavailableDelay: FiniteDuration = configuration.get[FiniteDuration]("riot.api.unavailable.delay")
  override val tooMuchRequestsDelay: FiniteDuration = configuration.get[FiniteDuration]("riot.api.too.much.delay")

  implicit val scheduler: Scheduler = system.scheduler

  override def getSummonerMatches(puuid: String)(region: Region): Future[Seq[String]] = {
    logger.debug(s"Getting TFT matches for puuid $puuid and region ${region.globalValue} from Riot")

    val action = () => apiClient.getSummonerMatches(puuid)(region.globalHost).get().map { response =>
      response.status match {
        case Status.OK =>
          val ids = response.json.as[Seq[String]]

          logger.debug(s"Successfully got ${ids.length} matches for puuid $puuid and region ${region.globalValue} from Riot")
          ids
        case Status.TOO_MANY_REQUESTS => throw TooManyRequestsException(response.body)
        case status => throw ApiException(status, response.body)
      }
    }

    retry(action)
  }

  override def getMatch(matchId: String)(region: Region): Future[TFTMatch] = {
    logger.debug(s"Getting TFT match $matchId info in region ${region.globalValue} from Riot")

    val action = () => apiClient.getMatch(matchId)(region.globalHost).get().flatMap { response =>
      response.status match {
        case Status.OK =>
          val matchResponse = response.json.as[TFTMatch]

          logger.debug(s"Successfully got TFT match $matchId info in region ${region.globalValue} from Riot")
          Future.successful(matchResponse)
        case Status.NOT_FOUND => throw NotFoundException(s"Match $matchId")
        case Status.TOO_MANY_REQUESTS => throw TooManyRequestsException(response.body)
        case status => throw ApiException(status, response.body)
      }
    }

    retry(action)
  }
}
