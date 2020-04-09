package apis

import com.google.inject.{ImplementedBy, Inject, Singleton}
import exceptions.ApiException
import helpers.LoggingHelper
import models.Champion
import play.api.http.Status

import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[DragonApiImpl])
trait DragonApi {

  def getChampions: Future[Seq[Champion]]
}

@Singleton
class DragonApiImpl @Inject()(apiClient: DragonApiClient)(implicit ec: ExecutionContext) extends DragonApi with LoggingHelper {

  override def getChampions: Future[Seq[Champion]] = {
    logger.debug("Getting champions from Data Dragon")

    apiClient.getChampions.get().map { response =>
      response.status match {
        case Status.OK =>
          val champions = Champion.parse(response.json)

          logger.debug(s"Successfully got ${champions.length} champions from Data Dragon")
          champions
        case status => throw ApiException(status, response.body)
      }
    }
  }
}
