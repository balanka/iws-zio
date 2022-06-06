package com.kabasoft.iws.api

import zio._
import zio.json._
import com.kabasoft.iws.domain._

object Protocol {
  final case class Orders(orders: Chunk[Order])

  final case class Customers(customers: Chunk[Customer])

  final case class CustomerWrapper(customers: List[CustomerWithOrderDate])

  final case class BankStatements(bankStatements: Chunk[BankStatement])
  final case class Datas(data: Chunk[Daten])
  final case class BaseDatas(baseData: Chunk[BaseData])

  final case class Accounts(accounts: Chunk[Account])
  final case class CustomerCountWrapper(
    customers: List[CustomerWithOrderNumber]
  )
  implicit val accountEncoder: JsonEncoder[Account] = DeriveJsonEncoder.gen[Account]
  implicit val accountDecoder: JsonDecoder[Account] = DeriveJsonDecoder.gen[Account]

  implicit val baseDataEncoder: JsonEncoder[BaseData] = DeriveJsonEncoder.gen[BaseData]
  implicit val baseDataDecoder: JsonDecoder[BaseData] = DeriveJsonDecoder.gen[BaseData]

  implicit val datenEncoder: JsonEncoder[Daten] = DeriveJsonEncoder.gen[Daten]
  implicit val datenDecoder: JsonDecoder[Daten] = DeriveJsonDecoder.gen[Daten]



  implicit val vatEncoder: JsonEncoder[Vat] = DeriveJsonEncoder.gen[Vat]
  implicit val vatDecoder: JsonDecoder[Vat] = DeriveJsonDecoder.gen[Vat]

  implicit val bankEncoder: JsonEncoder[Bank] = DeriveJsonEncoder.gen[Bank]
  implicit val bankDecoder: JsonDecoder[Bank] = DeriveJsonDecoder.gen[Bank]

  implicit val bankStatementEncoder: JsonEncoder[BankStatement] = DeriveJsonEncoder.gen[BankStatement]
  implicit val bankStatementDecoder: JsonDecoder[BankStatement] = DeriveJsonDecoder.gen[BankStatement]

  implicit val pacEncoder: JsonEncoder[PeriodicAccountBalance] = DeriveJsonEncoder.gen[PeriodicAccountBalance]
  implicit val pacDecoder: JsonDecoder[PeriodicAccountBalance] = DeriveJsonDecoder.gen[PeriodicAccountBalance]

  implicit val customerEncoder: JsonEncoder[Customer] = DeriveJsonEncoder.gen[Customer]
  implicit val customerDecoder: JsonDecoder[Customer] = DeriveJsonDecoder.gen[Customer]

  implicit val financialsEncoder: JsonEncoder[DerivedTransaction] = DeriveJsonEncoder.gen[DerivedTransaction]
  implicit val financialsDecoder: JsonDecoder[DerivedTransaction] = DeriveJsonDecoder.gen[DerivedTransaction]

  implicit val orderEncoder: JsonEncoder[Order] = DeriveJsonEncoder.gen[Order]
  implicit val orderDecoder: JsonDecoder[Order] = DeriveJsonDecoder.gen[Order]

  implicit val ordersEncoder: JsonEncoder[Orders] =
    DeriveJsonEncoder.gen[Orders]

  implicit val bankStatementsEncoder: JsonEncoder[BankStatements] =
    DeriveJsonEncoder.gen[BankStatements]

  implicit val orderDetailEncoder: JsonEncoder[OrderDetail] =
    DeriveJsonEncoder.gen[OrderDetail]

  implicit val customerWithOrderDateEncoder: JsonEncoder[CustomerWithOrderDate] =
    DeriveJsonEncoder.gen[CustomerWithOrderDate]

  implicit val customerWrapperEncoder: JsonEncoder[CustomerWrapper] =
    DeriveJsonEncoder.gen[CustomerWrapper]

  implicit val customerWithOrderNumberEncoder: JsonEncoder[CustomerWithOrderNumber] =
    DeriveJsonEncoder.gen[CustomerWithOrderNumber]

  implicit val customerCountWrapperEncoder: JsonEncoder[CustomerCountWrapper] =
    DeriveJsonEncoder.gen[CustomerCountWrapper]

  implicit val customersEncoder: JsonEncoder[Customers] =
    DeriveJsonEncoder.gen[Customers]
}
