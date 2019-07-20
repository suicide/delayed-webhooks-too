package com.hastybox.delayedwebhooks.core

import cats.effect.{Resource, Sync, Timer}
import cats.syntax.all._
import com.typesafe.scalalogging.Logger
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._
import org.http4s.{Uri, UrlForm}

import scala.concurrent.duration._
import scala.language.higherKinds

trait WebHookExecutor[F[_]] {

  def execute(hook: WebHookCall): F[Either[Throwable, Boolean]]

}

object HttpClientWebHookExecutor {
  private val log = Logger[HttpClientWebHookExecutor[Any]]
}

trait HttpClientWebHookExecutor[F[_]] extends WebHookExecutor[F] with Http4sClientDsl[F] {

  import HttpClientWebHookExecutor._

  implicit def F: Sync[F]

  def httpClient: Resource[F, Client[F]]

  def timer: Timer[F]

  override def execute(hook: WebHookCall): F[Either[Throwable, Boolean]] = {
    for {
      duration <- F.delay(hook.delay.map(_.seconds).getOrElse(0.seconds))
      uri <- toUri(hook)
      req <- POST(UrlForm(), uri)
      _ <- timer.sleep(duration)
      result <- httpClient.use(_.fetch[Either[Throwable, Boolean]](req) {
        case res if res.status.isSuccess =>
          F.delay(log.debug(s"Post to $uri successful")) *>
            F.pure(Right(true))
        case res if res.status.code >= 400 && res.status.code < 500 =>
          F.delay(log.warn(s"Post to $uri failed due to client error $res")) *>
            F.raiseError(ClientSideException(s"Received client error code ${res.status.code}: response $res"))
        case res if res.status.code >= 500 =>
          F.delay(log.warn(s"Post to $uri encountered server error: $res")) *>
            F.pure(Right(false))
        case res =>
          F.delay(log.warn(s"Post to $uri failed due to error $res")) *>
            F.raiseError(WebHookExecutionException(s"Execution failed with response $res"))
      })
    } yield result
  }

  def toUri(webHook: WebHookCall): F[Uri] = {
    F.delay(
      Uri.fromString(s"https://maker.ifttt.com/trigger/${webHook.event}/with/key/${webHook.key}"))
      .flatMap({
        case Left(e) => F.raiseError(e)
        case Right(u) => F.pure(u)
      })
  }

}

class DefaultHttpClientWebHookExecutor[F[_]](
                                              val httpClient: Resource[F, Client[F]]
                                            )(
                                              implicit val F: Sync[F],
                                              implicit val timer: Timer[F]
                                            ) extends HttpClientWebHookExecutor[F]

case class WebHookExecutionException(msg: String) extends RuntimeException(msg)
case class ClientSideException(msg: String) extends RuntimeException(msg)
