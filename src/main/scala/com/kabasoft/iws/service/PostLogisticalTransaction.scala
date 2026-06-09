package com.kabasoft.iws.service

import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{given, _}
import com.kabasoft.iws.domain.TransactionModelId._
import zio._
import zio.prelude.FlipOps // still used for some prelude ops, but not misused
import java.time.Instant
import java.math.BigDecimal

trait PostLogisticalTransaction:

  // --- safe helpers ----------------------------------------------------------
  def filterIWS[A <: IWS](list: List[A], param: String): List[A] = list.filter(_.id == param)

  def findObjectById [A<:IWS](accounts: List[A], id: String): ZIO[Any, RepositoryError, A] =
    ZIO.getOrFailWith(RepositoryError(s"Object with id $id not found"))(accounts.find(_.id == id))


  def articleId2Account(articleId: String, articles: List[Article], accounts: List[Account], flag:Boolean): Account = {
    filterIWS(articles, articleId).flatMap(article =>{
    if (flag) {
      filterIWS(accounts, article.account)
    } else {
      filterIWS(accounts, article.oaccount)
    }
    })
  }.headOption.getOrElse(Account.dummy)

  private def findPartnerAccountId(suppliers: List[BusinessPartner], partnerId: String, accounts: List[Account]): IO[RepositoryError, String] =
    for
      partner <- ZIO.getOrFailWith(RepositoryError(s"Partner $partnerId not found"))(suppliers.find(_.id == partnerId))
      account <- findObjectById(accounts, partner.account)
    yield account.id

  // --- core business logic ---------------------------------------------------
  def buildPacId(period: Int, accountId: (String, String)): List[String] =
    List(PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  def buildPacId2(period: Int, accountId: (String, String)): (String, String) =
    (PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  def buildPacsFromTransaction(model: Transaction,
                                articles: List[Article],
                                accounts: List[Account],
                                oaccountId: String
                              ): IO[RepositoryError, List[PeriodicAccountBalance]] =
    for
      pacs <- ZIO.foreach(model.lines)(line => createPac(model, line, accounts, articles, oaccountId))
      grouped = pacs.flatten.groupBy(_.id).view.mapValues(common.reduce(_, PeriodicAccountBalance.dummy)).values.toList
    yield grouped

  def postFinancials(models: List[FinancialsTransaction],
                      financialsService: FinancialsService
                    ): ZIO[Any, RepositoryError, List[(FinancialsTransaction, List[PeriodicAccountBalance], UIO[List[PeriodicAccountBalance]], List[Journal])]] =
    ZIO.foreach(models)(model => postOneFinancials(model, financialsService))

  def postOneFinancials(model: FinancialsTransaction,
                         financialsService: FinancialsService
                       ): ZIO[Any, RepositoryError, (FinancialsTransaction, List[PeriodicAccountBalance], UIO[List[PeriodicAccountBalance]], List[Journal])] =
    financialsService.postNewFinancialsTransaction(model)
  
  def buildTransaction(model: Transaction,
                        articles: List[Article],
                        accounts: List[Account],
                        suppliers: List[BusinessPartner],
                        vats: List[Vat],
                        oaccountId: String,
                        modelid: Int
                      ): ZIO[Any, RepositoryError, (Transaction, FinancialsTransaction)] =
    for
      partnerAccountId <- findPartnerAccountId(suppliers, model.account, accounts)
      firstLine        <- ZIO.getOrFailWith(RepositoryError("Transaction has no lines"))(model.lines.headOption)
      currency          = firstLine.currency
      vatDetails      <- ZIO.foreach(model.lines) { line =>
        def buildDetails (accounts:List[Account], accountId:String, oaccountId:String, vat:Vat) =
          for {
            account <- findObjectById(accounts, accountId)
            oaccount <- findObjectById(accounts, oaccountId)
            debitAcc = account.id
            creditAcc = oaccount.id
            vatAmount = line.quantity.multiply(line.price).multiply(vat.percent)
          } yield FinancialsTransactionDetails(-1, 0, debitAcc, side = true, creditAcc, vatAmount, Instant.now(),
            line.text, currency, model.company, account.name, oaccount.name, modelid)    
        for
          vat <- findObjectById(vats, line.vatCode)
          detail <- model.modelid match
            case SUPPLIER_INVOICE.modelid => buildDetails(accounts, vat.inputVatAccount, partnerAccountId, vat)
            case CUSTOMER_INVOICE.modelid => buildDetails(accounts, partnerAccountId, vat.outputVatAccount, vat)
            case _ => ZIO.succeed(FinancialsTransactionDetails.dummy)
        yield detail
      }
      vatDetailsFiltered = vatDetails.filterNot(_.account == FinancialsTransactionDetails.dummy.account)
      netDetails <- ZIO.foreach(model.lines) { line =>
        val netAmount = line.quantity.multiply(line.price)
        model.modelid match
          case CONSUMPTION.modelid =>
            for
              article <- findObjectById[Article](articles, line.article)
              account <- findObjectById(accounts, model.account)
              oaccount <- articleId2AccountIO(line.article, articles, accounts, flag = false)
              debitAcc = account.id
              creditAcc = oaccount.id
              amount    = line.quantity.multiply(article.avgPrice)
            yield FinancialsTransactionDetails(-1, 0, debitAcc, side = true, creditAcc, amount, Instant.now()
              , line.text, currency, model.company, account.name, oaccount.name, modelid)
          case GOODRECEIVING.modelid =>
            for
              account <- articleId2AccountIO(line.article, articles, accounts, flag = true)
              oacc    <- findObjectById(accounts, oaccountId)
              debitAcc = account.id
              creditAcc = oacc.id
            yield FinancialsTransactionDetails(-1, 0, debitAcc, side = true, creditAcc, netAmount, Instant.now()
              , model.text, currency, model.company, account.name, oacc.name, modelid)
          case BILL_OF_DELIVERY.modelid =>
            for
              article <- findObjectById[Article](articles, line.article)
              account    <- findObjectById(accounts, oaccountId)
              oaccount    <- findObjectById(accounts, article.revenueAccount)
              //account <- articleId2AccountIO(line.article, articles, accounts, flag = false)
              //oaccount<- articleId2AccountIO(line.re, articles, accounts, flag = true)
              debitAcc = account.id
              creditAcc = oaccount.id
              amount    = line.quantity.multiply(line.price)
            yield FinancialsTransactionDetails(-1, 0, debitAcc, side = true, creditAcc, amount , Instant.now()
              , model.text, currency, model.company, account.name, oaccount.name, modelid)
          case TransactionModelId.SUPPLIER_INVOICE.modelid =>
            for
              account   <- findObjectById(accounts, oaccountId)
              oaccount  <- findObjectById(accounts, partnerAccountId)
              debitAcc = account.id
              creditAcc = partnerAccountId
            yield FinancialsTransactionDetails(-1, 0, debitAcc, side = true, creditAcc, netAmount, Instant.now()
              , model.text, currency, model.company, account.name, oaccount.name, modelid)
          case CUSTOMER_INVOICE.modelid =>
            for
              acc   <- findObjectById(accounts, partnerAccountId)
              oacc  <- findObjectById(accounts, oaccountId)
              debitAcc = partnerAccountId
              creditAcc = oaccountId
            yield FinancialsTransactionDetails(-1, 0, debitAcc, side = true, creditAcc, netAmount, Instant.now()
              , model.text, currency, model.company, acc.name, oacc.name, modelid)
          case _ => ZIO.succeed(FinancialsTransactionDetails.dummy)
      }.map( list =>list.groupBy(line => (line.account, line.oaccount)).map {
           case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)
      }.toList)

      netDetailsFiltered = netDetails.filterNot(_.account == FinancialsTransactionDetails.dummy.account)
      combinedDetails = (netDetailsFiltered ++ vatDetailsFiltered)
        .groupBy(d => (d.account, d.oaccount))
        .view
        .mapValues(common.reduce(_, FinancialsTransactionDetails.dummy))
        .values
        .toList

      financials = FinancialsTransaction(
        -1, model.id, 0, model.store, partnerAccountId, model.transdate,
        Instant.now(), Instant.now(), model.period, posted = false, modelid,
        model.company, model.text, -1, -1, combinedDetails
      )
    yield (model.copy(posted = true), financials.copy(posted = true))

  // --- periodic account balance helpers ---------------------------------------
  def groupById(r: List[PeriodicAccountBalance]): List[PeriodicAccountBalance] =
    (r.groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}).toList

  def updatePac(oldPacs: List[PeriodicAccountBalance], tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] =
    for
        newRecords <- groupById(oldPacs).map(TPeriodicAccountBalance.apply).flip
        _ <- newRecords.map(pac => transfer(pac, tpacs)).flip
    yield tpacs


  def articleId2AccountIO( articleId: String,
                           articles: List[Article],
                           accounts: List[Account],
                           flag: Boolean): IO[RepositoryError, Account] =
    for
      article <- findObjectById(articles, articleId)
      accountId = if flag then article.account else article.oaccount
      account <- findObjectById(accounts, accountId)
    yield account

  // --- private helpers --------------------------------------------------------
  private def createPac(
                         model: Transaction,
                         line: TransactionDetails,
                         accounts: List[Account],
                         articles: List[Article],
                         oaccountId: String
                       ): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    for
      article <- findObjectById(articles, line.article)
      result <- model.modelid match
        case TransactionModelId.BILL_OF_DELIVERY.modelid =>
          for
            account  <- findObjectById(accounts, oaccountId)
            oaccount <- findObjectById(accounts, article.account)
          yield List(
            mkPac(model, line, account),
            mkPac(model, line, oaccount, isCredit = true)
          )
        case _ => // GOODRECEIVING and others
          for
            account  <- findObjectById(accounts, article.account)
            oaccount <- findObjectById(accounts, oaccountId)
          yield List(
            mkPac(model, line, account),
            mkPac(model, line, oaccount, isCredit = true),
          )
    yield result

  private def mkPac( model:Transaction,
                     line: TransactionDetails,
                     account:Account,
                     isCredit: Boolean = false
                   ): PeriodicAccountBalance =
    val (debit, credit) = if isCredit then (zeroAmount, line.quantity.multiply(line.price))
                          else (line.quantity.multiply(line.price), zeroAmount)
    PeriodicAccountBalance(
      id = PeriodicAccountBalance.createId(model.period, account.id), account = account.id, period = model.period
      , idebit = zeroAmount, icredit = zeroAmount, debit = debit, credit = credit, bdebit = debit, bcredit = credit,
      currency = line.currency, company = model.company, name = account.name, modelid = ModelId.PERIODIC_ACCOUNT_BALANCE.modelid)


  private def transfer(pac: TPeriodicAccountBalance, tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id == pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  // --- transaction log --------------------------------------------------------
  def buildTransactionLog(
                           models: List[Transaction],
                           stocks: List[Stock],
                           newStock: List[Stock],
                           articles: List[Article]
                         ): ZIO[Any, RepositoryError, List[TransactionLog]] =
    val allStock = stocks ++ newStock
    ZIO.foreach(models) { tr =>
      ZIO.foreach(tr.lines) { line =>
        for
          stock <- ZIO.getOrFailWith(RepositoryError(s"Stock not found for store ${tr.store}, article ${line.article}"))(
            allStock.find(_.id == tr.store.concat(line.article).concat(tr.company).concat(""))
          )
          article <- findObjectById(articles, stock.article)
        yield TransactionLog(
          0L, tr.id, tr.id1, tr.oid, tr.store, tr.account, line.article, line.quantity,
          stock.quantity, zeroAmount, article.quantityUnit, line.price, article.avgPrice,
          article.currency, line.duedate, line.text, tr.transdate, tr.postingdate, tr.enterdate,
          tr.period, tr.company, tr.modelid
        )
      }
    }.map(_.flatten)

  // --- assumed constants (replace with actual from domain) -------------------
  private val zeroAmount: BigDecimal = BigDecimal(0)

end PostLogisticalTransaction