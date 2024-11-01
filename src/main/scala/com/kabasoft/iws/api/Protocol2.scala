package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Article, Bom, Stock, Store, Transaction, TransactionDetails}
import zio.json._

object Protocol2 {
  implicit lazy val articleCodec: JsonCodec[Article] = DeriveJsonCodec.gen[Article]
  implicit lazy val bomCodec: JsonCodec[Bom] = DeriveJsonCodec.gen[Bom]
  implicit val transactionDetailsCodec: JsonCodec[TransactionDetails] = DeriveJsonCodec.gen[TransactionDetails]
  implicit val transactionCodec: JsonCodec[Transaction] = DeriveJsonCodec.gen[Transaction]
  implicit val stockCodec: JsonCodec[Stock] = DeriveJsonCodec.gen[Stock]
  implicit val storeCodec: JsonCodec[Store] = DeriveJsonCodec.gen[Store]

}
