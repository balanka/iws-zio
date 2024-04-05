package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common.{pacMonoid, zeroAmount}
import zio._
import zio.prelude.FlipOps

trait  PostLogisticalTransaction {

   def filterIWS[A <: IWS](list: List[A], param: String): List[A] = list.filter(_.id == param)
   def articleId2AccountId(articleId:String, articles:List[Article], accounts:List[Account]): List[String] =
    filterIWS(articles,  articleId).flatMap(art=>filterIWS(accounts, art.stockAccount).map(_.id))

  def buildPacId(period: Int, accountId:(String,  String)): List[String] =
    List(PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

   def buildPacId2(period: Int, accountId:(String,  String)): (String, String) =
    (PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  def buildPacsFromTransaction(model:Transaction, articles:List[Article], accounts:List[Account], accountId: String): List[PeriodicAccountBalance] =
    model.lines.flatMap { line =>
      val stockAccountIds: List[String] = articleId2AccountId(line.article, articles, accounts)
      stockAccountIds.map( accountId=>createPac (accountId, model, line, debitCredit = true))++
        List(createPac (accountId, model, line, debitCredit = false))
    }

  def groupById(r: List[PeriodicAccountBalance]): List[PeriodicAccountBalance] =
    (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList

   def updatePac(oldPacs: List[PeriodicAccountBalance], tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] =
    for{
      newRecords<- groupById(oldPacs).map(TPeriodicAccountBalance.apply).flip
      _<- newRecords.map ( pac =>transfer(pac, tpacs)).flip
    }yield ( if(tpacs.nonEmpty) tpacs  else newRecords)


  def transfer( pac:TPeriodicAccountBalance,  tpacs:List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id ==  pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))

  def createPac (accountId:String, model:Transaction, line:TransactionDetails, debitCredit:Boolean): PeriodicAccountBalance = {
    val amount = line.quantity.multiply(line.price)
    val debitAmount = if (debitCredit) amount else zeroAmount
    val creditAmount = if (debitCredit) zeroAmount else amount
    PeriodicAccountBalance.apply(
      PeriodicAccountBalance.createId(model.period, accountId),
      accountId,
      model.period,
      zeroAmount,
      zeroAmount,
      debitAmount,
      creditAmount,
      line.currency,
      model.company,
      "",
      PeriodicAccountBalance.MODELID
    )
  }

  def createPac (accountId:String, model:Transaction, debitCredit:Boolean): PeriodicAccountBalance = {
    val amount = model.total
    val currency = model.lines.headOption.getOrElse(TransactionDetails.dummy).currency
    val debitAmount = if (debitCredit) amount else zeroAmount
    val creditAmount = if (debitCredit) zeroAmount else amount
    PeriodicAccountBalance.apply(
      PeriodicAccountBalance.createId(model.period, accountId),
      accountId,
      model.period,
      zeroAmount,
      zeroAmount,
      debitAmount,
      creditAmount,
      currency,
      model.company,
      "",
      PeriodicAccountBalance.MODELID
    )
  }
   def buildJournalEntries(model: Transaction, line: TransactionDetails,
                                  pac: PeriodicAccountBalance, account: String, oaccount: String,side:Boolean): Journal =
    Journal(
      -1,
      model.id,
      model.oid,
      account,
      oaccount,
      model.transdate,
      model.postingdate,
      model.enterdate,
      model.getPeriod,
      line.quantity.multiply(line.price),
      pac.idebit,
      pac.debit,
      pac.icredit,
      pac.credit,
      line.currency,
      side,
      line.text,
      model.month.toInt,
      model.year,
      model.company,
      model.modelid
    )

  def buildTransactionLog(models: List[Transaction], stocks:List[Stock], newStock: List[Stock],
                                  articles:List[Article] ): ZIO[Any, Nothing, List[TransactionLog]] = ZIO.succeed {
    val allStock = stocks++newStock
    models.flatMap(tr => tr.lines.map( line =>
      allStock.find(_.id == tr.store.concat(line.article).concat(tr.company).concat("")) // Find stock for article  in  store
        .flatMap(st=> articles.find(_.id == st.article).map( article =>                    // Find article for the line
          TransactionLog(0L, tr.id, tr.oid, tr.store, tr.costcenter, line.article, line.quantity // build TransactionLog
            , st.quantity, /*article.wholeStock*/ zeroAmount, article.quantityUnit , article.pprice , article.avgPrice
            , article.currency, line.duedate, line.text, tr.transdate, tr.postingdate, tr.enterdate, tr.period, tr.company, tr.modelid)
        ))).map(_.toList)).flatten
  }
}

