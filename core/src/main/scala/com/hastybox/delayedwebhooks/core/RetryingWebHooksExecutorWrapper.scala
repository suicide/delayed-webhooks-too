package com.hastybox.delayedwebhooks.core

import cats.effect.{Sync, Timer}
import cats.syntax.all._
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

/**
 * retries all hook calls again
 *
 * @tparam F
 */
trait RetryingWebHooksExecutorWrapper[F[_]] extends WebHooksExecutor[F] {

  import RetryingWebHooksExecutorWrapper._

  def delay: FiniteDuration

  def maxRetries: Int

  def delegate: WebHooksExecutor[F]

  implicit def F: Sync[F]

  def timer: Timer[F]

  override def execute(hooks: List[WebHookCall]): F[Either[Throwable, Boolean]] = {

    def withRetries(retry: Int): F[Either[Throwable, Boolean]] = {
      if (retry >= maxRetries) {
        return F.pure(Left(RetriesDepletedException(s"Unable to complete all webhook calls $hooks repeatedly")))
      }

      for {
        exec <- delegate.execute(hooks)
        result <- exec match {
          case Right(false) =>
            F.delay(log.warn(s"Webhook calls $hooks failed, retrying in retry ${retry + 1}")) *>
              timer.sleep(delay) *>
              withRetries(retry + 1)
          case _ => F.pure(exec)
        }
      } yield result
    }

    withRetries(0);
  }

}

object RetryingWebHooksExecutorWrapper {
  private val log = Logger[RetryingWebHooksExecutorWrapper[Any]]
}

case class RetriesDepletedException(msg: String) extends RuntimeException(msg)

class DefaultRetryingWebHooksExecutorWrapper[F[_]](
                                                    val delay: FiniteDuration,
                                                    val maxRetries: Int,
                                                    val delegate: WebHooksExecutor[F]
                                                  )(
                                                    implicit val F: Sync[F],
                                                    implicit val timer: Timer[F]
                                                  ) extends RetryingWebHooksExecutorWrapper[F]