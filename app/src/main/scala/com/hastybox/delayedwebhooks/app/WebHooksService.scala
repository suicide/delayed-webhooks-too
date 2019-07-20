package com.hastybox.delayedwebhooks.app

import cats.effect.Sync
import cats.syntax.all._
import com.hastybox.delayedwebhooks.core.{WebHookCall, WebHooksExecutor}
import com.typesafe.scalalogging.Logger
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.{HttpRoutes, _}
import io.circe.generic.auto._

import scala.language.higherKinds

object WebHooksService {
  private val log = Logger[WebHooksService[Any]]
}

trait WebHooksService[F[_]] extends Http4sDsl[F] {

  import WebHooksService._

  implicit def F: Sync[F]

  def webHooksExecutor: WebHooksExecutor[F]

  implicit lazy val webHookCallDecoder = jsonOf[F, WebHookCall]
  implicit lazy val webHookCallListDecoder = jsonOf[F, List[WebHookCall]]

  def app: HttpApp[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "webhooks" =>
      for {
        hooks <- req.as[List[WebHookCall]]
        exec <- webHooksExecutor.execute(hooks)
        result <- exec match {
          case Right(true) => Ok("created")
          case _ => for {
            _ <- F.delay(log.error(s"Executing $req failed"))
            e <- InternalServerError()
          } yield e
        }
      } yield result
    case GET -> Root => Ok("ok")
  }.orNotFound

}

class SimpleWebHooksService[F[_]](
                                   val webHooksExecutor: WebHooksExecutor[F]
                                 )(
                                   implicit val F: Sync[F]
                                 ) extends WebHooksService[F]
