package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Stock, Store, Transaction, TransactionDetails}
import zio.json._

object Protocol2 {
  implicit val transactionDetailsCodec: JsonCodec[TransactionDetails] = DeriveJsonCodec.gen[TransactionDetails]
  implicit val transactionCodec: JsonCodec[Transaction] = DeriveJsonCodec.gen[Transaction]
  implicit val stockCodec: JsonCodec[Stock] = DeriveJsonCodec.gen[Stock]
  implicit val storeCodec: JsonCodec[Store] = DeriveJsonCodec.gen[Store]

}
