package apis

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSRequest}

class DragonApiClientSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val apiHost = "host"

  val ws: WSClient = mock[WSClient]
  val configuration: Configuration = mock[Configuration]
  when(configuration.get[String]("dragon.api.host")).thenReturn(apiHost)

  val request: WSRequest = mock[WSRequest]
  val dragonApiClient = new DragonApiClientImpl(ws, configuration)

  before {
    reset(ws, request)
  }

  "DragonApiClient getChampions" should {
    "succeed" in {
      when(ws.url(anyString)).thenReturn(request)

      dragonApiClient.getChampions

      verify(ws).url(s"$apiHost/champions.json")
    }
  }
}
