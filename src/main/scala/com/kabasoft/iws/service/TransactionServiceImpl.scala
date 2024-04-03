package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, CompanyRepository, JournalRepository,
  PacRepository, PostTransactionRepository, StockRepository, TransactionLogRepository, TransactionRepository}
import zio._

final class TransactionServiceImpl( ftrRepo: TransactionRepository
                                    , orderService:PostOrder
                                    , postGoodreceiving: PostGoodreceiving
                                    , postBillOfDelivery: PostBillOfDelivery
                                    , companyRepository: CompanyRepository
                                  ) extends TransactionService  {

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- ftrRepo
        .find4Period(fromPeriod, toPeriod, company)
        .filter(_.posted == false)
        .runCollect
      nr <- postAll(models.map(_.id).toList, company)
    } yield nr

  override def postAll(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, Int] = for {
    queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId((id, companyId))).map(_.filter(m => !m.posted))
    company <- companyRepository.getBy(companyId)
    models = queries.filter(_.posted == false).map(_.copy(posted = true))
    goodreceiving = models.filter(_.modelid == TransactionModelId.GOORECEIVING.id)
    bilOfDelivery = models.filter(_.modelid == TransactionModelId.BILL_OF_DELIVERY.id)
    purchaseOrder = models.filter(_.modelid == TransactionModelId.PURCHASE_ORDER.id)
    postedGoodreceiving <- postGoodreceiving.postAll(goodreceiving, company)
    postedBillOfDelivery <- postBillOfDelivery.postAll(bilOfDelivery, company)
    postedOrder <- orderService.postAll(purchaseOrder, company)
    } yield postedGoodreceiving+ postedBillOfDelivery+postedOrder

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] = postAll(List(id), company)

}

object TransactionServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with TransactionLogRepository with AccountRepository with PostOrder with PostGoodreceiving
    with PostBillOfDelivery  with CompanyRepository
    with JournalRepository with ArticleRepository with StockRepository with PostTransactionRepository, RepositoryError, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceImpl(_, _, _, _, _))

}
