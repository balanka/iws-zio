package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{given, *}
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, CompanyRepository, JournalRepository,
  PacRepository, PostTransactionRepository, StockRepository, TransactionLogRepository, TransactionRepository}
import zio._

final class TransactionServiceLive(ftrRepo: TransactionRepository
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
      models <- ftrRepo.find4Period(fromPeriod, toPeriod, false, company)
      nr <- postAll(models.map(m=> (m.id, m.modelid)), company)
    } yield nr

  override def postAll(ids:List[(Long, Int)], companyId: String): ZIO[Any, RepositoryError, Int] = for {
    queries <- ZIO.foreach(ids)(p => ftrRepo.getById((p._1, p._2, companyId))).map(_.filter(m => !m.posted))
    company <- companyRepository.getById((companyId, Company.MODEL_ID))
    models = queries.filter(_.posted == false).map(_.copy(posted = true))
    goodreceiving = models.filter(_.modelid == TransactionModelId.GOORECEIVING.id)
    bilOfDelivery = models.filter(_.modelid == TransactionModelId.BILL_OF_DELIVERY.id)
    purchaseOrder = models.filter(_.modelid == TransactionModelId.PURCHASE_ORDER.id)
    salesOrder = models.filter(_.modelid == TransactionModelId.SALES_ORDER.id)
    supplierInvoice = models.filter(_.modelid == TransactionModelId.SUPPLIER_INVOICE.id)
    customerInvoice = models.filter(_.modelid == TransactionModelId.CUSTOMER_INVOICE.id)
    postedOrder <- orderService.postAll(purchaseOrder, company)
    postedSalesOrder <- salesOrderService.postAll(salesOrder, company)
    postedGoodreceiving <- postGoodreceiving.postAll(goodreceiving, company)
    postedBillOfDelivery <- postBillOfDelivery.postAll(bilOfDelivery, company)
    postedSupplierInvoice <- postSupplierInvoice.postAll(supplierInvoice, company)
    postedCustomerInvoice <- postCustomerInvoice.postAll(customerInvoice, company)
    } yield postedOrder + postedSalesOrder+postedGoodreceiving + postedBillOfDelivery+postedSupplierInvoice+postedCustomerInvoice

  override def post(id: (Long, Int), company: String): ZIO[Any, RepositoryError, Int] = postAll(List(id), company)



object TransactionServiceLive:
  val live: ZLayer[PacRepository& TransactionRepository& TransactionLogRepository& AccountRepository& PostOrder& PostSalesOrder&
     PostGoodreceiving&  PostBillOfDelivery&  PostCustomerInvoice& PostSupplierInvoice&CompanyRepository&
    JournalRepository&  ArticleRepository&  StockRepository&  PostTransactionRepository, RepositoryError, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceLive(_, _, _, _, _, _, _,_))

