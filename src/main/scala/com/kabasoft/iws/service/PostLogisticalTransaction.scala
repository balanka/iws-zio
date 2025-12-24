package com.kabasoft.iws.service
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{given, _}
import zio._
import zio.prelude.FlipOps

import java.time.Instant

trait  PostLogisticalTransaction:

  def filterIWS[A <: IWS](list: List[A], param: String): List[A] = list.filter(_.id == param)

  def articleId2Account(articleId: String, articles: List[Article], accounts: List[Account], flag:Boolean): Account = {
    filterIWS(articles, articleId).flatMap(art => {
      if (flag) filterIWS(accounts, art.account) else  filterIWS(accounts, art.oaccount)
    }).headOption.getOrElse(Account.dummy)
  }

  def buildPacId(period: Int, accountId: (String, String)): List[String] =
    List(PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  def buildPacId2(period: Int, accountId: (String, String)): (String, String) =
    (PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  def buildPacsFromTransaction(model: Transaction, articles: List[Article], accounts: List[Account], oaccountId:String): List[PeriodicAccountBalance] =
    val x:List[PeriodicAccountBalance] = model.lines.flatMap(line =>  createPac( model, line, accounts, articles, oaccountId))
    x.groupBy(_.id).map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy) }.toList

  def postFinancials(models: List[FinancialsTransaction], financialsService:FinancialsService):
  ZIO[Any, RepositoryError, List[(FinancialsTransaction, List[PeriodicAccountBalance], UIO[List[PeriodicAccountBalance]]
    , List[Journal])]] = models.map( postOneFinancials(_, financialsService) ).flip 

  def postOneFinancials(model: FinancialsTransaction, financialsService:FinancialsService):
  ZIO[Any, RepositoryError, (FinancialsTransaction, List[PeriodicAccountBalance], UIO[List[PeriodicAccountBalance]], List[Journal])] = for {
    (models, newRecords, oldPacs, journalEntries) <- financialsService.postNewFinancialsTransaction(model)
  } yield (models, newRecords, oldPacs, journalEntries)   
    
  def buildTransaction(model: Transaction, articles: List[Article], accounts: List[Account], suppliers: List[BusinessPartner]
                       , vats: List[Vat], oaccountId: String, modelid: Int): (Transaction, FinancialsTransaction) = {
    // Fetch the supplier using its id from the transaction (account field)
    val partnerAccountId = suppliers.filter(_.id == model.account)
      .flatMap(s => accounts.filter(_.id == s.account))
      .headOption.getOrElse(Account.dummy).id
    val currency = model.lines.headOption.getOrElse(TransactionDetails.dummy).currency
    // build details for vat
    val vatDetails:List[FinancialsTransactionDetails] = model.lines.map { l =>
      val vat = vats.find(vat => vat.id == l.vatCode).getOrElse(Vat.dummy)
      if (model.modelid == TransactionModelId.SUPPLIER_INVOICE.id) {
        val accountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
        val oaccountName = accounts.find(_.id == vat.inputVatAccount).fold("")(acc => acc.name)
        FinancialsTransactionDetails(-1, 0, vat.inputVatAccount, side = true, partnerAccountId, l.quantity.multiply(l.price).multiply(vat.percent)
          , Instant.now(), l.text, currency, model.company, accountName, oaccountName)
      } else if (model.modelid == TransactionModelId.CUSTOMER_INVOICE.id) {
        val accountName = accounts.find(_.id == vat.outputVatAccount).fold("")(acc => acc.name)
        val oaccountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
        FinancialsTransactionDetails(-1, 0, partnerAccountId, side = true, vat.outputVatAccount, l.quantity.multiply(l.price).multiply(vat.percent)
          , Instant.now(), l.text, currency, model.company, accountName, oaccountName)
      }else FinancialsTransactionDetails.dummy
    }.filterNot(_.account == FinancialsTransactionDetails.dummy.account)
    // build details for net amount
    val netDetails:List[FinancialsTransactionDetails] = model.lines.map { line =>
      if (model.modelid == TransactionModelId.GOORECEIVING.id) {
        val account = articleId2Account(line.article, articles, accounts, true)
        val oaccountName = accounts.find(_.id == oaccountId).fold("")(acc => acc.name)
        FinancialsTransactionDetails(-1, 0, account.id, side = true, oaccountId, line.quantity.multiply(line.price), Instant.now()
          , model.text, currency, model.company, account.name, oaccountName)
      } else if (model.modelid == TransactionModelId.BILL_OF_DELIVERY.id) {
        val article = articles.find(_.id == line.article).getOrElse(Article.dummy)
        val account = articleId2Account(line.article, articles, accounts, false)
        val oaccount = articleId2Account(line.article, articles, accounts, true)
        FinancialsTransactionDetails(-1, 0, account.id, side = true, oaccount.id, line.quantity.multiply(article.avgPrice), Instant.now()
          , model.text, currency, model.company, account.name, oaccount.name)
      } else if (model.modelid == TransactionModelId.SUPPLIER_INVOICE.id) {
        val accountId = oaccountId
        val accountName = accounts.find(_.id == accountId).fold("")(acc => acc.name)
        val oaccountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
        FinancialsTransactionDetails(-1, 0, accountId, side = true, partnerAccountId, line.quantity.multiply(line.price), Instant.now()
          , model.text, currency, model.company, accountName, oaccountName)
      } else if (model.modelid == TransactionModelId.CUSTOMER_INVOICE.id) {
        val oaccountName = accounts.find(_.id == oaccountId).fold("")(acc => acc.name)
        val accountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
        FinancialsTransactionDetails(-1, 0, partnerAccountId , side = true, oaccountId, line.quantity.multiply(line.price), Instant.now()
          , model.text, currency, model.company, accountName, oaccountName)
      }else FinancialsTransactionDetails.dummy  
    }.groupBy(line=>(line.account, line.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)
    }.toList
    
    val details:List[FinancialsTransactionDetails]= (netDetails ++ vatDetails).filterNot(_.account == FinancialsTransactionDetails.dummy.account)
      .groupBy(d=>(d.account, d.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)}.toList
    val financialsTransaction = FinancialsTransaction(-1, model.id, 0, model.store, partnerAccountId, model.transdate
      , Instant.now(), Instant.now(), model.period, posted = false, modelid, model.company, model.text, -1, -1, details)
    (model.copy(posted = true), financialsTransaction.copy(posted=true))
  }

  def groupById(r: List[PeriodicAccountBalance]): List[PeriodicAccountBalance] =
    (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList

  def updatePac(oldPacs: List[PeriodicAccountBalance], tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] = 
    for 
        newRecords <- groupById(oldPacs).map(TPeriodicAccountBalance.apply).flip
        _ <- newRecords.map(pac => transfer(pac, tpacs)).flip
    yield tpacs

  def makeJournal(models: List[Transaction], newPacs: List[PeriodicAccountBalance], oldPacs: List[UIO[PeriodicAccountBalance]],
                  articles: List[Article], oaccountId: String): ZIO[Any, Nothing, List[Journal]] =
    if (articles.isEmpty) throw IllegalStateException(" Error: a  goodreceiving transaction without article may not be posted!!!")
    for {
      oldPacList <- oldPacs.flip
      journal = models.flatMap(model =>
        model.lines.flatMap { line =>
          val article: Article = articles.find(_.id == line.article).getOrElse(Article.dummy)
          val pacId = buildPacId2(model.getPeriod, (article.account, oaccountId))
          val pac = findOrBuildPac(pacId._1, model.getPeriod, oldPacList ++ newPacs)
          val poac = findOrBuildPac(pacId._2, model.getPeriod, oldPacList ++ newPacs)
          val jou1 = buildJournalEntries(model, line, pac, article.account, oaccountId, side = true, article)
          val jou2 = buildJournalEntries(model, line, poac, oaccountId, article.account, side = false, article)
          List(jou1, jou2)
        }.groupBy(j =>(j.account, j.oaccount, j.side)) map { case (_, v) => common.reduce(v, Journal.dummy)})
      _<- ZIO.logInfo(s"Journal ${journal}")
    } yield journal

  def transfer(pac: TPeriodicAccountBalance, tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id == pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.iterator.find(_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))

  private def createPac(model: Transaction, line: TransactionDetails, accounts: List[Account], articles: List[Article], oaccountId:String): List[PeriodicAccountBalance] = {
    val article: Article = articles.find(_.id == line.article).getOrElse(Article.dummy)
    val pacList =  if (model.modelid == TransactionModelId.BILL_OF_DELIVERY.id)  {
      val accountId = oaccountId
      val account = accounts.find(_.id == accountId).getOrElse(Account.dummy)
      val oaccount = accounts.find(_.id == article.account).getOrElse(Account.dummy)
      val amount = line.quantity.multiply(article.avgPrice)
      
      List(PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, account.id),
        account.id,
        model.period,
        zeroAmount,
        zeroAmount,
        amount,
        zeroAmount,
        amount,
        zeroAmount,
        line.currency,
        model.company,
        account.name,
        PeriodicAccountBalance.MODELID
      ),
       PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, oaccount.id),
        oaccount.id,
        model.period,
        zeroAmount,
        zeroAmount,
        amount,
        zeroAmount,
        amount,
        zeroAmount,
        line.currency,
        model.company,
        oaccount.name,
        PeriodicAccountBalance.MODELID
      ))
    } else {
      val oaccount = accounts.find(_.id == oaccountId).getOrElse(Account.dummy)
      val account = accounts.find(_.id == article.account).getOrElse(Account.dummy)
      val amount = line.quantity.multiply(line.price)
      List(PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, account.id),
        account.id,
        model.period,
        zeroAmount,
        zeroAmount,
        amount,
        zeroAmount,
        amount,
        zeroAmount,
        line.currency,
        model.company,
        account.name,
        PeriodicAccountBalance.MODELID
      ),
      PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, oaccount.id),
        oaccount.id,
        model.period,
        zeroAmount,
        zeroAmount,
        zeroAmount,
        amount,
        zeroAmount,
        amount,
        line.currency,
        model.company,
        oaccount.name,
        PeriodicAccountBalance.MODELID
      )
     )
    }
    pacList
  }

  def buildJournalEntries(model: Transaction, line: TransactionDetails,
                          pac: PeriodicAccountBalance, account: String, oaccount: String, side: Boolean, article: Article): Journal = {
    val parentAccount= ""
    val parentOAccount= ""
    val amount = if (model.modelid == TransactionModelId.BILL_OF_DELIVERY.id) {
      line.quantity.multiply(article.avgPrice)
    } else {
      line.quantity.multiply(line.price)
    }
    Journal(
      -1,
      model.id,
      model.oid,
      account,
      oaccount,
      parentAccount,
      parentOAccount,
      model.transdate,
      model.postingdate,
      model.enterdate,
      model.getPeriod,
      amount,
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
  }

  def buildTransactionLog(models: List[Transaction], stocks: List[Stock], newStock: List[Stock],
                          articles: List[Article]): ZIO[Any, Nothing, List[TransactionLog]] = ZIO.succeed {
    val allStock = stocks ++ newStock
    models.flatMap(tr => tr.lines.map(line => // Extract lines transaction from each transaction 
      allStock.find(_.id == tr.store.concat(line.article).concat(tr.company).concat("")) // Find stock for article  in  store
        .flatMap(st => articles.find(_.id == st.article).map(article => // Find article for the line
          TransactionLog(0L, tr.id, tr.id1, tr.oid, tr.store, tr.account, line.article, line.quantity // build TransactionLog
            , st.quantity, /*article.wholeStock*/ zeroAmount, article.quantityUnit, line.price, article.avgPrice
            , article.currency, line.duedate, line.text, tr.transdate, tr.postingdate, tr.enterdate, tr.period, tr.company, tr.modelid)
        ))).map(_.toList)).flatten
  }


