package com.hastybox.delayedwebhooks.core

import scala.language.higherKinds

trait WebHooksExecutor[F[_]] {

  def execute(hooks: List[WebHookCall]): F[Either[Throwable, Boolean]]

}
