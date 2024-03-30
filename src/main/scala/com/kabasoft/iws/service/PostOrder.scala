package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.repository._
import zio._


final class PostOrder( ftrRepo: TransactionRepository, repository4PostingTransaction: PostTransactionRepository)
                                     extends TransactionService {

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- ftrRepo
        .find4Period(fromPeriod, toPeriod, company)
        .filter(_.posted == false)
        .runCollect
      nr <- ZIO.foreach(models)(trans => postTransaction(List(trans))).map(_.toList).map(_.sum)
    } yield nr

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId((id, company)))
      models = queries.filter(_.posted == false)
      _ <- ZIO.foreachDiscard(models.map(_.id))(
        id => ZIO.logInfo(s"Posting transaction with id ${id} of company ${company}"))
      nr <- postTransaction(models)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}") *>
      ftrRepo
        .getByTransId((id, company))
        .flatMap(trans => postTransaction(List(trans)))

  private[this] def postTransaction(transactions: List[Transaction]): ZIO[Any, RepositoryError, Int] = for {
    post <- repository4PostingTransaction.post(transactions, Nil, ZIO.succeed(List.empty[PeriodicAccountBalance]),
      Nil, Nil, Nil, Nil, Nil)
  } yield post


}


