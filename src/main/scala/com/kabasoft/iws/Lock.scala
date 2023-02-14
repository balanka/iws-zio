package com.kabasoft.iws

import zio.{UIO, ZIOAppDefault, _}

object StmLock extends ZIOAppDefault {

  import zio.stm._

  class Lock private (tref: TRef[Boolean]) {
    def acquire: UIO[Unit] = tref.get.commit.unit
    def release: UIO[Unit] = tref.set(false).commit
  }
  object Lock                              {
    def make: UIO[Lock] = TRef.makeCommit(true).map(new Lock(_))
  }

  val run =
    (for {
      lock   <- Lock.make
      fiber1 <- ZIO
                  .acquireReleaseWith(lock.acquire)(_ => lock.release)(_ => Console.printLine("Bob  : I have the lock!"))
                  .delay(Duration.fromMillis(1))
                  .repeat(Schedule.recurs(10))
                  .fork
      fiber2 <- ZIO
                  .acquireReleaseWith(lock.acquire)(_ => lock.release)(_ => Console.printLine("Sarah: I have the lock!"))
                  .repeat(Schedule.recurs(10))
                  .fork
      _      <- (fiber1 zip fiber2).join
    } yield ())
}
