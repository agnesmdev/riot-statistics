package controllers

import exceptions._
import helpers.{JsonHelper, LoggingHelper}
import javax.inject.Inject
import models.Region
import play.api.mvc._
import services.RiotService
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.{ExecutionContext, Future}

class RiotController @Inject()(cc: ControllerComponents, riotService: RiotService)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with LoggingHelper with JsonHelper {

  def getWastedTime(name: String, region: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Computing wasting time on LoL for summoner $name on region $region")

    validateRegionAndMail(region, request).flatMap {
      case (r, email) => riotService.getWastedTime(name, r, email).map { time =>
        logger.info(s"Successfully began computing wasted time for summoner $name on region $region, estimated time: $time")
        Accepted(s"Computing wasted time, estimated time: ${time.toMinutes} minutes, exactly ${time.toSeconds} seconds")
      }
    }.recover {
      case e: NotFoundException =>
        logger.warn(s"Failed to compute wasting time, error: ${e.getMessage}", e)
        NotFound(e.getMessage)
      case e: FunctionalException =>
        logger.warn(s"Failed to compute wasting time, error: ${e.getMessage}", e)
        BadRequest(e.getMessage)
      case e: TechnicalException =>
        logger.warn(s"Failed to compute wasting time, error: ${e.getMessage}", e)
        ServiceUnavailable(e.getMessage)
      case e =>
        logger.error(s"Failed to compute casting, error: ${e.getMessage}", e)
        InternalServerError(e.getMessage)
    }
  }

  private def validateRegionAndMail(region: String, request: Request[AnyContent]): Future[(Region, String)] = Future {
    val r = Region.parse(region).getOrElse(throw InvalidJsonException(s"invalid region $region"))
    val mail = request.body.asText.getOrElse(throw MissingParameterException("email"))
    if (!EmailAddress.isValid(mail)) throw InvalidJsonException(s"invalid email $mail")

    (r, mail)
  }
}
