package com.kabasoft.iws.service

import com.kabasoft.iws.domain.TPeriodicAccountBalance
import zio._
import zio.stm._

object Transfer {

  def showDebitAccount(account: TPeriodicAccountBalance): UIO[BigDecimal] =
    account.debit.get.commit
  def showCreditAccount(account: TPeriodicAccountBalance): UIO[BigDecimal] =
    account.credit.get.commit


  def showBalance(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance): UIO[Unit] =
    for {
        x <- showDebitAccount(from)
        x1 <- showCreditAccount(from)
        y <- showCreditAccount(to)
        y1 <- showDebitAccount(to)

      _ <- ZIO.logInfo(  s"FROM Account ${from.id } :  debit: ${x}  credit:${x1} balance: ${x-x1}" )
      _ <- ZIO.logInfo( s"TO Account ${to.id }  debit:  ${y1}  credit:  ${y} balance: ${y1-y}"  )
    } yield ()

  def main: UIO[Unit] =

    for {
       idebit <- TRef.makeCommit(BigDecimal(0))
       debit <- TRef.makeCommit(BigDecimal(4000000) )
       credit <- TRef.makeCommit(BigDecimal(0) )
       icredit <- TRef.makeCommit(BigDecimal(0) )
       idebit2 <- TRef.makeCommit(BigDecimal(0) )
       debit2 <- TRef.makeCommit(BigDecimal(0) )
       credit2 <- TRef.makeCommit(BigDecimal(0) )
       icredit2 <- TRef.makeCommit(BigDecimal(0) )

      from = TPeriodicAccountBalance("1", "Account1", 202201, idebit,  icredit, debit,  credit,"EUR", "1000" )
      to = TPeriodicAccountBalance("2", "Account2", 202201, idebit2, icredit2, debit2,  credit2,"EUR", "1000" )

      _   <- showBalance(from, to)
      _   <- ZIO.logInfo(s"Before Transfering 4000000  \n credited Acct  ${from}  \n debited Acct ${to}")
      _  <- ZIO.foreachPar((1 to 4000000).toList) { _ => from.transfer(to, BigDecimal(1))}
      _  <- ZIO.logInfo(s"After Transfering 4000000  \n credited Acct  ${from} \n debited Acct  ${to}")
      _  <- showBalance(from, to)
    } yield ()

}

