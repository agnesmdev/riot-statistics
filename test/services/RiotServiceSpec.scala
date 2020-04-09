package services

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

import apis.{RiotApi, RiotTFTApi}
import exceptions.NotFoundException
import models.{NormalType, TFTType, TimeData, WastedTime, _}
import org.mockito.ArgumentMatchers.{any, anyLong, anyString}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class RiotServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  implicit val patience: PatienceConfig = PatienceConfig(30.seconds, 1.seconds)

  val name = "name"
  val email = "test@dmail.com"

  val riotApi: RiotApi = mock[RiotApi]
  val riotTFTApi: RiotTFTApi = mock[RiotTFTApi]
  val mailjetService: MailjetService = mock[MailjetService]
  val riotService = new RiotServiceImpl(riotApi, riotTFTApi, mailjetService)

  val summoner: Summoner = Summoner("id", name, "puuid")
  val matchInfo: MatchInfo = MatchInfo(19739749, 12, None, None, 182972979)
  val normalMatch: Match = Match(matchInfo.gameId, 12, 20365, matchInfo.timestamp, 13)
  val tftMatch: TFTMatch = TFTMatch(TFTMatchInfo(1829722345, 123047))
  val matchId = "1U3JDE0U3"

  before {
    reset(riotApi, riotTFTApi, mailjetService)
  }

  "RiotService getWastedTime" should {
    "succeed" in {
      when(riotApi.getSummoner(anyString)(any[Region])).thenReturn(Future.successful(summoner))
      when(riotApi.getSummonerMatches(anyString)(any[Region])).thenReturn(Future.successful(Seq(matchInfo)))
      when(riotTFTApi.getSummonerMatches(anyString)(any[Region])).thenReturn(Future.successful(Seq(matchId)))
      when(riotApi.getMatch(anyLong)(any[Region])).thenReturn(Future.successful(normalMatch))
      when(riotTFTApi.getMatch(anyString)(any[Region])).thenReturn(Future.successful(tftMatch))
      when(mailjetService.sendWastedTime(any[WastedTime], any[WastedTime], anyString)).thenReturn(Future.successful(true))

      val result = riotService.getWastedTime(name, EuwRegion, email)

      whenReady(result) { d =>
        d mustEqual 2.seconds

        val inOrder = Mockito.inOrder(riotApi, riotTFTApi, mailjetService)
        inOrder.verify(riotApi).getSummoner(name)(EuwRegion)
        inOrder.verify(riotApi).getSummonerMatches(summoner.accountId)(EuwRegion)
        inOrder.verify(riotTFTApi).getSummonerMatches(summoner.puuid)(EuwRegion)
        inOrder.verify(riotApi).getMatch(matchInfo.gameId)(EuwRegion)
        inOrder.verify(riotTFTApi).getMatch(matchId)(EuwRegion)
        inOrder.verify(mailjetService).sendWastedTime(extractWastedTime(name, Seq(normalMatch)), extractTFTWastedTime(name, Seq(tftMatch.info)), email)
      }
    }

    "succeed even if computation fails" in {
      when(riotApi.getSummoner(anyString)(any[Region])).thenReturn(Future.successful(summoner))
      when(riotApi.getSummonerMatches(anyString)(any[Region])).thenReturn(Future.successful(Seq(matchInfo)))
      when(riotTFTApi.getSummonerMatches(anyString)(any[Region])).thenReturn(Future.successful(Seq(matchId)))
      when(riotApi.getMatch(anyLong)(any[Region])).thenReturn(Future.failed(NotFoundException(s"Match ${matchInfo.gameId}")))

      val result = riotService.getWastedTime(name, EuwRegion, email)

      whenReady(result) { d =>
        d mustEqual 2.seconds

        val inOrder = Mockito.inOrder(riotApi, riotTFTApi, mailjetService)
        inOrder.verify(riotApi).getSummoner(name)(EuwRegion)
        inOrder.verify(riotApi).getSummonerMatches(summoner.accountId)(EuwRegion)
        inOrder.verify(riotTFTApi).getSummonerMatches(summoner.puuid)(EuwRegion)
        inOrder.verify(riotApi).getMatch(matchInfo.gameId)(EuwRegion)
        verifyNoMoreInteractions(riotTFTApi)
        verifyNoMoreInteractions(mailjetService)
      }
    }
  }

  private def extractWastedTime(summonerName: String, matches: Seq[Match]): WastedTime = matches match {
    case Nil => WastedTime.empty(summonerName, NormalType)
    case _ =>
      val gameTotal = matches.map(_.gameDuration).sum
      val firstGame = matches.minBy(_.gameCreation).gameCreation.milliseconds
      val begin = LocalDateTime.ofEpochSecond(firstGame.toSeconds, 0, UTC)

      WastedTime(summonerName, matches.length, TimeData(gameTotal), begin, NormalType)
  }

  private def extractTFTWastedTime(summonerName: String, matches: Seq[TFTMatchInfo]): WastedTime = matches match {
    case Nil => WastedTime.empty(summonerName, TFTType)
    case _ =>
      val gameTotal = matches.map(_.game_length).sum.toLong
      val firstGame = matches.minBy(_.game_datetime).game_datetime.milliseconds
      val begin = LocalDateTime.ofEpochSecond(firstGame.toSeconds, 0, UTC)

      WastedTime(summonerName, matches.length, TimeData(gameTotal), begin, TFTType)
  }
}
