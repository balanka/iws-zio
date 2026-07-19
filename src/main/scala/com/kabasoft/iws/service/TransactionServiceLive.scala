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
                                  ) extends TransactionService:

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- trRepo.find4Period(fromPeriod, toPeriod, false, company)
      nr <- postAll(models.map(m=> (m.id, m.modelid)), company)
    } yield nr

  override def postAll(ids:List[(Long, Int)], companyId: String): ZIO[Any, RepositoryError, Int] =
    if (ids.isEmpty) throw IllegalStateException(" Error: Empty transaction ids may not be posted!!!")
    for {
      queries <- ZIO.foreach(ids)(p => trRepo.getById((p._1, p._2, companyId))).map(_.filter(m => !m.posted))
      company <- companyRepository.getById((companyId, ModelId.COMPANY.modelid))
      models = queries.map(tr=>tr.copy(posted = true))
      _<- ZIO.logInfo(s"Posting transactions  ${models}")
      requisition = models.filter(_.modelid == PURCHASE_REQUISITION.modelid)
      rqf = models.filter(_.modelid == RQF.modelid)
      purchaseQuotation = models.filter(_.modelid == PURCHASE_QUOTATION.modelid)
      purchaseContract = models.filter(_.modelid == PURCHASE_CONTRACT.modelid)
      salesQuotation = models.filter(_.modelid == SALES_QUOTATION.modelid)
      salesContract = models.filter(_.modelid == SALES_CONTRACT.modelid)
      salesOrder = models.filter(_.modelid == SALES_ORDER.modelid)
      goodreceiving = models.filter(_.modelid == GOODRECEIVING.modelid)
      bilOfDelivery = models.filter(_.modelid == BILL_OF_DELIVERY.modelid)
      purchaseOrder = models.filter(_.modelid == PURCHASE_ORDER.modelid)
      supplierInvoice = models.filter(_.modelid == SUPPLIER_INVOICE.modelid)
      customerInvoice = models.filter(_.modelid == CUSTOMER_INVOICE.modelid)
      stocktransfer = models.filter(_.modelid == STOCK_TRANSFER.modelid)
      consumption = models.filter(_.modelid == CONSUMPTION.modelid)
      stocktake = models.filter(_.modelid == STOCK_TAKE.modelid)
      _<- ZIO.logInfo(s"Posting stocktransfer transactions  ${consumption}")
      postedRqf <- ZIO.when(salesOrder.nonEmpty)(salesOrderService.postAll(rqf, company))
      postedPurchaseQuotation <- ZIO.when(salesOrder.nonEmpty)(salesOrderService.postAll(purchaseQuotation, company))
      postedPurchaseContract <- ZIO.when(salesOrder.nonEmpty)(salesOrderService.postAll(purchaseContract, company))
      postedRequisition <- ZIO.when(salesOrder.nonEmpty)(salesOrderService.postAll(requisition, company))
      postedSalesQuotation <- ZIO.when(requisition.nonEmpty)(orderService.postAll(salesQuotation, company))
      postedSalesContract <- ZIO.when(requisition.nonEmpty)(orderService.postAll(salesContract, company))
      postedOrder <- ZIO.when(requisition.nonEmpty)(orderService.postAll(purchaseOrder, company))
      postedSalesOrder <- ZIO.when(salesOrder.nonEmpty)( salesOrderService.postAll(salesOrder, company))
      postedGoodreceiving <- ZIO.when(goodreceiving.nonEmpty)(postGoodreceiving.postAll(goodreceiving, company))
      postedBillOfDelivery <- ZIO.when(bilOfDelivery.nonEmpty)(postBillOfDelivery.postAll(bilOfDelivery, company))
      postedSupplierInvoice <- ZIO.when(supplierInvoice.nonEmpty)(postSupplierInvoice.postAll(supplierInvoice, company))
      postedCustomerInvoice <- ZIO.when(customerInvoice.nonEmpty)(postCustomerInvoice.postAll(customerInvoice, company))
      postedStocktransfer <- ZIO.when(stocktransfer.nonEmpty)(postStocktransferService.postAll(stocktransfer, company))
      postedStocktake <- ZIO.when(stocktake.nonEmpty)(postStocktakeService.postAll(stocktake, company))
      postedConsumption <- ZIO.when(consumption.nonEmpty)(postConsumptionService.postAll(consumption, company))
    } yield postedOrder.getOrElse(0)+ postedSalesOrder.getOrElse(0)+postedGoodreceiving.getOrElse(0)
    + postedBillOfDelivery.getOrElse(0)+postedSupplierInvoice.getOrElse(0)+postedCustomerInvoice.getOrElse(0)
    + postedStocktransfer.getOrElse(0)+ postedConsumption.getOrElse(0)+postedStocktake.getOrElse(0)
    + postedRqf.getOrElse(0) +postedRequisition.getOrElse(0)+postedPurchaseContract.getOrElse(0)+postedPurchaseQuotation.getOrElse(0)
    + postedSalesQuotation.getOrElse(0)+ postedSalesContract.getOrElse(0)
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
     PostGoodreceiving&  PostBillOfDelivery&  PostCustomerInvoice& PostSupplierInvoice &PostStockTransfer& PostStocktake& PostConsumption&
    JournalRepository&  ArticleRepository&  StockRepository&  PostTransactionRepository &CompanyRepository, RepositoryError, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceLive(_, _, _, _, _, _, _,_, _, _, _))

