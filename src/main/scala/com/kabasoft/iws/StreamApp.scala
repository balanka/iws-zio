package com.kabasoft.iws

import com.kabasoft.iws.domain.common.zeroAmount
import zio._
import zio.stream._

import java.math.{BigDecimal, RoundingMode}

object StreamApp extends ZIOAppDefault {

  trait AppError
  case class DBError(message: String) extends AppError
  case class A(id: Int, amount: BigDecimal)
  case class B(id: Int, amount: BigDecimal)
  def f1(input: A): ZIO[Any, Nothing, B]                                              = ZIO.succeed(B(input.id, input.amount.multiply(amount200)))
  def f2(input: A): ZIO[Any, Nothing, B]                                              = ZIO.succeed(B(input.id, input.amount.multiply(amount200) ))
  def persist(bs: B): ZIO[Any, Nothing, BigDecimal]                                   = ZIO.succeed(bs.amount)
  def persistA(bs: A): ZIO[Any, Nothing, BigDecimal]                                  = ZIO.succeed(bs.amount)
  val amount100 = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
  val amount200 = new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP)
  val amount300_ = new BigDecimal("-300.00").setScale(2, RoundingMode.HALF_UP)
  val amount400_ = new BigDecimal("-400.00").setScale(2, RoundingMode.HALF_UP)
  val s1                                                                              =
    ZStream(A(1, amount100), A(2, amount200), A(3, amount300_), A(4, amount400_))
  val sink1: ZSink[Any, Nothing, A, Nothing, List[zio.ZIO[Any, Nothing, BigDecimal]]] =
    ZSink.collectAll[A].map(_.toList.map(persistA))

  def run = for {
    result  <-
      s1.tapSink(sink1)
        .partition(bs => bs.amount.compareTo(zeroAmount) >= 0)
        .map(bs =>
          (bs._1
            .mapZIO(f1)
            .merge(bs._2.mapZIO(f2)))
        )
    result1 <- result
                 .mapZIO(x => persist(x))
                  .tap(e=>ZIO.logInfo(s"Element: $e"))
                 .runCollect
                 .map(_.toList)
  } yield (result1)
}
