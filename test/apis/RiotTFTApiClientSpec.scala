package apis

import apis.ApiConstants.RIOT_TOKEN_HEADER
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSRequest}

class RiotTFTApiClientSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val apiKey = "key"
  val apiHost = "host"
  val region = "host"

  val ws: WSClient = mock[WSClient]
  val configuration: Configuration = mock[Configuration]
  when(configuration.get[String]("riot.api.key")).thenReturn(apiKey)
  when(configuration.get[String](region)).thenReturn(apiHost)

  val request: WSRequest = mock[WSRequest]
  val riotTFTApiClient = new RiotTFTApiClientImpl(ws, configuration)

  before {
    reset(ws, request)
  }

  "RiotTFTApiClient getSummonerMatches" should {
    "succeed" in {
      val puuid = "puuid"
      when(ws.url(anyString())).thenReturn(request)
      when(request.withHttpHeaders(any[(String, String)])).thenReturn(request)

      riotTFTApiClient.getSummonerMatches(puuid)(region)

      verify(ws).url(s"$apiHost/tft/match/v1/matches/by-puuid/$puuid/ids?count=1000000000")
      verify(request).withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
    }
  }

  "RiotTFTApiClient getMatch" should {
    "succeed" in {
      val matchId = "13974597"
      when(ws.url(anyString())).thenReturn(request)
      when(request.withHttpHeaders(any[(String, String)])).thenReturn(request)

      riotTFTApiClient.getMatch(matchId)(region)

      verify(ws).url(s"$apiHost/tft/match/v1/matches/$matchId")
      verify(request).withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
    }
  }
}
