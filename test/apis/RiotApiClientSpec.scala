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

class RiotApiClientSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val apiKey = "key"
  val apiHost = "host"
  val region = "host"

  val ws: WSClient = mock[WSClient]
  val configuration: Configuration = mock[Configuration]
  when(configuration.get[String]("riot.api.key")).thenReturn(apiKey)
  when(configuration.get[String](region)).thenReturn(apiHost)

  val request: WSRequest = mock[WSRequest]
  val riotApiClient = new RiotApiClientImpl(ws, configuration)

  before {
    reset(ws, request)
  }

  "RiotApiClient getSummoner" should {
    "succeed" in {
      val name = "name"
      when(ws.url(anyString())).thenReturn(request)
      when(request.withHttpHeaders(any[(String, String)])).thenReturn(request)

      riotApiClient.getSummoner(name)(region)

      verify(ws).url(s"$apiHost/lol/summoner/v4/summoners/by-name/$name")
      verify(request).withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
    }
  }

  "RiotApiClient getSummonerMatches" should {
    "succeed" in {
      val accountId = "id"
      when(ws.url(anyString())).thenReturn(request)
      when(request.withHttpHeaders(any[(String, String)])).thenReturn(request)

      riotApiClient.getSummonerMatches(accountId, 0)(region)

      verify(ws).url(s"$apiHost/lol/match/v4/matchlists/by-account/$accountId?beginIndex=0")
      verify(request).withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
    }
  }

  "RiotApiClient getMatch" should {
    "succeed" in {
      val matchId = 13974597
      when(ws.url(anyString())).thenReturn(request)
      when(request.withHttpHeaders(any[(String, String)])).thenReturn(request)

      riotApiClient.getMatch(matchId)(region)

      verify(ws).url(s"$apiHost/lol/match/v4/matches/$matchId")
      verify(request).withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
    }
  }
}
