package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.repository.*
import zio._


final class PostOrderLive(repository4PostingTransaction: PostTransactionRepository) extends PostOrder:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int] =
    if (transactions.isEmpty) ZIO.succeed(0) else
    for {
      nr <- repository4PostingTransaction.post(transactions, Nil, ZIO.succeed(List.empty[PeriodicAccountBalance]),
        Nil, Nil, Nil, Nil, Nil)
    } yield nr
  
object PostOrderLive:
  val live: ZLayer[PostTransactionRepository, RepositoryError, PostOrder] =
    ZLayer.fromFunction(new PostOrderLive(_))

