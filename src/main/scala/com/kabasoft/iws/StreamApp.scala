package com.kabasoft.iws

import zio._
import zio.stream._

object StreamApp extends ZIOAppDefault {
  val ZERO = BigDecimal(0)
  trait AppError
  case class DBError(message: String) extends AppError
  case class A(id: Int, amount: BigDecimal)
  case class B(id: Int, amount: BigDecimal)
  def f1(input: A): ZIO[Any, Nothing, B]                                              = ZIO.succeed(B(input.id, input.amount * 100))
  def f2(input: A): ZIO[Any, Nothing, B]                                              = ZIO.succeed(B(input.id, input.amount * 200))
  def persist(bs: B): ZIO[Any, Nothing, BigDecimal]                                   = ZIO.succeed(bs.amount)
  def persistA(bs: A): ZIO[Any, Nothing, BigDecimal]                                  = ZIO.succeed(bs.amount)
  val s1                                                                              = ZStream(A(1, BigDecimal(100)), A(2, BigDecimal(200)), A(3, BigDecimal(-300)), A(4, BigDecimal(-400)))
  val sink1: ZSink[Any, Nothing, A, Nothing, List[zio.ZIO[Any, Nothing, BigDecimal]]] =
    ZSink.collectAll[A].map(_.toList.map(persistA))

  def run = (for {
    result  <-
      s1.tapSink(sink1)
        .partition(bs => bs.amount.compareTo(ZERO) >= 0)
        .map(bs =>
          (bs._1
            .mapZIO(f1)
            .merge(bs._2.mapZIO(f2)))
        )
    result1 <- result
                 .mapZIO(x => persist(x))
                 .runCollect
                 .map(_.toList)
  } yield (result1))
}
