package apis

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSRequest}

class GameApiClientSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val apiHost = "host"

  val ws: WSClient = mock[WSClient]
  val configuration: Configuration = mock[Configuration]
  when(configuration.get[String]("game.api.host")).thenReturn(apiHost)

  val request: WSRequest = mock[WSRequest]
  val gameApiClient = new GameApiClientImpl(ws, configuration)

  before {
    reset(ws, request)
  }

  "GameApiClient getQueues" should {
    "succeed" in {
      when(ws.url(anyString())).thenReturn(request)

      gameApiClient.getQueues

      verify(ws).url(s"$apiHost/queues.json")
    }
  }

  "GameApiClient getSeasons" should {
    "succeed" in {
      when(ws.url(anyString())).thenReturn(request)

      gameApiClient.getSeasons

      verify(ws).url(s"$apiHost/seasons.json")
    }
  }
}
