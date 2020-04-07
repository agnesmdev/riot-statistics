package controllers

import exceptions.{NotFoundException, ServiceUnavailableException}
import models.{EuwRegion, Region}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, _}
import play.api.{Application, Configuration}
import services.RiotService

import scala.concurrent.Future
import scala.concurrent.duration._

class RiotControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val riotService: RiotService = mock[RiotService]
  val app: Application = new GuiceApplicationBuilder()
    .loadConfig(env => Configuration.load(env))
    .overrides(bind[RiotService].toInstance(riotService))
    .build()

  val name = "name"
  val region: String = EuwRegion.value
  val email = "test@dmail.com"

  before {
    reset(riotService)
  }

  "RiotController getWastedTime" should {
    "return Accepted" in {
      val request = FakeRequest(POST, s"/summoners/$name/regions/$region/wasted").withTextBody(email)
      val time = 5.minutes + 20.seconds
      when(riotService.getWastedTime(anyString, any[Region], anyString)).thenReturn(Future.successful(time))

      val result = route(app, request).value

      status(result) mustEqual ACCEPTED
      contentAsString(result) mustEqual s"Computing wasted time, estimated time: ${time.toMinutes} minutes, exactly ${time.toSeconds} seconds"
    }

    "return BadRequest if region is incorrect" in {
      val incorrectRegion = "incorrect_region"
      val request = FakeRequest(POST, s"/summoners/$name/regions/$incorrectRegion/wasted").withTextBody(email)

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustEqual s"Invalid json input, error: invalid region $incorrectRegion"
    }

    "return BadRequest if mail is not specified" in {
      val request = FakeRequest(POST, s"/summoners/$name/regions/$region/wasted")

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustEqual "Missing parameter: email"
    }

    "return BadRequest if mail is incorrect" in {
      val incorrectEmail = "not_an_email"
      val request = FakeRequest(POST, s"/summoners/$name/regions/$region/wasted").withTextBody(incorrectEmail)

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustEqual s"Invalid json input, error: invalid email $incorrectEmail"
    }

    "return NotFound" in {
      val request = FakeRequest(POST, s"/summoners/$name/regions/$region/wasted").withTextBody(email)
      when(riotService.getWastedTime(anyString(), any[Region], anyString())).thenReturn(Future.failed(NotFoundException(s"Summoner $name")))

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND
      contentAsString(result) mustEqual s"Summoner $name does not exist"
    }

    "return ServiceUnavailable" in {
      val request = FakeRequest(POST, s"/summoners/$name/regions/$region/wasted").withTextBody(email)
      when(riotService.getWastedTime(anyString(), any[Region], anyString())).thenReturn(Future.failed(ServiceUnavailableException("boom")))

      val result = route(app, request).value

      status(result) mustEqual SERVICE_UNAVAILABLE
      contentAsString(result) mustEqual "boom"
    }

    "return InternalServerError" in {
      val request = FakeRequest(POST, s"/summoners/$name/regions/$region/wasted").withTextBody(email)
      when(riotService.getWastedTime(anyString(), any[Region], anyString())).thenReturn(Future.failed(new Exception("boom")))

      val result = route(app, request).value

      status(result) mustEqual INTERNAL_SERVER_ERROR
      contentAsString(result) mustEqual "boom"
    }
  }
}
