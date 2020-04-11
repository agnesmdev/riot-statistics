package services

import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mailjet.client.errors.{MailjetException, MailjetSocketTimeoutException}
import com.mailjet.client.resource.Emailv31
import com.mailjet.client.{MailjetClient, MailjetRequest}
import exceptions.{ApiException, ConnectionException, ServiceUnavailableException}
import helpers.LoggingHelper
import models.{Email, EmailParty, WastedTime}
import org.json.JSONArray
import play.api.Configuration
import play.api.http.Status

import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[MailjetServiceImpl])
trait MailjetService {

  def sendWastedTime(wastedTime: WastedTime, tftWastedTime: WastedTime, emailAddress: String): Future[Boolean]
}

@Singleton
class MailjetServiceImpl @Inject()(client: MailjetClient, configuration: Configuration)(implicit ec: ExecutionContext) extends MailjetService with LoggingHelper {

  private val fromEmail: String = configuration.get[String]("mailjet.api.from.email")
  private val fromName: String = configuration.get[String]("mailjet.api.from.name")
  private val fromParty = EmailParty(fromEmail, fromName)

  private val apiHost: String = configuration.get[String]("api.host")

  private val wastedTimeSubject = "Wasted Time on LoL"

  override def sendWastedTime(wastedTime: WastedTime, tftWastedTime: WastedTime, emailAddress: String): Future[Boolean] = Future {
    logger.debug(s"Sending email with wasted time info for summoner ${wastedTime.summonerName} to $emailAddress")

    val content = views.html.app.wasted(wastedTime, tftWastedTime)(apiHost).toString()

    val email = Email(fromParty, EmailParty(emailAddress, wastedTime.summonerName), wastedTimeSubject, "", content)
    val request = new MailjetRequest(Emailv31.resource).property(Emailv31.MESSAGES, new JSONArray().put(email.toJSONObject))

    client.post(request) match {
      case response if response.getStatus == Status.OK =>
        logger.debug(s"Successfully sent email with wasted time info for summoner ${wastedTime.summonerName} to $emailAddress")
        true
      case response => throw ApiException(response.getStatus, response.getData.toString())
    }
  }.recover {
    case e: MailjetException => throw ConnectionException(e)
    case e: MailjetSocketTimeoutException => throw ServiceUnavailableException(e.getMessage)
  }
}
