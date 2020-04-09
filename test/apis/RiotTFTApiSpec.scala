package apis

import akka.actor.ActorSystem
import exceptions.{ApiException, NotFoundException, TooManyRequestsException}
import models._
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.io.Source

class RiotTFTApiSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  implicit val patience: PatienceConfig = PatienceConfig(30.seconds, 1.seconds)

  val riotTFTApiClient: RiotTFTApiClient = mock[RiotTFTApiClient]
  val system: ActorSystem = ActorSystem()
  val configuration: Configuration = mock[Configuration]
  when(configuration.get[Int]("riot.api.max.retries")).thenReturn(1)
  when(configuration.get[FiniteDuration]("riot.api.unavailable.delay")).thenReturn(1.seconds)
  when(configuration.get[FiniteDuration]("riot.api.too.much.delay")).thenReturn(1.seconds)

  val request: WSRequest = mock[WSRequest]
  val response: WSResponse = mock[WSResponse]
  val riotTFTApi = new RiotTFTApiImpl(riotTFTApiClient, system, configuration)

  val puuid = "puuid"
  val summonerJson: JsValue = Json.parse(Source.fromResource("summoner.json").mkString)
  val summoner: Summoner = summonerJson.as[Summoner]

  val matchId = "EUW1_451719379"
  val matchJson: JsValue = Json.parse(Source.fromResource("tft_match.json").mkString)
  val matchObject: TFTMatch = matchJson.as[TFTMatch]

  before {
    reset(riotTFTApiClient, request, response)
  }

  "RiotTFTApi getSummonerMatches" should {
    "succeed when retry for unavailability" in {
      when(riotTFTApiClient.getSummonerMatches(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE, Status.OK)
      when(response.json).thenReturn(Json.toJson(Seq(matchId)))

      val result = riotTFTApi.getSummonerMatches(puuid)(EuwRegion)

      whenReady(result) { m =>
        m mustEqual Seq(matchId)

        verify(riotTFTApiClient, times(2)).getSummonerMatches(puuid)(EuwRegion.globalHost)
        verify(request, times(2)).get()
      }
    }

    "succeed when retry for too many requests" in {
      when(riotTFTApiClient.getSummonerMatches(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.TOO_MANY_REQUESTS, Status.OK)
      when(response.json).thenReturn(Json.toJson(Seq(matchId)))

      val result = riotTFTApi.getSummonerMatches(puuid)(EuwRegion)

      whenReady(result) { m =>
        m mustEqual Seq(matchId)

        verify(riotTFTApiClient, times(2)).getSummonerMatches(puuid)(EuwRegion.globalHost)
        verify(request, times(2)).get()
      }
    }

    "fail if too many requests are made" in {
      when(riotTFTApiClient.getSummonerMatches(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.TOO_MANY_REQUESTS)
      when(response.body).thenReturn("")

      val result = riotTFTApi.getSummonerMatches(puuid)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual TooManyRequestsException("")

        verify(riotTFTApiClient, times(2)).getSummonerMatches(puuid)(EuwRegion.globalHost)
        verify(request, times(2)).get()
      }
    }

    "fail if api fails" in {
      when(riotTFTApiClient.getSummonerMatches(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = riotTFTApi.getSummonerMatches(puuid)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(riotTFTApiClient, times(2)).getSummonerMatches(puuid)(EuwRegion.globalHost)
        verify(request, times(2)).get()
      }
    }
  }

  "RiotTFTApi getMatch" should {
    "succeed" in {
      when(riotTFTApiClient.getMatch(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE, Status.OK, Status.OK)
      when(response.json).thenReturn(matchJson)

      val result = riotTFTApi.getMatch(matchId)(EuwRegion)

      whenReady(result) { s =>
        s mustEqual matchObject

        verify(riotTFTApiClient, times(2)).getMatch(matchId)(EuwRegion.globalHost)
        verify(request, times(2)).get()
      }
    }

    "fail if match is not found" in {
      when(riotTFTApiClient.getMatch(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.NOT_FOUND)

      val result = riotTFTApi.getMatch(matchId)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual NotFoundException(s"Match $matchId")

        verify(riotTFTApiClient).getMatch(matchId)(EuwRegion.globalHost)
        verify(request).get()
      }
    }

    "fail if too many requests are made" in {
      when(riotTFTApiClient.getMatch(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.TOO_MANY_REQUESTS)
      when(response.body).thenReturn("")

      val result = riotTFTApi.getMatch(matchId)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual TooManyRequestsException("")

        verify(riotTFTApiClient, times(2)).getMatch(matchId)(EuwRegion.globalHost)
        verify(request, times(2)).get()
      }
    }

    "fail if api fails" in {
      when(riotTFTApiClient.getMatch(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = riotTFTApi.getMatch(matchId)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(riotTFTApiClient, times(2)).getMatch(matchId)(EuwRegion.globalHost)
        verify(request, times(2)).get()
      }
    }
  }
}
