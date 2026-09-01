package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.domain.ModelId.{BILL_OF_DELIVERY, CONSUMPTION, CUSTOMER_INVOICE, GOODRECEIVING, PURCHASE_CONTRACT
  , PURCHASE_QUOTATION, PURCHASE_ORDER, PURCHASE_REQUISITION, RQF, SALES_CONTRACT, SALES_ORDER, SALES_QUOTATION
  , STOCK_TAKE, STOCK_TRANSFER, SUPPLIER_INVOICE}
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, CompanyRepository, JournalRepository, PacRepository, PostTransactionRepository, StockRepository, TransactionLogRepository, TransactionRepository}
import zio.*

final class TransactionServiceLive(trRepo: TransactionRepository
                                   , orderService:PostOrder
                                   , salesOrderService: PostSalesOrder
                                   , postGoodreceiving: PostGoodreceiving
                                   , postStocktransferService: PostStockTransfer
                                   , postConsumptionService: PostConsumption
                                   , postStocktakeService: PostStocktake
                                   , postBillOfDelivery: PostBillOfDelivery
                                   , postSupplierInvoice: PostSupplierInvoice
                                   , postCustomerInvoice: PostCustomerInvoice
                                   , companyRepository: CompanyRepository
                                   , articleRepository:ArticleRepository
                                  ) extends TransactionService:

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- trRepo.find4Period(fromPeriod, toPeriod, false, company)
      nr <- postAll(models.map(m=> (m.id, m.modelid)), company)
    } yield nr


  override def postAll(
                        ids: List[(Long, Int)],
                        companyId: String
                      ): ZIO[Any, RepositoryError, Int] =
    for {
      // Validate input: fail with RepositoryError if ids is empty
      _ <- if (ids.isEmpty)
        ZIO.fail(RepositoryError("Error: Empty transaction ids may not be posted!!!"))
      else ZIO.unit
      articles <- articleRepository.all(ModelId.ARTICLE.modelid, companyId)

      // Fetch only non‑posted transactions
      base <- ZIO.foreach(ids)(p => trRepo.getById((p._1, p._2, companyId)))
        .map(_.filter(!_.posted))

      _ <- ZIO.logInfo(s"Posting base transactions: $base")

      // Split into services (non‑stocked articles) and inventory (stocked)
      services = base.map { tr =>tr.copy(lines =tr.lines.filter(l => articles.exists(art => art.id == l.article && !art.stocked)))}
      inventory = base.map { tr =>tr.copy(lines =tr.lines.filter(l => articles.exists(art => art.id == l.article && art.stocked)))}

      _ <- ZIO.logInfo(s"Posting services transactions: $services")
      _ <- ZIO.logInfo(s"Posting inventory transactions: $inventory")
      company <- companyRepository.getById((companyId, ModelId.COMPANY.modelid))
      // Mark inventory transactions as posted
      models = inventory.map(_.copy(posted = true))
      // Group models by modelid for efficient processing
      grouped = models.groupBy(_.modelid)
      // Process each group via pattern matching
      postedInventories <- ZIO.foreach(grouped.toList) { case (modelId, transactions) =>
        modelId match {
          case PURCHASE_REQUISITION.modelid => salesOrderService.postAll(transactions, company)
          case RQF.modelid => salesOrderService.postAll(transactions, company)
          case PURCHASE_QUOTATION.modelid => salesOrderService.postAll(transactions, company)
          case PURCHASE_CONTRACT.modelid => salesOrderService.postAll(transactions, company)
          case SALES_QUOTATION.modelid => orderService.postAll(transactions, company)
          case SALES_CONTRACT.modelid => orderService.postAll(transactions, company)
          case SALES_ORDER.modelid => salesOrderService.postAll(transactions, company)
          case GOODRECEIVING.modelid => postGoodreceiving.postAll(transactions, company)
          case BILL_OF_DELIVERY.modelid => postBillOfDelivery.postAll(transactions, company)
          case PURCHASE_ORDER.modelid => orderService.postAll(transactions, company)
          case SUPPLIER_INVOICE.modelid => postSupplierInvoice.postAll(transactions, company)
          case CUSTOMER_INVOICE.modelid => postCustomerInvoice.postAll(transactions, company)
          case STOCK_TRANSFER.modelid => postStocktransferService.postAll(transactions, company)
          case CONSUMPTION.modelid => postConsumptionService.postAll(transactions, company)
          case STOCK_TAKE.modelid => postStocktakeService.postAll(transactions, company)
          case _ => ZIO.succeed(0) // ignore unknown types
        }
      }

      // Post services (non‑inventory transactions) separately
      postedServices <- if (services.nonEmpty) postConsumptionService.postAll(services, company)
      else ZIO.succeed(0)

      // Sum all results
      total = postedInventories.sum + postedServices

    } yield total
  override def post(id: (Long, Int), company: String): ZIO[Any, RepositoryError, Int] = postAll(List(id), company)

  override def copyFrom(id: Long, modelidFrom: Int, modelidTo: Int, companyId: String): ZIO[Any, RepositoryError, Transaction] =
    for {
      company <- companyRepository.getById(companyId, ModelId.COMPANY.modelid)
      //fmodule <- fmoduleRepo.getById(modelidTo, Fmodule.MODEL_ID, companyId)
      //account <- accRepo.getById(fmodule.account, Account.MODELID, companyId)
      _ <- ZIO.logInfo(s" Company with id = $companyId ${company}")
      trans <- trRepo.getById(id, modelidFrom, companyId)
      _ <- ZIO.logInfo(s"Copying the transaction ${trans} to a new one with modelid $modelidTo ")
      transaction = (trans.modelid, modelidTo) match {
        case (PURCHASE_ORDER.modelid, GOODRECEIVING.modelid) => Copy2Self.copy(trans,  modelidTo, company)
        case (GOODRECEIVING.modelid, SUPPLIER_INVOICE.modelid) => Copy2Self.copy(trans,  modelidTo, company)
        case (SALES_ORDER.modelid, BILL_OF_DELIVERY.modelid) => Copy2Self.copy(trans,  modelidTo, company)
        case (BILL_OF_DELIVERY.modelid, CUSTOMER_INVOICE.modelid) => Copy2Self.copy(trans,  modelidTo, company)
        case _ => Copy2Self.copy(trans,  modelidTo, company)
      }
       _ <- ZIO.logInfo(s"newly Created financials transaction  from one with id = $id ${transaction}")
      trans2 <- trRepo.create(transaction)
    } yield trans2



object TransactionServiceLive:
  val live: ZLayer[PacRepository& TransactionRepository& TransactionLogRepository& AccountRepository& PostOrder& PostSalesOrder&
     PostGoodreceiving&  PostBillOfDelivery&  PostCustomerInvoice& PostSupplierInvoice &PostStockTransfer& PostStocktake&
    PostConsumption& JournalRepository&  ArticleRepository&  StockRepository&  PostTransactionRepository
    &CompanyRepository &ArticleRepository, RepositoryError, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceLive(_, _, _, _, _, _, _,_, _, _, _, _))

