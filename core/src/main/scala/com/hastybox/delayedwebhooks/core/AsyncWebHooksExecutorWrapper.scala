package com.hastybox.delayedwebhooks.core

import cats.effect.Effect
import com.typesafe.scalalogging.Logger

import scala.language.higherKinds

object AsyncWebHooksExecutorWrapper {
  private val log = Logger[AsyncWebHooksExecutorWrapper[Any]]
}

trait AsyncWebHooksExecutorWrapper[F[_]] extends WebHooksExecutor[F] {

  import AsyncWebHooksExecutorWrapper._

  implicit def F: Effect[F]

  def webHooksExecutor: WebHooksExecutor[F]

  override def execute(hooks: List[WebHookCall]): F[Either[Throwable, Boolean]] = {

    F.async(cb => {
      F.toIO(webHooksExecutor.execute(hooks)).unsafeRunAsync {
        case Right(Right(true)) => log.info(s"Executed hooks successfully: $hooks")
        case result => log.warn(s"Hook execution failed: $result, hooks: $hooks")
      }

      // just say everything is fine
      cb(Right(Right(true)))
    })

  }
}

class DefaultAsyncWebHooksExecutorWrapper[F[_]](
                                                 val webHooksExecutor: WebHooksExecutor[F]
                                               )(
                                                 implicit val F: Effect[F]
                                               ) extends AsyncWebHooksExecutorWrapper[F]
