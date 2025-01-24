package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{given, *}
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, CompanyRepository, JournalRepository,
  PacRepository, PostTransactionRepository, StockRepository, TransactionLogRepository, TransactionRepository}
import zio._
import zio.prelude.DebugOps

final class TransactionServiceLive(trRepo: TransactionRepository
                                   , orderService:PostOrder
                                   , salesOrderService: PostSalesOrder
                                   , postGoodreceiving: PostGoodreceiving
                                   , postBillOfDelivery: PostBillOfDelivery
                                   , postSupplierInvoice: PostSupplierInvoice
                                   , postCustomerInvoice: PostCustomerInvoice
                                   , companyRepository: CompanyRepository
                                  ) extends TransactionService:

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- trRepo.find4Period(fromPeriod, toPeriod, false, company)
      nr <- postAll(models.map(m=> (m.id1, m.modelid)), company)
    } yield nr

  override def postAll(ids:List[(Long, Int)], companyId: String): ZIO[Any, RepositoryError, Int] =
    if (ids.isEmpty) throw IllegalStateException(" Error: Empty transaction ids may not be posted!!!")
    for {
    queries <- ZIO.foreach(ids)(p => trRepo.getById((p._1, p._2, companyId))).map(_.filter(m => !m.posted))
    company <- companyRepository.getById((companyId, Company.MODEL_ID))
    models = queries.filter(_.posted == false).map(_.copy(posted = true))
    goodreceiving = models.filter(_.modelid == TransactionModelId.GOORECEIVING.id)
    bilOfDelivery = models.filter(_.modelid == TransactionModelId.BILL_OF_DELIVERY.id)
    purchaseOrder = models.filter(_.modelid == TransactionModelId.PURCHASE_ORDER.id)
    salesOrder = models.filter(_.modelid == TransactionModelId.SALES_ORDER.id)
    supplierInvoice = models.filter(_.modelid == TransactionModelId.SUPPLIER_INVOICE.id)
    customerInvoice = models.filter(_.modelid == TransactionModelId.CUSTOMER_INVOICE.id)
    postedOrder <- ZIO.when(!purchaseOrder.isEmpty)(orderService.postAll(purchaseOrder, company))
    postedSalesOrder <- ZIO.when(!salesOrder.isEmpty)( salesOrderService.postAll(salesOrder, company))
    postedGoodreceiving <- ZIO.when(!goodreceiving.isEmpty)(postGoodreceiving.postAll(goodreceiving, company))
    postedBillOfDelivery <- ZIO.when(!bilOfDelivery.isEmpty)(postBillOfDelivery.postAll(bilOfDelivery, company))
    postedSupplierInvoice <- ZIO.when(!supplierInvoice.isEmpty)(postSupplierInvoice.postAll(supplierInvoice, company))
    postedCustomerInvoice <- ZIO.when(!customerInvoice.isEmpty)(postCustomerInvoice.postAll(customerInvoice, company))
    } yield postedOrder.getOrElse(0)+ postedSalesOrder.getOrElse(0)+postedGoodreceiving.getOrElse(0)
    + postedBillOfDelivery.getOrElse(0)+postedSupplierInvoice.getOrElse(0)+postedCustomerInvoice.getOrElse(0)

  override def post(id: (Long, Int), company: String): ZIO[Any, RepositoryError, Int] = postAll(List(id), company)



object TransactionServiceLive:
  val live: ZLayer[PacRepository& TransactionRepository& TransactionLogRepository& AccountRepository& PostOrder& PostSalesOrder&
     PostGoodreceiving&  PostBillOfDelivery&  PostCustomerInvoice& PostSupplierInvoice&CompanyRepository&
    JournalRepository&  ArticleRepository&  StockRepository&  PostTransactionRepository, RepositoryError, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceLive(_, _, _, _, _, _, _,_))

