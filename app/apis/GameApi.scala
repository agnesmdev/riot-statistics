package apis

import com.google.inject.{ImplementedBy, Inject, Singleton}
import exceptions.ApiException
import helpers.LoggingHelper
import models.{Queue, Season}
import play.api.http.Status

import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[GameApiImpl])
trait GameApi {

  def getQueues: Future[Seq[Queue]]

  def getSeasons: Future[Seq[Season]]
}

@Singleton
class GameApiImpl @Inject()(apiClient: GameApiClient)(implicit ec: ExecutionContext) extends GameApi with LoggingHelper {

  override def getQueues: Future[Seq[Queue]] = {
    logger.debug("Getting queues from Game Constants")

    apiClient.getQueues.get().map { response =>
      response.status match {
        case Status.OK =>
          val queues = response.json.as[Seq[Queue]]

          logger.debug(s"Successfully got ${queues.length} queues from Game Constants")
          queues
        case status => throw ApiException(status, response.body)
      }
    }
  }

  override def getSeasons: Future[Seq[Season]] = {
    logger.debug("Getting seasons from Game Constants")

    apiClient.getSeasons.get().map { response =>
      response.status match {
        case Status.OK =>
          val seasons = response.json.as[Seq[Season]]

          logger.debug(s"Successfully got ${seasons.length} seasons from Game Constants")
          seasons
        case status => throw ApiException(status, response.body)
      }
    }
  }
}
