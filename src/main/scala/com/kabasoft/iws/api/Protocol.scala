package com.kabasoft.iws.api

import zio._
import zio.json._
import com.kabasoft.iws.domain._

object Protocol {
  final case class Orders(orders: Chunk[Order])

  final case class Customers(customers: Chunk[Customer])

  final case class BankAccounts(bankAccounts: List[BankAccount])

  final case class CustomerWrapper(customers: List[CustomerWithOrderDate])

  final case class BankStatements(bankStatements: Chunk[BankStatement])
  final case class Banks(banks: List[Bank])
  final case class Datas(data: List[Data])
  final case class BaseDatas(baseData: List[BaseData])

  final case class Accounts(accounts: Set[Account])

  final case class Journals(journals: Chunk[Journal])
  final case class CustomerCountWrapper(customers: List[CustomerWithOrderNumber])

  implicit val moduleDecoder: JsonDecoder[Module] = DeriveJsonDecoder.gen[Module]
  implicit val moduleEncoder: JsonEncoder[Module] = DeriveJsonEncoder.gen[Module]

  implicit val bankAccountEncoder: JsonEncoder[BankAccount]   = DeriveJsonEncoder.gen[BankAccount]
  implicit val bankAccountDecoder: JsonDecoder[BankAccount]   = DeriveJsonDecoder.gen[BankAccount]
  implicit val bankAccountsEncoder: JsonEncoder[BankAccounts] = DeriveJsonEncoder.gen[BankAccounts]
  implicit val bankAccountsDecoder: JsonDecoder[BankAccounts] = DeriveJsonDecoder.gen[BankAccounts]
  implicit val supplierEncoder: JsonEncoder[Supplier]         = DeriveJsonEncoder.gen[Supplier]
  implicit val supplierDecoder: JsonDecoder[Supplier]         = DeriveJsonDecoder.gen[Supplier]

  implicit lazy val accountEncoder: JsonEncoder[Account] = DeriveJsonEncoder.gen[Account]
  implicit lazy val accountDecoder: JsonDecoder[Account] = DeriveJsonDecoder.gen[Account]
  implicit lazy val accountsEncoder: JsonEncoder[Accounts] = DeriveJsonEncoder.gen[Accounts]
  implicit lazy val accountsDecoder: JsonDecoder[Accounts] = DeriveJsonDecoder.gen[Accounts]

  implicit val userEncoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  implicit val userDecoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  implicit val roleEncoder: JsonEncoder[Role] = DeriveJsonEncoder.gen[Role]
  implicit val roleDecoder: JsonDecoder[Role] = DeriveJsonDecoder.gen[Role]

  implicit val loginRequestEncoder: JsonEncoder[LoginRequest] = DeriveJsonEncoder.gen[LoginRequest]
  implicit val loginRequestDecoder: JsonDecoder[LoginRequest] = DeriveJsonDecoder.gen[LoginRequest]

  implicit val vatEncoder: JsonEncoder[Vat] = DeriveJsonEncoder.gen[Vat]
  implicit val vatDecoder: JsonDecoder[Vat] = DeriveJsonDecoder.gen[Vat]

  implicit val bankEncoder: JsonEncoder[Bank] = DeriveJsonEncoder.gen[Bank]
  implicit val bankDecoder: JsonDecoder[Bank] = DeriveJsonDecoder.gen[Bank]

  implicit val banksEncoder: JsonEncoder[Banks] = DeriveJsonEncoder.gen[Banks]
  implicit val banksDecoder: JsonDecoder[Banks] = DeriveJsonDecoder.gen[Banks]

  implicit val bankStatementEncoder: JsonEncoder[BankStatement] = DeriveJsonEncoder.gen[BankStatement]
  implicit val bankStatementDecoder: JsonDecoder[BankStatement] = DeriveJsonDecoder.gen[BankStatement]

  implicit val pacEncoder: JsonEncoder[PeriodicAccountBalance] = DeriveJsonEncoder.gen[PeriodicAccountBalance]
  implicit val pacDecoder: JsonDecoder[PeriodicAccountBalance] = DeriveJsonDecoder.gen[PeriodicAccountBalance]

  implicit val companyEncoder: JsonEncoder[Company] = DeriveJsonEncoder.gen[Company]
  implicit val companyDecoder: JsonDecoder[Company] = DeriveJsonDecoder.gen[Company]
  implicit val costcenterEncoder: JsonEncoder[Costcenter] = DeriveJsonEncoder.gen[Costcenter]
  implicit val costcenterDecoder: JsonDecoder[Costcenter] = DeriveJsonDecoder.gen[Costcenter]

  implicit val customerEncoder: JsonEncoder[Customer] = DeriveJsonEncoder.gen[Customer]
  implicit val customerDecoder: JsonDecoder[Customer] = DeriveJsonDecoder.gen[Customer]
  implicit val oldcustomerEncoder: JsonEncoder[Customer_OLD] = DeriveJsonEncoder.gen[Customer_OLD]
  implicit val oldcustomerDecoder: JsonDecoder[Customer_OLD] = DeriveJsonDecoder.gen[Customer_OLD]

  implicit val financialsDerivedEncoder: JsonEncoder[DerivedTransaction] = DeriveJsonEncoder.gen[DerivedTransaction]
  implicit val financialsDerivedDecoder: JsonDecoder[DerivedTransaction] = DeriveJsonDecoder.gen[DerivedTransaction]

  implicit val financialsDetailsEncoder: JsonEncoder[FinancialsTransactionDetails] = DeriveJsonEncoder.gen[FinancialsTransactionDetails]
  implicit val financialsDetailsDecoder: JsonDecoder[FinancialsTransactionDetails] = DeriveJsonDecoder.gen[FinancialsTransactionDetails]

  implicit val financialsEncoder: JsonEncoder[FinancialsTransaction] = DeriveJsonEncoder.gen[FinancialsTransaction]
  implicit val financialsDecoder: JsonDecoder[FinancialsTransaction] = DeriveJsonDecoder.gen[FinancialsTransaction]

  implicit val journalEncoder: JsonEncoder[Journal] = DeriveJsonEncoder.gen[Journal]
  implicit val journalDecoder: JsonDecoder[Journal] = DeriveJsonDecoder.gen[Journal]

  implicit val orderEncoder: JsonEncoder[Order] = DeriveJsonEncoder.gen[Order]
  implicit val orderDecoder: JsonDecoder[Order] = DeriveJsonDecoder.gen[Order]

  implicit val ordersEncoder: JsonEncoder[Orders] = DeriveJsonEncoder.gen[Orders]

  implicit val bankStatementsEncoder: JsonEncoder[BankStatements] = DeriveJsonEncoder.gen[BankStatements]

  implicit val orderDetailEncoder: JsonEncoder[OrderDetail] = DeriveJsonEncoder.gen[OrderDetail]

  implicit val customerWithOrderDateEncoder: JsonEncoder[CustomerWithOrderDate] = DeriveJsonEncoder.gen[CustomerWithOrderDate]

  implicit val customerWrapperEncoder: JsonEncoder[CustomerWrapper] =
    DeriveJsonEncoder.gen[CustomerWrapper]

  implicit val customerWithOrderNumberEncoder: JsonEncoder[CustomerWithOrderNumber] =
    DeriveJsonEncoder.gen[CustomerWithOrderNumber]

  implicit val customerCountWrapperEncoder: JsonEncoder[CustomerCountWrapper] =
    DeriveJsonEncoder.gen[CustomerCountWrapper]

}
