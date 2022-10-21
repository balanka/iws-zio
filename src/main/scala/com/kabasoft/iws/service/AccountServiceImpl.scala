package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.repository.{ AccountRepository, PacRepository }
import zio._
import zhttp.logging.Logger
import scala.annotation.tailrec

final class AccountServiceImpl(accRepo: AccountRepository, pacRepo: PacRepository) extends AccountService {
  val logger = Logger.make.withLevel(zhttp.logging.LogLevel.Error)

  def getBalance(accId: String, fromPeriod: Int, toPeriod: Int, companyId: String
  ): ZIO[Any, RepositoryError, Account] =
    for {
      accounts    <- accRepo.list(companyId).runCollect.map(_.toList)
      period       = fromPeriod.toString.slice(0, 4).concat("00").toInt
      pacBalances <- pacRepo.getBalances4Period(fromPeriod, toPeriod, companyId).runCollect.map(_.toList)
      pacs        <- pacRepo.find4Period(period, period, companyId).runCollect.map(_.toList)
    } yield {
      val all = (pacBalances ::: pacs)
        .groupBy(_.account)
        .map { case (k, v) => reduce(v, PeriodicAccountBalance.dummy) }
        .filterNot(_.id == PeriodicAccountBalance.dummy.id)
        .toList

      val acc     = accounts.filter(_.id == accId)
      val list    = all
        .flatMap(pac =>
          accounts
            .find(acc => pac.account == acc.id)
            .map(_.copy(idebit = pac.idebit, debit = pac.debit, icredit = pac.icredit, credit = pac.credit))
        ) //::: acc
      println(" list::::::" + list)
      val account = group(accId, list, accounts)
      logger.info(" ACCOUNTS::::::" + account)
      account
    }


  @tailrec
  def group(accid: String, accounts0: List[Account], all: List[Account]): Account =
    if (accounts0.size == 2 && accounts0.head.account.equals(accid) ) accounts0.head
    else {
      val accounts1 = accounts0
        .groupBy(_.account)
        .map { case (_, v) => reduce(v, Account.dummy).copy(subAccounts = v.toSet) }
        .filterNot(_.id == Account.dummy.id)
        .flatMap(pac =>
        all
          .find(acc => acc.id == pac.account)
          .map(_.copy(idebit = pac.idebit, debit = pac.debit, icredit = pac.icredit, credit = pac.credit))
          ).toList
      println(" lisaccounts1::::::" + accounts1)
      group(accid, accounts1, all)
    }

  def closePeriod(fromPeriod: Int, toPeriod: Int, inStmtAccId: String, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      pacs         <- pacRepo.findBalance4Period(fromPeriod, toPeriod, company).runCollect.map(_.toList)
      allAccounts  <- accRepo.list(company).runCollect.map(_.toList)
      currentYear   = fromPeriod.toString.slice(0, 4).toInt
      currentPeriod = currentYear.toString.concat("00").toInt
      nextPeriod    = (currentYear + 1).toString.concat("00").toInt
      initial      <- pacRepo.find4Period(currentPeriod, currentPeriod, company).runCollect.map(_.toList)
      list          = Account.flattenTailRec(Set(Account.withChildren(inStmtAccId, allAccounts)))
      initpacList   = (pacs ++: initial)
                        .groupBy(_.account)
                        .map { case (_, v) => reduce(v, PeriodicAccountBalance.dummy) }
                        .filterNot(x => x.id == PeriodicAccountBalance.dummy.id)
                        .toList

      filteredList = initpacList.filterNot(x => list.find(_.id == x.account).fold(false)(_ => true))

      pacList      = filteredList
                       .filterNot(x => x.dbalance == 0 || x.cbalance == 0)
                       .map(pac => allAccounts.find(_.id == pac.account).fold(pac)(acc => net(acc, pac, nextPeriod)))
      oldPacs     <- pacRepo.getByIds(pacList.map(_.id), company)
      newPacs      = pacList.filterNot(oldPacs.contains)
      pac_created <- if (newPacs.isEmpty) ZIO.succeed(0) else pacRepo.create(newPacs)
      pac_updated <- if (oldPacs.isEmpty) ZIO.succeed(0) else pacRepo.modify(oldPacs)
    } yield pac_created + pac_updated

  def net(acc: Account, pac: PeriodicAccountBalance, nextPeriod: Int) =
    if (acc.isDebit) {
      pac
        .copy(id = PeriodicAccountBalance.createId(nextPeriod, acc.id), period = nextPeriod)
        .idebiting(pac.debit - pac.icredit - pac.credit)
        .copy(debit = 0, icredit = 0, credit = 0)
    } else {
      pac
        .copy(id = PeriodicAccountBalance.createId(nextPeriod, acc.id), period = nextPeriod)
        .icrediting(pac.credit - pac.idebit - pac.debit)
        .copy(credit = 0, idebit = 0, debit = 0)
    }
}
object AccountServiceImpl {
  val live: ZLayer[AccountRepository with PacRepository, RepositoryError, AccountService] =
    ZLayer.fromFunction(new AccountServiceImpl(_, _))
}
