package com.kabasoft.iws.service

import zio._
import zio.stm._
import com.kabasoft.iws.domain._

object PacSTMService {

  def transfer(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance, amount: BigDecimal): UIO[Unit] =
    STM.atomically {
      to.debit
        .update(_ + amount)
        .*>(from.credit.update(_ + amount))
    }

  def getDebit(account: TPeriodicAccountBalance): UIO[BigDecimal]  = account.debit.get.commit
  def getCredit(account: TPeriodicAccountBalance): UIO[BigDecimal] = account.credit.get.commit
  def printLine(line: String): UIO[Unit]                           = ZIO.succeed(println(line))

  def showBalance(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance): UIO[Unit] =
    for {
      x  <- getDebit(from)
      x1 <- getCredit(from)
      y  <- getCredit(to)
      y1 <- getDebit(to)

      _ <- printLine(s" FROM balance:  debit ${x} credit  ${x1} ")
      _ <- printLine(s"TO balance: debit  ${y1}  credit  ${y} ")

    } yield ()

  def process: UIO[Unit] =
    for {
      idebit   <- TRef.makeCommit(BigDecimal(0))
      debit    <- TRef.makeCommit(BigDecimal(0))
      credit   <- TRef.makeCommit(BigDecimal(0))
      icredit  <- TRef.makeCommit(BigDecimal(0))
      idebit2  <- TRef.makeCommit(BigDecimal(0))
      debit2   <- TRef.makeCommit(BigDecimal(0))
      credit2  <- TRef.makeCommit(BigDecimal(0))
      icredit2 <- TRef.makeCommit(BigDecimal(0))

      from          =
        TPeriodicAccountBalance("202205Account1", "Account1", 202205, idebit, debit, icredit, credit, "EUR", "1000")
      to            =
        TPeriodicAccountBalance("202205Account2", "Account2", 202205, idebit2, debit2, icredit2, credit2, "EUR", "1000")
      _            <- showBalance(from, to)
      _            <- printLine(s"Before Transfering 10000  \n credited Acct  ${from}  \n debited Acct ${to}")
      // _  <-ZIO.collectAllPar(ZIO.replicate(10000)(from.transfer(to, BigDecimal(1))))
      _            <- ZIO.collectAllPar(Chunk.fill(10000)(from.transfer(to, BigDecimal(1))))
      dbalanceFrom <- from.dbalance
      cbalanceFrom <- from.cbalance
      dbalanceTo   <- to.dbalance
      cbalanceTo   <- to.cbalance
      _            <- printLine(s"After Transfering 10000  \n credited Acct  ${from} \n debited Acct  ${to}")
      _            <- printLine(s"After Transfering 10000  \n dbalanceFrom  ${dbalanceFrom} \n cbalanceFrom ${cbalanceFrom}")
      _            <- printLine(s"After Transfering 10000  \n dbalanceTo  ${dbalanceTo} \n cbalanceTo ${cbalanceTo}")
      // _  <- showBalance(from, to)
    } yield ()

}
