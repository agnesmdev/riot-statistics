package apis

import exceptions.ApiException
import models.{Queue, Season}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source

class GameApiSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  implicit val patience: PatienceConfig = PatienceConfig(30.seconds, 1.seconds)

  val gameApiClient: GameApiClient = mock[GameApiClient]
  val request: WSRequest = mock[WSRequest]
  val response: WSResponse = mock[WSResponse]
  val gameApi = new GameApiImpl(gameApiClient)

  val queuesJson: JsValue = Json.parse(Source.fromResource("queues.json").mkString)
  val queues: Seq[Queue] = queuesJson.as[Seq[Queue]]

  val seasonsJson: JsValue = Json.parse(Source.fromResource("seasons.json").mkString)
  val seasons: Seq[Season] = seasonsJson.as[Seq[Season]]

  before {
    reset(gameApiClient, request, response)
  }

  "GameApi getQueues" should {
    "succeed" in {
      when(gameApiClient.getQueues).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.OK)
      when(response.json).thenReturn(queuesJson)

      val result = gameApi.getQueues

      whenReady(result) { c =>
        c mustEqual queues

        verify(gameApiClient).getQueues
        verify(request).get()
      }
    }

    "fail" in {
      when(gameApiClient.getQueues).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = gameApi.getQueues

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(gameApiClient).getQueues
        verify(request).get()
      }
    }
  }

  "GameApi getSeasons" should {
    "succeed" in {
      when(gameApiClient.getSeasons).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.OK)
      when(response.json).thenReturn(seasonsJson)

      val result = gameApi.getSeasons

      whenReady(result) { c =>
        c mustEqual seasons

        verify(gameApiClient).getSeasons
        verify(request).get()
      }
    }

    "fail" in {
      when(gameApiClient.getSeasons).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = gameApi.getSeasons

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(gameApiClient).getSeasons
        verify(request).get()
      }
    }
  }
}
