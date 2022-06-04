package com.kabasoft.iws.domain

import java.util.{ Locale, UUID }
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneId }
import zio.prelude._
import zio.prelude.NonEmptyList
import zio.stm._
import zio._

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import scala.collection.Seq
import scala.collection.immutable.{ ::, Nil }

object common {

  type Amount = scala.math.BigDecimal
  def getMonthAsString(month: Int): String       =
    if (month <= 9) {
      "0".concat(month.toString)
    } else month.toString
  def getYear(instant: Instant)                  = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+1")).getYear
  def getMonthAsString(instant: Instant): String =
    getMonthAsString(LocalDateTime.ofInstant(instant, ZoneId.of("UTC+1")).getMonth.getValue)
  def getPeriod(instant: Instant)                = {
    val year = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+1")).getYear
    year.toString.concat(getMonthAsString(instant)).toInt
  }
}

final case class Account(
  id: String,
  name: String,
  description: String,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  company: String,
  modelid: Int = 9,
  account: String = "",
  isDebit: Boolean,
  balancesheet: Boolean,
  currency: String = "EUR ",
  idebit: BigDecimal = BigDecimal(0),
  icredit: BigDecimal = BigDecimal(0),
  debit: BigDecimal = BigDecimal(0),
  credit: BigDecimal = BigDecimal(0),
  subAccounts: Set[Account] = Nil.toSet
  // parent: Option[Account] = None
) /*extends IWS*/ {
  def debiting(amount: BigDecimal) = copy(debit = debit.+(amount))

  def crediting(amount: BigDecimal) = copy(credit = credit.+(amount))

  def idebiting(amount: BigDecimal) = copy(idebit = idebit.+(amount))

  def icrediting(amount: BigDecimal) = copy(icredit = icredit.+(amount))

  def fdebit = debit + idebit

  def fcredit = credit + icredit

  def dbalance = fdebit - fcredit

  def cbalance = fcredit - fdebit

  def balance = if (isDebit) dbalance else cbalance

  def add(acc: Account): Account =
    copy(subAccounts = subAccounts + acc);

  def addAll(accSet: Set[Account]) =
    copy(subAccounts = subAccounts ++ accSet)

  def updateBalance(acc: Account): Account =
    idebiting(acc.idebit).icrediting(acc.icredit).debiting(acc.debit).crediting(acc.credit)

  def childBalances: BigDecimal = // subAccounts.toList.reduceLeft((x,y) => x + y).balance
    if (subAccounts.size > 0) {
      subAccounts.toList match {
        case Nil     => BigDecimal.apply(0)
        case x :: xs => NonEmptyList.fromIterable(x, xs).reduce.balance

      }
    } else {
      BigDecimal.apply(0)
    }

  def getChildren(): Set[Account] = subAccounts.toList match {
    case Nil     => Set(copy(id = id))
    case x :: xs => Set(x) ++ xs.flatMap(_.getChildren())
  }
}
object Account {
  type Account_Type = (
    String,
    String,
    String,
    Instant,
    Instant,
    Instant,
    String,
    Int,
    String,
    Boolean,
    Boolean,
    String,
    BigDecimal,
    BigDecimal,
    BigDecimal,
    BigDecimal
  )
/*
  def apply(  id: String,
              name: String,
              description: String,
              enterdate: Instant = Instant.now(),
              changedate: Instant = Instant.now(),
              postingdate: Instant = Instant.now(),
              company: String,
              modelid: Int = 9,
              account: String = "",
              isDebit: Boolean,
              balancesheet: Boolean,
              currency: String = "EUR ",
              idebit: BigDecimal = BigDecimal(0),
              icredit: BigDecimal = BigDecimal(0),
              debit: BigDecimal = BigDecimal(0),
              credit: BigDecimal = BigDecimal(0)):Account = Account(id, name, description, enterdate, changedate, postingdate,
    company, modelid, account ,isDebit, balancesheet, currency, idebit, icredit, debit, credit,Nil.toSet)
    
 */

  def apply(acc: Account_Type): Account             =
     new Account(
      acc._1,
      acc._2,
      acc._3,
      acc._4,
      acc._5,
      acc._6,
      acc._7,
      acc._8,
      acc._9,
      acc._10,
      acc._11,
      acc._12,
      acc._13,
      acc._14,
      acc._15,
      acc._16,
      Nil.toSet
    )
  val dummy                                         = Account("", "", "", Instant.now(), Instant.now(), Instant.now()
    , "1000", 9, "", false, false, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)
  def group(accounts: List[Account]): List[Account] =
    accounts
      .groupBy(_.id)
      .map ({ case (k, v: List[Account]) =>
        v match {
          case Nil       => dummy
          case (x :: xs) => NonEmptyList.fromIterable(x, xs).reduce.copy(id = k)
        }
      })
      .toList

  def removeSubAccounts(account: Account): Account                                                        =
    account.subAccounts.toList match {
      case Nil      => account
      case rest @ _ =>
        val sub = account.subAccounts.filterNot((acc => acc.balance == 0 && acc.subAccounts.size == 0))
        if (account.subAccounts.size > 0)
          account.copy(subAccounts = sub.map(removeSubAccounts))
        else account
    }
  def addSubAccounts2(account: Account, accMap: Map[String, List[Account]]): Account                      =
    accMap.get(account.id) match {
      case Some(acc) =>
        val x = account.subAccounts ++ acc.map(x => addSubAccounts(x, accMap))
        account.copy(subAccounts = x)
      case None      =>
        if (account.subAccounts.size > 0)
          account.copy(
            subAccounts = account.subAccounts ++ account.subAccounts.toList.map(x => addSubAccounts(x, accMap))
          )
        else account
    }
  def addSubAccounts(account: Account, accMap: Map[String, List[Account]]): Account                       =
    accMap.get(account.id) match {
      case Some(accList) =>
        // account.copy(subAccounts = account.subAccounts ++ (group(accList).map(x => addSubAccounts(x, accMap))))
        account.copy(subAccounts = account.subAccounts ++ accList.map(x => addSubAccounts(x, accMap)))
      case None          =>
        if (account.subAccounts.size > 0)
          account.copy(
            subAccounts = account.subAccounts ++ account.subAccounts.toList.map(x => addSubAccounts(x, accMap))
          )
        else account
    }
  def getInitialDebitCredit(accId: String, pacs: List[PeriodicAccountBalance], side: Boolean): BigDecimal =
    pacs.find(x => x.account == accId) match {
      case Some(acc) => if (side) acc.idebit else acc.icredit
      case None      => BigDecimal(0)
    }
  def getAllSubBalances(account: Account, pacs: List[PeriodicAccountBalance]): Account                    =
    account.subAccounts.toList match {
      case Nil      =>
        account.copy(
          idebit = getInitialDebitCredit(account.id, pacs, true),
          icredit = getInitialDebitCredit(account.id, pacs, false)
        )
      case rest @ _ =>
        val sub    = account.subAccounts.map(acc => getAllSubBalances(acc, pacs))
        val subALl = sub.toList match {
          case Nil     => dummy
          case x :: xs => NonEmptyList.fromIterable(x, xs).reduce
        }
        account
          .idebiting(subALl.idebit)
          .icrediting(subALl.icredit)
          .debiting(subALl.debit)
          .crediting(subALl.credit)
          .copy(subAccounts = sub)
    }

  def unwrapDataTailRec(account: Account): List[Account] = {
    // @tailrec
    def unwrapData(res: List[Account]): List[Account] =
      res.flatMap(acc =>
        acc.subAccounts.toList match {
          case Nil                     => if (acc.balance == 0.0 && acc.subAccounts.isEmpty) List.empty[Account] else List(acc)
          case (head: Account) :: tail => List(acc, head) ++ unwrapData(tail)
        }
      )
    unwrapData(account.subAccounts.toList)
  }

  def wrapAsData(account: Account): Data =
    account.subAccounts.toList match {
      case Nil      => Data(BaseData(account))
      case rest @ _ =>
        Data(BaseData(account)).copy(children = account.subAccounts.toList.map(wrapAsData))
    }

  def consolidateData(acc: Account): Data =
    List(acc).map(wrapAsData) match {
      case Nil          => Data(BaseData(Account.dummy))
      case account :: _ => account
    }

  def consolidate(accId: String, accList: List[Account], pacs: List[PeriodicAccountBalance]): Account = {
    val accMap = accList.groupBy(_.account)
    accList.find(x => x.id == accId) match {
      case Some(acc) =>
        val x: Account = addSubAccounts(acc, accMap) // List(acc)
        val y          = getAllSubBalances(x, pacs)
        val z          = removeSubAccounts(y.copy(id = acc.id))
        z
      case None      => Account.dummy
    }
  }

  def withChildren(accId: String, accList: List[Account]): Account =
    accList.find(x => x.id == accId) match {
      case Some(acc) => List(acc).foldMap(addSubAccounts2(_, accList.groupBy(_.account))).copy(id = accId)
      case None      => Account.dummy
    }

  def flattenTailRec(ls: Set[Account]): Set[Account] = {
    // @tailrec
    def flattenR(res: List[Account], rem: List[Account]): List[Account] = rem match {
      case Nil                     => res
      case (head: Account) :: tail => flattenR(res ++ List(head), head.subAccounts.toList ++ tail)
    }
    flattenR(List.empty[Account], ls.toList).toSet
  }

  implicit def reduce[A: Identity](as: NonEmptyList[A]): A = as.reduce
  type Balance_Type = (BigDecimal, BigDecimal, BigDecimal, BigDecimal)

  implicit val accMonoid: Identity[Account] = new Identity[Account] {
    def identity: Account                       = dummy
    def combine(m1: => Account, m2: => Account) =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
  }

  implicit val balanceMonoid: Identity[Balance_Type] = new Identity[Balance_Type] {
    def identity: Balance_Type                            = (BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0))
    def combine(m1: => Balance_Type, m2: => Balance_Type) =
      new Balance_Type(m1._1 + m2._1, m1._2 + m2._2, m1._3 + m2._3, m1._4 + m2._4)
  }

}
final case class BaseData(
  id: String,
  name: String,
  description: String,
  modelId: Int = 19,
  isDebit: Boolean,
  balancesheet: Boolean,
  idebit: BigDecimal,
  icredit: BigDecimal,
  debit: BigDecimal,
  credit: BigDecimal,
  currency: String,
  company: String
) {
  def debiting(amount: BigDecimal)   = copy(debit = debit.+(amount))
  def crediting(amount: BigDecimal)  = copy(credit = credit.+(amount))
  def idebiting(amount: BigDecimal)  = copy(idebit = idebit.+(amount))
  def icrediting(amount: BigDecimal) = copy(icredit = icredit.+(amount))
  def fdebit                         = debit + idebit
  def fcredit                        = credit + icredit
  def dbalance                       = fdebit - fcredit
  def cbalance                       = fcredit - fdebit
  def balance                        = if (isDebit) dbalance else cbalance

}
object BaseData {
  def apply(acc: Account): BaseData =
    BaseData(
      acc.id,
      acc.name,
      acc.description,
      19,
      acc.isDebit,
      acc.balancesheet,
      acc.idebit,
      acc.icredit,
      acc.debit,
      acc.credit,
      acc.currency,
      acc.company
    )
}
final case class Data(data: BaseData, children: Seq[Data] = Nil)
final case class Order(
  id: UUID,
  customerId: UUID,
  date: LocalDate
)

final case class Customer(
  id: UUID,
  fname: String,
  lname: String,
  verified: Boolean,
  dateOfBirth: LocalDate
)

final case class Product(
  id: UUID,
  name: String,
  description: String,
  imageUrl: String
)

final case class ProductPrice(
  id: UUID,
  effective: LocalDate,
  price: Double
)

final case class OrderDetail(
  orderId: UUID,
  productId: UUID,
  quantity: Int,
  unitPrice: Double
)

final case class CustomerWithOrderDate(
  firstName: String,
  lastName: String,
  orderDate: LocalDate
  // cid: UUID,
)

final case class CustomerWithOrderNumber(
  firstName: String,
  lastName: String,
  count: Long
)

sealed trait AppError extends Throwable

object AppError      {
  final case class RepositoryError(cause: Throwable) extends AppError
  final case class DecodingError(message: String)    extends AppError
}
sealed trait IWS     {
  def id: String
}
final case class Bank(
  id: String,
  name: String = "",
  description: String = "",
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  modelid: Int = 11,
  company: String
)
final case class BankAccount(id: String, bic: String, owner: String, company: String, modelid: Int = 12)
final case class BankStatement(
  id: Long,
  depositor: String,
  postingdate: Instant,
  valuedate: Instant,
  postingtext: String,
  purpose: String,
  beneficiary: String,
  accountno: String,
  bankCode: String,
  amount: BigDecimal,
  currency: String,
  info: String,
  company: String,
  companyIban: String,
  posted: Boolean = false,
  modelid: Int = 18
)
object BankStatement {
  val CENTURY         = "20"
  val COMPANY         = "1000"
  val COMPANY_IBAN    = "DE47480501610043006329"
  val zoneId          = ZoneId.of("Europe/Berlin")
  val DATE_FORMAT     = "dd.MM.yyyy"
  val FIELD_SEPARATOR = ';'
  val NUMBER_FORMAT   = NumberFormat.getInstance(Locale.GERMAN)

  type BS_Type = (
    Long,
    String,
    Instant,
    Instant,
    String,
    String,
    String,
    String,
    String,
    BigDecimal,
    String,
    String,
    String,
    String,
    Boolean,
    Int
  )

  type BS_Type2 = (
    String,
    Instant,
    Instant,
    String,
    String,
    String,
    String,
    String,
    BigDecimal,
    String,
    String,
    String,
    String,
    Boolean,
    Int
  )

  def apply(bs: BS_Type): BankStatement = BankStatement(
    bs._1,
    bs._2,
    bs._3,
    bs._4,
    bs._5,
    bs._6,
    bs._7,
    bs._8,
    bs._9,
    bs._10,
    bs._11,
    bs._12,
    bs._13,
    bs._14,
    bs._15,
    bs._16
  )

  def apply2(bs: BS_Type2): BankStatement    = BankStatement(
    -1L,
    bs._1,
    bs._2,
    bs._3,
    bs._4,
    bs._5,
    bs._6,
    bs._7,
    bs._8,
    bs._9,
    bs._10,
    bs._11,
    bs._12,
    bs._13,
    bs._14,
    bs._15
  )
  def fullDate(partialDate: String): Instant = {
    val index    = partialDate.lastIndexOf(".")
    val pYear    = partialDate.substring(index + 1)
    val fullDate = partialDate.substring(0, index + 1).concat(CENTURY.concat(pYear))
    LocalDate
      .parse(fullDate, DateTimeFormatter.ofPattern(DATE_FORMAT))
      .atStartOfDay(zoneId)
      .toInstant
  }

  def from(s: String) = {
    val values      = s.split(FIELD_SEPARATOR)
    val companyIban = values(0)
    val bid         = -1L
    val date1_      = values(1)
    val date2       = values(2)
    val date1       = if (date1_.trim.nonEmpty) date1_ else date2
    val postingdate = fullDate(date1)
    val valuedate   = fullDate(date2)
    // val depositor = values(3)
    val postingtext = values(3)
    val purpose     = values(4)
    val beneficiary = values(5)
    val accountno   = values(6)
    val bankCode    = values(7)
    val amount_     = values(8).trim
    val amount      = BigDecimal(NUMBER_FORMAT.parse(amount_).toString)
    val currency    = values(9)
    val info        = values(10)
    val bs          = BankStatement(
      bid,
      companyIban,
      postingdate,
      valuedate,
      postingtext,
      purpose,
      beneficiary,
      accountno,
      bankCode,
      amount,
      currency,
      info,
      COMPANY,
      companyIban
    )
    // println ("BankStatement>>"+bs)
    bs
  }

}

final case class Vat(
  id: String,
  name: String = "",
  description: String = "",
  percent: BigDecimal,
  inputVatAccount: String,
  outputVatAccount: String,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  company: String,
  modelid: Int = 6
)

final case class TPeriodicAccountBalance(
  id: String,
  account: String,
  period: Int,
  idebit: TRef[BigDecimal],
  icredit: TRef[BigDecimal],
  debit: TRef[BigDecimal],
  credit: TRef[BigDecimal],
  currency: String,
  company: String,
  modelid: Int = PeriodicAccountBalance.MODELID
) {
  self =>
  def debiting(amount: BigDecimal) = STM.atomically {
    self.debit.get.flatMap(balance => self.debit.set(balance + amount))
  }

  def crediting(amount: BigDecimal) = STM.atomically {
    self.credit.get.flatMap(balance => self.credit.set(balance + amount))
  }

  def idebiting(amount: BigDecimal) = STM.atomically {
    self.idebit.get.flatMap(balance => self.idebit.set(balance + amount))
  }

  def icrediting(amount: BigDecimal) = STM.atomically {
    self.icredit.get.flatMap(balance => self.icredit.set(balance + amount))
  }

  def fdebit = for {
    debitx  <- self.debit.get.commit
    idebitx <- self.idebit.get.commit
  } yield (debitx + idebitx)

  def fcredit = for {
    creditx  <- self.credit.get.commit
    icreditx <- self.icredit.get.commit
  } yield (creditx + icreditx)

  def dbalance = for {
    fdebitx  <- self.fdebit
    fcreditx <- self.fcredit
  } yield (fdebitx - fcreditx)

  def cbalance = for {
    fdebitx  <- self.fdebit
    fcreditx <- self.fcredit
  } yield (fcreditx - fdebitx)

  def transfer(to: TPeriodicAccountBalance, amount: BigDecimal): UIO[Unit] =
    STM.atomically {
      to.debit.update(_ + amount).*>(self.credit.update(_ + amount))
    }
}
final case class PeriodicAccountBalance(
  id: String,
  account: String,
  period: Int,
  idebit: BigDecimal,
  icredit: BigDecimal,
  debit: BigDecimal,
  credit: BigDecimal,
  currency: String,
  company: String,
  modelid: Int = PeriodicAccountBalance.MODELID
) {
  def debiting(amount: BigDecimal)   = copy(debit = debit.+(amount))
  def crediting(amount: BigDecimal)  = copy(credit = credit.+(amount))
  def idebiting(amount: BigDecimal)  = copy(idebit = idebit.+(amount))
  def icrediting(amount: BigDecimal) = copy(icredit = icredit.+(amount))
  def fdebit                         = debit + idebit
  def fcredit                        = credit + icredit
  def dbalance                       = fdebit - fcredit
  def cbalance                       = fcredit - fdebit

}

object PeriodicAccountBalance {
  import zio.prelude._
  import zio.NonEmptyChunk

  val MODELID = 106

  def init(paccs: List[PeriodicAccountBalance]) =
    paccs.foreach(
      _.copy(idebit = BigDecimal(0), debit = BigDecimal(0), icredit = BigDecimal(0), credit = BigDecimal(0))
    )
  def createId(period: Int, accountId: String)  = period.toString.concat(accountId)
  val empty                                     =
    PeriodicAccountBalance("", "", 0, BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), "EUR", "1000")

  implicit val pacMonoid: Associative[PeriodicAccountBalance]  =
    new Associative[PeriodicAccountBalance] {
      def combine(m1: => PeriodicAccountBalance, m2: => PeriodicAccountBalance) =
        m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
    }
  implicit def reduce[A: Associative](as: NonEmptyChunk[A]): A = as.reduce(_ <> _)
  println(reduce(NonEmptyChunk(empty, empty)))
}

sealed trait BusinessPartner        {
  def id: String
  def name: String
  def description: String
  def street: String
  def city: String
  def state: String
  def zip: String
  def country: String
  def phone: String
  def email: String
  def account: String
  def oaccount: String
  def iban: String
  def vatcode: String
  def company: String
  def modelid: Int
  def enterdate: Instant
  def changedate: Instant
  def postingdate: Instant
  def bankaccounts: List[BankAccount]
}
final case class Supplier(
  id: String,
  name: String,
  description: String,
  street: String,
  city: String,
  state: String,
  zip: String,
  country: String,
  phone: String,
  email: String,
  account: String,
  oaccount: String,
  iban: String,
  vatcode: String,
  company: String,
  modelid: Int = 1,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  bankaccounts: List[BankAccount] = Nil
) extends BusinessPartner
final case class customerX(
  id: String,
  name: String,
  description: String,
  street: String,
  city: String,
  state: String,
  zip: String,
  country: String,
  phone: String,
  email: String,
  account: String,
  oaccount: String,
  iban: String,
  vatcode: String,
  company: String,
  modelid: Int = 3,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  bankaccounts: List[BankAccount] = Nil
) extends BusinessPartner

final case class FinancialsTransactionDetails(
  lid: Long,
  transid: Long,
  account: String,
  side: Boolean,
  oaccount: String,
  amount: BigDecimal,
  duedate: Instant = Instant.now(),
  text: String,
  currency: String
) {
  def id = lid.toString

}
final case class FinancialsTransaction(
  tid: Long,
  oid: Long,
  costcenter: String,
  account: String,
  transdate: Instant = Instant.now(),
  enterdate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  period: Int = common.getPeriod(Instant.now()),
  posted: Boolean = false,
  modelid: Int,
  company: String,
  text: String = "",
  typeJournal: Int = 0,
  file_content: Int = 0,
  lines: List[FinancialsTransactionDetails] = Nil
  // ,copyFlag: Boolean = false
) {
  def id            = tid.toString
  def month: String = common.getMonthAsString(transdate)
  def year: Int     = common.getYear(transdate)
  def getPeriod     = common.getPeriod(transdate)
  def total         = lines.reduce((l1, l2) => l2.copy(amount = l2.amount + l1.amount))

}
object FinancialsTransactionDetails {
  import FinancialsTransaction.FinancialsTransaction_Type2
  val EMPTY                                                   = FinancialsTransactionDetails(0, 0, "", true, "", BigDecimal(0), Instant.now(), "", "EUR")
  implicit val monoid: Identity[FinancialsTransactionDetails] =
    new Identity[FinancialsTransactionDetails] {
      def identity                                                                          = EMPTY
      def combine(m1: => FinancialsTransactionDetails, m2: => FinancialsTransactionDetails) =
        m2.copy(amount = m2.amount.+(m1.amount))
    }

  type FinancialsTransactionDetails_Type = (Long, Long, String, Boolean, String, BigDecimal, Instant, String, String)
  type FTX2                              = FinancialsTransaction_Type2
  def apply(tr: FinancialsTransactionDetails_Type): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9)
  def apply(tr: FinancialsTransactionDetails.FTX2): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(tr._15, tr._1, tr._16, tr._17, tr._18, tr._19, tr._20, tr._21, tr._22)

  def apply(x: DerivedTransaction): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(
      x.lid,
      x.id,
      x.account,
      x.side,
      x.oaccount,
      x.amount,
      x.transdate,
      x.terms,
      x.currency
    )
}
object FinancialsTransaction        {
  type FinancialsTransaction_Type  =
    (Long, Long, String, String, Instant, Instant, Instant, Int, Boolean, Int, String, String, Int, Int)
  type FinancialsTransaction_Type2 = (
    Long,
    Long,
    String,
    String,
    Instant,
    Instant,
    Instant,
    Int,
    Boolean,
    Int,
    String,
    String,
    Int,
    Int,
    Long,
    String,
    Boolean,
    String,
    BigDecimal,
    Instant,
    String,
    String
  )

  def applyC(tr: FinancialsTransaction_Type): FinancialsTransaction =
    FinancialsTransaction(
      tr._1,
      tr._2,
      tr._3,
      tr._4,
      tr._5,
      tr._6,
      tr._7,
      tr._8,
      tr._9,
      tr._10,
      tr._11,
      tr._12,
      tr._13,
      tr._14,
      Nil
    )

  def applyL(transactions: List[FinancialsTransactionDetails.FTX2]): List[FinancialsTransaction] =
    transactions
      .groupBy(rc =>
        (rc._1, rc._2, rc._3, rc._4, rc._5, rc._6, rc._7, rc._8, rc._9, rc._10, rc._11, rc._12, rc._13, rc._14)
      )
      .map { case (k, v) =>
        new FinancialsTransaction(
          k._1,
          k._2,
          k._3,
          k._4,
          k._5,
          k._6,
          k._7,
          k._8,
          k._9,
          k._10,
          k._11,
          k._12,
          k._13,
          k._14
        )
          .copy(lines = v.filter(p => p._15 != -1).map(FinancialsTransactionDetails.apply))
      }
      .toList

  def apply(x: FinancialsTransactionDetails.FTX2) =
    new FinancialsTransaction(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._11, x._12, x._13, x._14)
      .copy(lines = List(FinancialsTransactionDetails.apply(x)))

  def apply1(x: DerivedTransaction) =
    new FinancialsTransaction(
      x.id,
      x.oid,
      x.oaccount,
      x.account,
      x.transdate,
      x.enterdate,
      x.postingdate,
      x.period,
      x.posted,
      x.modelid,
      x.company,
      x.text,
      x.file,
      0
    )
      .copy(lines = List(FinancialsTransactionDetails.apply(x)))
}
final case class DerivedTransaction(
  id: Long,
  oid: Long,
  account: String,
  transdate: Instant = Instant.now(),
  enterdate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  period: Int = common.getPeriod(Instant.now()),
  posted: Boolean = false,
  modelid: Int,
  company: String,
  text: String = "",
  file: Int = 0,
  lid: Long,
  side: Boolean,
  oaccount: String,
  amount: BigDecimal,
  currency: String,
  terms: String = ""
)
