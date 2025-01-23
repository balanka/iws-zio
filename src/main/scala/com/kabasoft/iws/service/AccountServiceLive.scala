package com.kabasoft.iws.service
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.domain.common.{given, *}
import com.kabasoft.iws.repository.{ AccountRepository, PacRepository }
import zio._

final class AccountServiceLive(accRepo: AccountRepository, pacRepo: PacRepository) extends AccountService:
  def getBalance(accId: String, toPeriod: Int, companyId: String): ZIO[Any, RepositoryError, List[Account]] =
    (for {
      _<- ZIO.logInfo(s" >>>>>>>> toPeriod: $toPeriod for companyId $companyId" )
      accounts <- accRepo.all((Account.MODELID, companyId))
      period00 = toPeriod.toString.slice(0, 4).concat("00").toInt
      pacBalances <- pacRepo.findBalance4Period(toPeriod, companyId)
      pacs <- pacRepo.findBalance4Period( period00, companyId)
    } yield {
      val accountsWithBalances = pacBalances.flatMap(pac =>
        accounts.find(acc => pac.account == acc.id).map (_.copy(idebit = pac.idebit, debit = pac.debit, icredit = pac.icredit, credit = pac.credit)))
      val accountsWithoutBalances = accounts.filterNot(acc => accountsWithBalances.map(_.id).contains(acc.id))
      val all                     = accountsWithBalances ::: accountsWithoutBalances
      val account1                = Account.consolidate(accId, all, pacs)
      val account                 = Account.unwrapData(account1)
      ZIO.logInfo(s"Balance 4 account is ${account}")
      List(account1) ++ account
    }).mapBoth(e => RepositoryError(e.message), a => a)

  def closePeriod(toPeriod: Int, inStmtAccId: String, company: String): ZIO[Any, RepositoryError, Int] = {
    val currentYear   = toPeriod.toString.slice(0, 4).toInt
    val fromPeriod = currentYear.toString.concat("01").toInt
    val nr = for {
      pacs         <- pacRepo.findBalance4Period(fromPeriod, toPeriod, company)
      allAccounts  <- accRepo.all(Account.MODELID, company)
      currentPeriod = currentYear.toString.concat("00").toInt
      nextPeriod    = (currentYear + 1).toString.concat("00").toInt
      initial      <- pacRepo.findBalance4Period(currentPeriod, company)
      list          = Account.flattenTailRec(Set(Account.withChildren(inStmtAccId, allAccounts)))
      initpacList   = (pacs ++: initial)
                        .groupBy(_.account)
                        .map { case (_, v) => reduce(v, PeriodicAccountBalance.dummy) }
                        .filterNot(x => x.id == PeriodicAccountBalance.dummy.id)
                        .toList
      filteredList = initpacList.filterNot(x => list.map(_.id ).contains(x.account))
      //filteredList1 = initpacList.filterNot(x => list.find(_.id == x.account).fold(false)(_ => true))

      pacList      = filteredList
                       .filterNot(x => x.dbalance == zeroAmount || x.cbalance == zeroAmount)
                       .map(pac => allAccounts.find(_.id == pac.account).fold(pac)(acc => net(acc, pac, nextPeriod)))
      oldPacs     <- pacRepo.getBy(pacList.map(_.id), PeriodicAccountBalance.MODELID, company).map(_.filterNot(x => x.id == PeriodicAccountBalance.dummy.id))
      newPacs      = pacList.filterNot(oldPacs.contains)
      pac_created <- if (newPacs.isEmpty) ZIO.succeed(1) else pacRepo.create(newPacs).debug("pac_created")
      pac_updated <- if (oldPacs.isEmpty) ZIO.succeed(1) else pacRepo.update(oldPacs).debug("pac_updated")
    } yield  pac_created + pac_updated
    nr.mapBoth(e => RepositoryError(e.message), a => a)
  }

  private def net(acc: Account, pac: PeriodicAccountBalance, nextPeriod: Int) =
    if (acc.isDebit) {
      pac
        .copy(id = PeriodicAccountBalance.createId(nextPeriod, acc.id), period = nextPeriod)
        .idebiting(pac.debit.subtract(pac.icredit).subtract(pac.credit))
        .copy(debit = zeroAmount, icredit = zeroAmount, credit = zeroAmount)
    } else {
      pac
        .copy(id = PeriodicAccountBalance.createId(nextPeriod, acc.id), period = nextPeriod)
        .icrediting(pac.credit.subtract(pac.idebit).subtract(pac.debit))
        .copy(credit = zeroAmount, idebit = zeroAmount, debit = zeroAmount)
    }

object AccountServiceLive:
  val live: ZLayer[AccountRepository & PacRepository, RepositoryError, AccountService] =
    ZLayer.fromFunction(new AccountServiceLive(_, _))

