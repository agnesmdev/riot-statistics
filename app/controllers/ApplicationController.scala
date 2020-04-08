package controllers

import buildinfo.BuildInfo
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.ExecutionContext

class ApplicationController @Inject()(cc: ControllerComponents, ws: WSClient)
                                     (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def swagger: Action[AnyContent] = Action { _ =>
    Redirect("/docs/swagger-ui/index.html?url=/assets/swagger.json")
  }

  def status: Action[AnyContent] = Action { implicit request =>
    render {
      case Accepts.Html() => Ok(views.html.app.status(BuildInfo.name,
        BuildInfo.normalizedName,
        BuildInfo.version,
        BuildInfo.scalaVersion,
        BuildInfo.sbtVersion,
        BuildInfo.gitHeadCommit)
      )
      case Accepts.Json() => Ok(Json.obj(
        "name" -> BuildInfo.name,
        "normalizedName" -> BuildInfo.normalizedName,
        "version" -> BuildInfo.version,
        "scalaVersion" -> BuildInfo.scalaVersion,
        "sbtVersion" -> BuildInfo.sbtVersion,
        "gitHeadCommit" -> BuildInfo.gitHeadCommit
      ))
    }
  }
}