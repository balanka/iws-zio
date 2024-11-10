package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.repository.*
import zio._


final class PostSalesOrderLive(repository4PostingTransaction: PostTransactionRepository)
                                     extends PostSalesOrder:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int] =
    if (transactions.isEmpty) ZIO.succeed(0) else
    for {
      nr <- repository4PostingTransaction.post(transactions, Nil, ZIO.succeed(List.empty[PeriodicAccountBalance]),
        Nil, Nil, Nil, Nil, Nil)
    } yield nr

object PostSalesOrderLive:
  val live: ZLayer[PostTransactionRepository, RepositoryError, PostSalesOrder] =
    ZLayer.fromFunction(new PostSalesOrderLive(_))


