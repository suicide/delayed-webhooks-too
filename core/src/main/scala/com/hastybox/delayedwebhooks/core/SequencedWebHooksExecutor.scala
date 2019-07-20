package com.hastybox.delayedwebhooks.core

import cats.effect.Sync
import cats.implicits._
import com.typesafe.scalalogging.Logger

import scala.language.higherKinds

object SequencedWebHooksExecutor {
  private val log = Logger[SequencedWebHooksExecutor[Any]]
}

trait SequencedWebHooksExecutor[F[_]] extends WebHooksExecutor[F] {

  implicit def F: Sync[F]

  def webHookExecutor: WebHookExecutor[F]


  override def execute(hooks: List[WebHookCall]): F[Either[Throwable, Boolean]] = {

    val execs = for {
      hook <- hooks
    } yield webHookExecutor.execute(hook)

    for {
      e <- execs.sequence
    } yield e.foldLeft(Either.right[Throwable, Boolean](true))((a, b) => (a, b) match {
      case (Left(e), _) => Left(e)
      case (_, Left(e)) => Left(e)
      case (Right(x), Right(y)) => if (x && y) Right(true) else Right(false)
    })
  }
}

class DefaultSequencedWebHooksExecutor[F[_]](
                                              val webHookExecutor: WebHookExecutor[F]
                                            )(
                                              implicit val F: Sync[F]
                                            ) extends SequencedWebHooksExecutor[F]
