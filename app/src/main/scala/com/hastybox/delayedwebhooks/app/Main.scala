package com.hastybox.delayedwebhooks.app

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.hastybox.delayedwebhooks.core.{DefaultAsyncWebHooksExecutorWrapper, DefaultHttpClientWebHookExecutor, DefaultRetryingWebHooksExecutorWrapper, DefaultSequencedWebHooksExecutor}
import com.typesafe.scalalogging.Logger
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends IOApp with MainApp {
  private val log = Logger[MainApp]

  override def run(args: List[String]): IO[ExitCode] = {

    val blazeClientEc = ExecutionContext.fromExecutorService(
      Executors.newFixedThreadPool(10,
        new ThreadFactoryBuilder().setNameFormat("blaze-client-pool-%d").build())
    )

    for {
      httpClient <- IO(BlazeClientBuilder[IO](blazeClientEc).resource)
      webHookExecutor <- IO(new DefaultHttpClientWebHookExecutor[IO](httpClient))
      seqWebHooksExecutor <- IO(new DefaultSequencedWebHooksExecutor[IO](webHookExecutor))
      retryWebHooksExecutor <- IO(new DefaultRetryingWebHooksExecutorWrapper[IO](10.seconds, 5, seqWebHooksExecutor))
      asyncWebHooksExecutor <- IO(new DefaultAsyncWebHooksExecutorWrapper[IO](retryWebHooksExecutor))
      service <- IO(new SimpleWebHooksService[IO](asyncWebHooksExecutor))
      _ <- IO(log.info("Starting server"))
      server <- BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(service.app)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
      _ <- IO(log.info("Shutting down server"))
    } yield server
  }
}

trait MainApp
