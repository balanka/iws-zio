package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._


final class PostOrderLive(repository4PostingTransaction: PostTransactionRepository) extends PostOrder:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int] =
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
      nr <- repository4PostingTransaction.post(transactions, Nil, Nil, Nil, Nil, Nil)
    yield nr
  
object PostOrderLive:
  val live: ZLayer[PostTransactionRepository, RepositoryError, PostOrder] =
    ZLayer.fromFunction(new PostOrderLive(_))

