package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, PacRepository}
import zio._
import zio.prelude._

final class AccountServiceImpl(accRepo:AccountRepository, pacRepo:PacRepository) extends AccountService {

  def closePeriod(fromPeriod: Int, toPeriod: Int, inStmtAccId:String, company: String):ZIO[Any, RepositoryError, Int]=
    (for {

      pacs <- pacRepo.findBalance4Period(fromPeriod, toPeriod, company)
        .runCollect.map(_.toList)
      allAccounts <- accRepo.list(company).runCollect.map(_.toList)
      currentYear = fromPeriod.toString.slice(0, 4).toInt
      currentPeriod = currentYear.toString.concat("00").toInt
      nextPeriod = (currentYear + 1).toString.concat("00").toInt
      initial <- pacRepo.find4Period(fromPeriod, toPeriod, company).runCollect.map(_.toList)
      list = Account.flattenTailRec(Set(Account.withChildren(inStmtAccId, allAccounts.toList)))
      initpacList  = (pacs ++: initial)
        .groupBy(_.account)
        .map ({ case (_, v) =>
          v.toList match {
            case Nil       => PeriodicAccountBalance.dummy
            case (x :: xs) => NonEmptyList.fromIterable(x, xs).reduce
          }
        }).toList

      filteredList  = initpacList.filterNot(x => {
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

      pac_created <- pacRepo.create(pacList)
    } yield pac_created)
}
object AccountServiceImpl {
  val live: ZLayer[AccountRepository with PacRepository, RepositoryError, AccountService] =
    ZLayer.fromFunction(new AccountServiceImpl(_,_))
}



