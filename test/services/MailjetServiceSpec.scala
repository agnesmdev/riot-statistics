package services

import java.time.LocalDateTime

import com.mailjet.client.errors.{MailjetException, MailjetSocketTimeoutException}
import com.mailjet.client.resource.Emailv31
import com.mailjet.client.resource.Emailv31.Message
import com.mailjet.client.{MailjetClient, MailjetRequest, MailjetResponse}
import exceptions.{ConnectionException, ServiceUnavailableException}
import models.{NormalType, TFTType, TimeData, WastedTime, _}
import org.json.JSONArray
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, BeforeAndAfter}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class MailjetServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  implicit val patience: PatienceConfig = PatienceConfig(30.seconds, 1.seconds)

  val email = "test@dmail.com"
  val name = "Test"

  val summonerEmail = "summoner@dmail.com"

  val now: LocalDateTime = LocalDateTime.now()
  val wastedTime: WastedTime = WastedTime("Summoner", 100, TimeData(1082302), now.minusYears(2), NormalType)
  val tftWastedTime: WastedTime = WastedTime("Summoner", 50, TimeData(82302), now.minusYears(1), TFTType)

  val client: MailjetClient = mock[MailjetClient]
  val configuration: Configuration = mock[Configuration]
  when(configuration.get[String]("mailjet.api.from.email")).thenReturn(email)
  when(configuration.get[String]("mailjet.api.from.name")).thenReturn(name)

  val mailjetService = new MailjetServiceImpl(client, configuration)

  before {
    reset(client)
  }

  "MailjetService sendWastedTime" should {
    "succeed" in {
      val response = mock[MailjetResponse]
      when(client.post(any[MailjetRequest])).thenReturn(response)
      when(response.getStatus).thenReturn(Status.OK)

      val result = mailjetService.sendWastedTime(wastedTime, tftWastedTime, summonerEmail)

      whenReady(result) { r =>
        r mustBe true

        val requestCaptor: ArgumentCaptor[MailjetRequest] = ArgumentCaptor.forClass(classOf[MailjetRequest])
        verify(client).post(requestCaptor.capture())
        validateWastedTimeRequest(requestCaptor.getValue)
      }
    }

    "fail with ConnectionException" in {
      when(client.post(any[MailjetRequest])).thenThrow(new MailjetException("boom"))

      val result = mailjetService.sendWastedTime(wastedTime, tftWastedTime, summonerEmail)

      whenReady(result.failed) { e =>
        e mustBe a[ConnectionException]
        e.getMessage mustEqual "Connection failed, error: boom"

        val requestCaptor: ArgumentCaptor[MailjetRequest] = ArgumentCaptor.forClass(classOf[MailjetRequest])
        verify(client).post(requestCaptor.capture())
        validateWastedTimeRequest(requestCaptor.getValue)
      }
    }

    "fail with ServiceUnavailableException" in {
      when(client.post(any[MailjetRequest])).thenThrow(new MailjetSocketTimeoutException("boom"))

      val result = mailjetService.sendWastedTime(wastedTime, tftWastedTime, summonerEmail)

      whenReady(result.failed) { e =>
        e mustBe a[ServiceUnavailableException]
        e.getMessage mustEqual "boom"

        val requestCaptor: ArgumentCaptor[MailjetRequest] = ArgumentCaptor.forClass(classOf[MailjetRequest])
        verify(client).post(requestCaptor.capture())
        validateWastedTimeRequest(requestCaptor.getValue)
      }
    }
  }

  private def validateWastedTimeRequest(request: MailjetRequest): Assertion = {
    val messages = request.getBodyJSON.get(Emailv31.MESSAGES).asInstanceOf[JSONArray]
    messages.length() mustEqual 1

    val message = messages.getJSONObject(0)
    message.get(Message.FROM).toString mustEqual EmailParty(email, name).toJsonObject.toString

    val to = message.get(Message.TO).asInstanceOf[JSONArray]
    to.length() mustEqual 1
    to.get(0).toString mustEqual EmailParty(summonerEmail, wastedTime.summonerName).toJsonObject.toString

    message.get(Message.SUBJECT) mustEqual "Wasted Time on LoL"
    message.get(Message.TEXTPART) mustEqual ""
    message.get(Message.HTMLPART) mustEqual views.html.app.wasted(wastedTime, tftWastedTime).toString()
  }
}
