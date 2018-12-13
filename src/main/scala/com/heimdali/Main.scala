package com.heimdali

import java.util.concurrent.{Executor, Executors}

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import com.heimdali.modules._

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  val heimdaliApp = new IOAppModule[IO]
    with ExecutionContextModule[IO]
    with ConfigurationModule
    with ContextModule[IO]
    with FileSystemModule[IO]
    with StartupModule[IO]
    with HttpModule[IO]
    with ClusterModule[IO]
    with ClientModule[IO]
    with RepoModule
    with ServiceModule[IO]
    with RestModule

  override def run(args: List[String]): IO[ExitCode] = {
    import heimdaliApp._

    val startupContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    val startupShift = IO.contextShift(startupContext)

    for {
      _ <- heimdaliApp.startup.start().start(startupShift)
      result <- heimdaliApp.restAPI.build()
    } yield result
  }
}
