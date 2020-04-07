package apis

import exceptions.ApiException
import models.Champion
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

class DragonApiSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  implicit val patience: PatienceConfig = PatienceConfig(30.seconds, 1.seconds)

  val dragonApiClient: DragonApiClient = mock[DragonApiClient]
  val request: WSRequest = mock[WSRequest]
  val response: WSResponse = mock[WSResponse]
  val dragonApi = new DragonApiImpl(dragonApiClient)

  val championsJson: JsValue = Json.parse(Source.fromResource("champions.json").mkString)
  val champions: Seq[Champion] = Champion.parse(championsJson)

  before {
    reset(dragonApiClient, request, response)
  }

  "DragonApi getChampions" should {
    "succeed" in {
      when(dragonApiClient.getChampions).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.OK)
      when(response.json).thenReturn(championsJson)

      val result = dragonApi.getChampions

      whenReady(result) { c =>
        c mustEqual champions

        verify(dragonApiClient).getChampions
        verify(request).get()
      }
    }

    "fail" in {
      when(dragonApiClient.getChampions).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = dragonApi.getChampions

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(dragonApiClient).getChampions
        verify(request).get()
      }
    }
  }
}
