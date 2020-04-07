package helpers

import play.api.libs.json.{JsResult, JsSuccess}

trait JsonHelper {

  implicit class JsResultUtil[A](seq: Seq[JsResult[A]]) {
    def reduceJsResult: JsResult[Seq[A]] = seq.foldLeft[JsResult[Seq[A]]](JsSuccess(Nil)) {
      case (result, a) => result.flatMap(r => a.map(r ++ Seq(_)))
    }
  }

}
