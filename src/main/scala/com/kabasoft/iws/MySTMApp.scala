package com.kabasoft.iws

import com.kabasoft.iws.domain.TPeriodicAccountBalance
import zio._
import zio.stm.TRef

import java.math.BigDecimal

object MySTMApp extends ZIOAppDefault {

  def showDebitAccount(account: TPeriodicAccountBalance): UIO[BigDecimal] =
    account.debit.get.commit

  def showCreditAccount(account: TPeriodicAccountBalance): UIO[BigDecimal] =
    account.credit.get.commit

  def showBalance(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance): UIO[Unit] =
    for {
      x  <- showDebitAccount(from)
      x1 <- showCreditAccount(from)
      y  <- showCreditAccount(to)
      y1 <- showDebitAccount(to)

      _ <- ZIO.logInfo(s"FROM Account ${from.id} :  debit: ${x}  credit:${x1} balance: ${x.subtract(x1)}")
      _ <- ZIO.logInfo(s"TO Account ${to.id}  debit:  ${y1}  credit:  ${y} balance: ${y1.subtract(y)}")
    } yield ()

  def run = for {
    //debit    <- TRef.makeCommit(new BigDecimal(4000000))
    debit    <- TRef.makeCommit(BigDecimal.ZERO)
    idebit   <- TRef.makeCommit(BigDecimal.ZERO)
    credit   <- TRef.makeCommit(BigDecimal.ZERO)
    icredit  <- TRef.makeCommit(BigDecimal.ZERO)
    idebit2  <- TRef.makeCommit(BigDecimal.ZERO)
    debit2   <- TRef.makeCommit(BigDecimal.ZERO)
    credit2  <- TRef.makeCommit(BigDecimal.ZERO)
    icredit2 <- TRef.makeCommit(BigDecimal.ZERO)

    from = TPeriodicAccountBalance("1", "Account1", 202201, idebit, icredit, debit, credit, "EUR", "1000")
    to   = TPeriodicAccountBalance("2", "Account2", 202201, idebit2, icredit2, debit2, credit2, "EUR", "1000")

    _ <- showBalance(from, to)
    _ <- ZIO.logInfo(s"Before Transfering   \n credited Acct  ${from}  \n debited Acct ${to}")
    //_ <- ZIO.collectAllPar(Chunk.fill(4000000)(from.transfer(to, new BigDecimal(1))))
    _ <- ZIO.collectAllPar(Chunk.fill(4)(from.transfer(to, new BigDecimal(1))))
    // _ <- ZIO.foreachPar((1 to 4000000).toList) { _ => from.transfer(to, new BigDecimal(1)) }
    _ <- ZIO.logInfo(s"After Transfering   \n credited Acct  ${from} \n debited Acct  ${to}")
    _ <- showBalance(from, to)
  } yield ()

}
