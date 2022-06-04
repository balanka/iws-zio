package com.kabasoft.iws.service
/*
import com.kabasoft.iws.domain._

import zio._
import zio.stm._

object AccountService {

  def getParents(id: String, accList: List[Account]) =
    accList.filter(acc => acc.id == id)

  def copyIDebitICredit(account: Account, accList: List[PeriodicAccountBalance]) =
    accList.find(_.account == account.id) match {
      case Some(acc) => List(account.copy(idebit = acc.idebit, icredit = acc.icredit))
      case None => List.empty[Account]
    }
  def getBalances(accId: String, fromPeriod: Int, toPeriod: Int, company: String) =
    (for {
      list <- SQL.Account.listX(fromPeriod, toPeriod, company).map(Account.apply).to[List]
      period = fromPeriod.toString.slice(0, 4).concat("00").toInt
      pacs <- SQL.PeriodicAccountBalance.find4Period(company, List(period, period)).to[List]
    } yield {
      val account = Account.consolidate(accId, list, pacs)
      Account.unwrapDataTailRec(account) //.filterNot(acc => acc.id==accId)
    }).transact(transactor)

  def closePeriod(fromPeriod: Int, toPeriod: Int, company: String): F[List[Int]] =
    (for {
      pacs <- SQL.PeriodicAccountBalance.findBalance4Period(company, List(fromPeriod, toPeriod)).to[List]
      allAccounts <- SQL.Account.list(company).map(Account.apply).to[List]
      currentYear = fromPeriod.toString.slice(0, 4).toInt
      currentPeriod = currentYear.toString.concat("00").toInt
      nextPeriod = (currentYear + 1).toString.concat("00").toInt
      initial <- SQL.PeriodicAccountBalance.find4Period(company, List(currentPeriod, currentPeriod)).to[List]
      list = Account.flattenTailRec(Set(Account.withChildren(inStmtAccId, allAccounts)))
      initpacList = (pacs ++ initial)
        .groupBy(_.account)
        .map({ case (_, v) => v.combineAll(pacMonoid) })
        .toList

      filteredList = initpacList.filterNot(x => {
        list.find(_.id == x.account) match {
          case Some(_) => true
          case None => false
        }
      })

      pacList = filteredList
        .filterNot(x => (x.dbalance == 0 || x.cbalance == 0))
        .map(pac => {
          allAccounts.find(_.id == pac.account) match {
            case Some(acc) =>
              if (acc.isDebit)
                pac
                  .copy(id = PeriodicAccountBalance.createId(nextPeriod, acc.id), period = nextPeriod)
                  .idebiting(pac.debit - pac.icredit - pac.credit)
                  .copy(debit = 0)
                  .copy(icredit = 0)
                  .copy(credit = 0)
              else
                pac
                  .copy(id = PeriodicAccountBalance.createId(nextPeriod, acc.id), period = nextPeriod)
                  .icrediting(pac.credit - pac.idebit - pac.debit)
                  .copy(credit = 0)
                  .copy(idebit = 0)
                  .copy(debit = 0)
            case None => pac
          }
        })
      pac_created <- pacList.traverse(SQL.PeriodicAccountBalance.create(_).run)
    } yield pac_created).transact(transactor)
}

 */
