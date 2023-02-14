package com.kabasoft.iws.service
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.repository.{ AccountRepository, PacRepository }
import zio._
final class AccountServiceImpl(accRepo: AccountRepository, pacRepo: PacRepository) extends AccountService {
  def getBalance(accId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZIO[Any, RepositoryError, List[Account]] =
    (for {
      accounts <- accRepo.all(companyId)
      period = fromPeriod.toString.slice(0, 4).concat("00").toInt
      pacBalances <- pacRepo.getBalances4Period(fromPeriod, toPeriod, companyId).runCollect.map(_.toList)
      pacs <- pacRepo.find4Period(period, period, companyId).runCollect.map(_.toList)
    } yield {
      val accountsWithBalances = pacBalances.map(pac =>
        accounts.find(acc => pac.account == acc.id)
          map (_.copy(idebit = pac.idebit, debit = pac.debit, icredit = pac.icredit, credit = pac.credit)
          )).flatten
      val accountsWithoutBalances = accounts.filterNot(acc =>accountsWithBalances.map(_.id).contains(acc.id))
      val all = accountsWithBalances:::accountsWithoutBalances
      val account1 = Account.consolidate(accId, all, pacs)
      val account = Account.unwrapData(account1)
      //val result1 = accountsWithBalances.toSet
      //val result = result1.+(account)
      ZIO.logInfo(s"Balance 4 account is ${account}")

      List(account1)++account
    })

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
        .filterNot(x => x.dbalance == zeroAmount || x.cbalance == zeroAmount)
        .map(pac => allAccounts.find(_.id == pac.account).fold(pac)(acc => net(acc, pac, nextPeriod)))
      oldPacs     <- pacRepo.getByIds(pacList.map(_.id), company)
      newPacs      = pacList.filterNot(oldPacs.contains)
      pac_created <- if (newPacs.isEmpty) ZIO.succeed(0) else pacRepo.create(newPacs)
      pac_updated <- if (oldPacs.isEmpty) ZIO.succeed(0) else pacRepo.modify(oldPacs)
    } yield pac_created + pac_updated

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
}
object AccountServiceImpl {
  val live: ZLayer[AccountRepository with PacRepository, RepositoryError, AccountService] =
    ZLayer.fromFunction(new AccountServiceImpl(_, _))
}
