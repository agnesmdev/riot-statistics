package helpers

import scala.concurrent.{ExecutionContext, Future}

trait FutureHelper extends LoggingHelper {

  def oneByOne[A, B](seq: Seq[A])(fun: A => Future[B], res: Seq[B] = Nil)(implicit ec: ExecutionContext): Future[Seq[B]] = {
    seq.headOption match {
      case None => Future.successful(res)
      case Some(a) => fun(a).flatMap(b => oneByOne(seq.tail)(fun, res ++ Seq(b)))
    }
  }
}
