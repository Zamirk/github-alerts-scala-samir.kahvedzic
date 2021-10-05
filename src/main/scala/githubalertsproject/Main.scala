package githubalertsproject

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    GithubalertsprojectServer.stream[IO].compile.drain.as(ExitCode.Success)
}
