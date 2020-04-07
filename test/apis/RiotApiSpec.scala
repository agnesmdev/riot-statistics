package apis

import akka.actor.ActorSystem
import exceptions.{ApiException, NotFoundException, TooManyRequestsException}
import models._
import org.mockito.ArgumentMatchers.{anyInt, anyLong, anyString}
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

class RiotApiSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  implicit val patience: PatienceConfig = PatienceConfig(30.seconds, 1.seconds)

  val riotApiClient: RiotApiClient = mock[RiotApiClient]
  val system: ActorSystem = ActorSystem()
  val configuration: Configuration = mock[Configuration]
  when(configuration.get[Int]("riot.api.max.retries")).thenReturn(1)
  when(configuration.get[FiniteDuration]("riot.api.unavailable.delay")).thenReturn(1.seconds)
  when(configuration.get[FiniteDuration]("riot.api.too.much.delay")).thenReturn(1.seconds)

  val request: WSRequest = mock[WSRequest]
  val response: WSResponse = mock[WSResponse]
  val riotApi = new RiotApiImpl(riotApiClient, system, configuration)

  val name = "Summoner"
  val summonerJson: JsValue = Json.parse(Source.fromResource("summoner.json").mkString)
  val summoner: Summoner = summonerJson.as[Summoner]

  val matchId = 1239014886
  val matchesJson: JsValue = Json.parse(Source.fromResource("matches.json").mkString)
  val matches: Seq[MatchInfo] = matchesJson.as[MatchList].matches

  val matches2Json: JsValue = Json.parse(Source.fromResource("matches_2.json").mkString)
  val matches2: Seq[MatchInfo] = matches2Json.as[MatchList].matches

  val matchJson: JsValue = Json.parse(Source.fromResource("match.json").mkString)
  val matchObject: Match = matchJson.as[Match]

  before {
    reset(riotApiClient, request, response)
  }

  "RiotApi getSummoner" should {
    "succeed" in {
      when(riotApiClient.getSummoner(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE, Status.OK)
      when(response.json).thenReturn(summonerJson)

      val result = riotApi.getSummoner(name)(EuwRegion)

      whenReady(result) { s =>
        s mustEqual summoner

        verify(riotApiClient, times(2)).getSummoner(name)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }

    "fail if summoner is not found" in {
      when(riotApiClient.getSummoner(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.NOT_FOUND)

      val result = riotApi.getSummoner(name)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual NotFoundException(s"Summoner $name")

        verify(riotApiClient).getSummoner(name)(EuwRegion.host)
        verify(request).get()
      }
    }

    "fail if too many requests are made" in {
      when(riotApiClient.getSummoner(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.TOO_MANY_REQUESTS)
      when(response.body).thenReturn("")

      val result = riotApi.getSummoner(name)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual TooManyRequestsException("")

        verify(riotApiClient, times(2)).getSummoner(name)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }

    "fail if api fails" in {
      when(riotApiClient.getSummoner(anyString)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = riotApi.getSummoner(name)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(riotApiClient, times(2)).getSummoner(name)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }
  }

  "RiotApi getSummonerMatches" should {
    "succeed when retry for unavailability" in {
      when(riotApiClient.getSummonerMatches(anyString, anyInt)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE, Status.OK)
      when(response.json).thenReturn(matchesJson, matches2Json)

      val result = riotApi.getSummonerMatches(name)(EuwRegion)

      whenReady(result) { m =>
        m mustEqual matches ++ matches2

        verify(riotApiClient, times(2)).getSummonerMatches(name, 0)(EuwRegion.host)
        verify(riotApiClient).getSummonerMatches(name, 100)(EuwRegion.host)
        verify(request, times(3)).get()
      }
    }

    "succeed when retry for too many requests" in {
      when(riotApiClient.getSummonerMatches(anyString, anyInt)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.TOO_MANY_REQUESTS, Status.OK)
      when(response.json).thenReturn(matchesJson, matches2Json)

      val result = riotApi.getSummonerMatches(name)(EuwRegion)

      whenReady(result) { m =>
        m mustEqual matches ++ matches2

        verify(riotApiClient, times(2)).getSummonerMatches(name, 0)(EuwRegion.host)
        verify(riotApiClient).getSummonerMatches(name, 100)(EuwRegion.host)
        verify(request, times(3)).get()
      }
    }

    "succeed if no matches if found" in {
      when(riotApiClient.getSummonerMatches(anyString, anyInt)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.NOT_FOUND)

      val result = riotApi.getSummonerMatches(name)(EuwRegion)

      whenReady(result) { m =>
        m mustBe empty

        verify(riotApiClient).getSummonerMatches(name, 0)(EuwRegion.host)
        verify(request).get()
      }
    }

    "fail if too many requests are made" in {
      when(riotApiClient.getSummonerMatches(anyString, anyInt)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.TOO_MANY_REQUESTS)
      when(response.body).thenReturn("")

      val result = riotApi.getSummonerMatches(name)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual TooManyRequestsException("")

        verify(riotApiClient, times(2)).getSummonerMatches(name, 0)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }

    "fail if api fails" in {
      when(riotApiClient.getSummonerMatches(anyString, anyInt)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = riotApi.getSummonerMatches(name)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(riotApiClient, times(2)).getSummonerMatches(name, 0)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }
  }

  "RiotApi getMatch" should {
    "succeed" in {
      when(riotApiClient.getMatch(anyLong)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE, Status.OK)
      when(response.json).thenReturn(matchJson)

      val result = riotApi.getMatch(matchId)(EuwRegion)

      whenReady(result) { s =>
        s mustEqual matchObject

        verify(riotApiClient, times(2)).getMatch(matchId)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }

    "fail if match is not found" in {
      when(riotApiClient.getMatch(anyLong)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.NOT_FOUND)

      val result = riotApi.getMatch(matchId)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual NotFoundException(s"Match $matchId")

        verify(riotApiClient).getMatch(matchId)(EuwRegion.host)
        verify(request).get()
      }
    }

    "fail if too many requests are made" in {
      when(riotApiClient.getMatch(anyLong)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.TOO_MANY_REQUESTS)
      when(response.body).thenReturn("")

      val result = riotApi.getMatch(matchId)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual TooManyRequestsException("")

        verify(riotApiClient, times(2)).getMatch(matchId)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }

    "fail if api fails" in {
      when(riotApiClient.getMatch(anyLong)(anyString)).thenReturn(request)
      when(request.get()).thenReturn(Future.successful(response))
      when(response.status).thenReturn(Status.SERVICE_UNAVAILABLE)
      when(response.body).thenReturn("")

      val result = riotApi.getMatch(matchId)(EuwRegion)

      whenReady(result.failed) { e =>
        e mustEqual ApiException(Status.SERVICE_UNAVAILABLE, "")

        verify(riotApiClient, times(2)).getMatch(matchId)(EuwRegion.host)
        verify(request, times(2)).get()
      }
    }
  }
}
