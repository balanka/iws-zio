package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._


final class PostOrderImpl(repository4PostingTransaction: PostTransactionRepository)
                                     extends PostOrder {
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int] =
    for {
      nr <- repository4PostingTransaction.post(transactions, Nil, ZIO.succeed(List.empty[PeriodicAccountBalance]),
        Nil, Nil, Nil, Nil, Nil)
    } yield nr



}
object PostOrderImpl {
  val live: ZLayer[PostTransactionRepository, RepositoryError, PostOrder] =
    ZLayer.fromFunction(new PostOrderImpl(_))
}

