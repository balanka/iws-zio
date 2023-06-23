package com.kabasoft.iws.domain

import java.util.Locale
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import zio.prelude._
import zio.stm._
import zio.{UIO, _}

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import scala.collection.immutable.{::, Nil}
import scala.annotation.tailrec
import java.math.{BigDecimal, RoundingMode}
import com.kabasoft.iws.domain.FinancialsTransaction.DerivedTransaction_Type

final case class Company(
  id: String,
  name: String,
  street: String,
  zip: String,
  city: String,
  state: String,
  country: String,
  email: String,
  partner: String,
  phone: String,
  bankAcc: String,
  iban: String,
  taxCode: String,
  vatCode: String,
  currency: String,
  locale: String,
  balanceSheetAcc: String,
  incomeStmtAcc: String,
  modelid: Int
)
sealed trait AppError extends Throwable

object AppError {
  final case class RepositoryError(message: String) extends AppError
  final case class DecodingError(message: String)    extends AppError
}

object common {

  val zeroAmount   = BigDecimal.valueOf(0, 2)
  val dummyBalance = Balance("dummy", zeroAmount, zeroAmount, zeroAmount, zeroAmount)

  val DummyUser = User(-1, "dummy", "dummy", "dummy", "dummyhash2", "dummyphone", "dummy@user.com", "dummy", "dummymenu", "0000", 111)

  def reduce[A: Identity](all: Iterable[A], dummy: A): A =
    all.toList match {
      case Nil     => dummy
      case x :: xs => NonEmptyList.fromIterable(x, xs).reduce
    }

  implicit val accMonoid: Identity[Account] = new Identity[Account] {
    def identity: Account                       = Account.dummy
    def combine(m1: => Account, m2: => Account) =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
  }

  implicit val accBalanceMonoid: Identity[Balance] = new Identity[Balance] {
    def identity: Balance = dummyBalance

    def combine(m1: => Balance, m2: => Balance) =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
  }

  implicit val pacMonoid: Identity[PeriodicAccountBalance] = new Identity[PeriodicAccountBalance] {
    def identity: PeriodicAccountBalance                                      = PeriodicAccountBalance.dummy
    def combine(m1: => PeriodicAccountBalance, m2: => PeriodicAccountBalance) =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
  }
  def getMonthAsString(month: Int): String                 =
    if (month <= 9) {
      "0".concat(month.toString)
    } else month.toString
  def getYear(instant: Instant)                            = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+1")).getYear
  def getMonthAsString(instant: Instant): String           =
    getMonthAsString(LocalDateTime.ofInstant(instant, ZoneId.of("UTC+1")).getMonth.getValue)
  def getPeriod(instant: Instant)                          = {
    val year = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+1")).getYear
    year.toString.concat(getMonthAsString(instant)).toInt
  }
}
import common._
final case class Balance(id: String, idebit: BigDecimal, icredit: BigDecimal, debit: BigDecimal, credit: BigDecimal) {
  def debiting(amount: BigDecimal) = copy(debit = debit.add(amount))

  def crediting(amount: BigDecimal) = copy(credit = credit.add(amount))

  def idebiting(amount: BigDecimal) = copy(idebit = idebit.add(amount))

  def icrediting(amount: BigDecimal) = copy(icredit = icredit.add(amount))

}

final case class Account_(
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
  idebit: BigDecimal = zeroAmount,
  icredit: BigDecimal = zeroAmount,
  debit: BigDecimal = zeroAmount,
  credit: BigDecimal = zeroAmount
)

object Account_ {
  def apply(acc: Account): Account_ = new Account_(
    acc.id,
    acc.name,
    acc.description,
    acc.enterdate,
    acc.changedate,
    acc.postingdate,
    acc.company,
    acc.modelid,
    acc.account,
    acc.isDebit,
    acc.balancesheet,
    acc.currency,
    acc.idebit,
    acc.icredit,
    acc.debit,
    acc.credit
  )

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
  idebit: BigDecimal = zeroAmount,
  icredit: BigDecimal = zeroAmount,
  debit: BigDecimal = zeroAmount,
  credit: BigDecimal = zeroAmount,
  subAccounts: Set[Account] = Nil.toSet
) {
  def debiting(amount: BigDecimal) = copy(debit = debit.add(amount))

  def crediting(amount: BigDecimal) = copy(credit = credit.add(amount))

  def idebiting(amount: BigDecimal) = copy(idebit = idebit.add(amount))

  def icrediting(amount: BigDecimal) = copy(icredit = icredit.add(amount))

  def fdebit = debit.add(idebit)

  def fcredit = credit.add(icredit)

  def dbalance = fdebit.subtract(fcredit)

  def cbalance = fcredit.subtract(fdebit)

  def balance = if (isDebit) dbalance else cbalance

  def getBalance = Balance(id, idebit, icredit, debit, credit)

  def add(acc: Account): Account =
    copy(subAccounts = subAccounts + acc);

  def remove(acc: Account): Account =
    copy(subAccounts = subAccounts.filterNot(_.id == acc.id))

  def filterAddSubAccounts(accSet: Set[Account]): Account =
    copy(subAccounts = accSet.filter(_.account == id).map(_.filterAddSubAccounts(accSet)))

  def updateBalanceP(accounts: Ref[Set[Account]]): UIO[List[Account]] = {
    val r = accounts.get
    r.flatMap(all =>
      { println("all" + all); all.find(_.id == account) } match {
        case Some(parent) =>
          val updated = parent.updateBalance(this)
          val old     = all.filterNot(_.id == id)
          accounts.set(old + updated)
          val w       = updated.updateBalanceP(accounts)
          w
        case None         => ZIO.succeed(List(this))
      }
    )
  }

  def updateBalance(acc: Account): Account =
    idebiting(acc.idebit)
      .icrediting(acc.icredit)
      .debiting(acc.debit)
      .crediting(acc.credit)
      .remove(acc).add(acc)


  @tailrec
  def updateBalanceParent(all: List[Account]): List[Account] =
    all.find(acc => acc.id == account) match {
      case Some(parent) =>
        val y: Account       = parent.updateBalance(this)
        val z: List[Account] = all.filterNot(acc => acc.id == parent.id) :+ y
        y.updateBalanceParent(z)
      case None         => all
    }

  def getChildren: Set[Account] = subAccounts.toList match {
    case Nil     => Set(copy(id = id))
    case x :: xs => Set(x) ++ xs.flatMap(_.getChildren)
  }

  def addSubAccounts(accounts: List[Account]): Account =
    copy(subAccounts = accounts.filter(_.account == id).map(_.addSubAccounts(accounts)).toSet)



}
object Account {
  import common._

  val MODELID = 9
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

  def apply(acc: Account_): Account                 = new Account(
    acc.id,
    acc.name,
    acc.description,
    acc.enterdate,
    acc.changedate,
    acc.postingdate,
    acc.company,
    acc.modelid,
    acc.account,
    acc.isDebit,
    acc.balancesheet,
    acc.currency,
    acc.idebit,
    acc.icredit,
    acc.debit,
    acc.credit,
    Set.empty[Account]
  )
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
  val dummy                                         = Account(
    "",
    "",
    "",
    Instant.now(),
    Instant.now(),
    Instant.now(),
    "1000",
    9,
    "",
    false,
    false,
    "EUR",
    zeroAmount,
    zeroAmount,
    zeroAmount,
    zeroAmount,
    Nil.toSet
  )
  def group(accounts: List[Account]): List[Account] =
    accounts
      .groupBy(_.id)
      .map { case (k, v: List[Account]) => reduce(v, Account.dummy).copy(id = k) }
      .filterNot(_.id == Account.dummy.id)
      .toList

  def removeSubAccounts(account: Account): Account =
    account.subAccounts.toList match {
      case Nil      => account
      case rest @ _ =>
        val sub = account.subAccounts.filterNot((acc => acc.balance == 0 && acc.subAccounts.isEmpty))
        if (account.subAccounts.nonEmpty)
          account.copy(subAccounts = sub.map(removeSubAccounts))
        else account
    }

  def addSubAccounts(account: Account, accMap: Map[String, List[Account]]): Account =
    accMap.get(account.id) match {
      case Some(accList) => addSubAcc(account, accMap, accList)
      case None          =>
        if (account.subAccounts.nonEmpty) {
          addSubAcc(account, accMap, account.subAccounts.toList)
        } else account
    }

  private def addSubAcc(account: Account, accMap: Map[String, List[Account]], accList: List[Account]) =
    account.copy(subAccounts = account.subAccounts ++ accList.map(x => addSubAccounts(x, accMap)))

  def getInitialDebitCredit(accId: String, pacs: List[PeriodicAccountBalance], side: Boolean): BigDecimal =
    pacs.find(x => x.account == accId) match {
      case Some(acc) => if (side) acc.idebit else acc.icredit
      case None      => zeroAmount
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
        val subALl = reduce(sub, Account.dummy)
        account
          .idebiting(subALl.idebit)
          .icrediting(subALl.icredit)
          .debiting(subALl.debit)
          .crediting(subALl.credit)
          .copy(subAccounts = sub)
    }

  def unwrapData(account: Account): List[Account] = {
    // @tailrec
    def unwrapData_(res: Set[Account]): Set[Account] =
      res.flatMap(acc =>
        acc.subAccounts.toList match {
          case Nil                     => if (acc.balance.compareTo(zeroAmount) == 0 && acc.subAccounts.isEmpty) Set.empty[Account] else Set(acc)
          // case (head: Account) :: tail => Set(acc, head).filterNot(_.balance.compareTo(zeroAmount) == 0) ++ unwrapData_(tail.toSet)
          case (head: Account) :: tail => Set(acc, head) ++ unwrapData_(tail.toSet)
        }
      )

    unwrapData_(account.subAccounts).toList
  }

  def withChildren(accId: String, accList: List[Account]): Account =
    accList.find(x => x.id == accId) match {
      case Some(acc) => List(acc).foldMap(addSubAccounts(_, accList.groupBy(_.account))).copy(id = accId)
      case None      => Account.dummy
    }

  def consolidate(accId: String, accList: List[Account], pacs: List[PeriodicAccountBalance]): Account = {
    val accMap = accList.groupBy(_.account)
    accList.find(x => x.id == accId) match {
      case Some(acc) => updateSubAccountBalance(pacs, accMap, acc)
      case None      => Account.dummy
    }
  }

  private def updateSubAccountBalance(
    pacs: List[PeriodicAccountBalance],
    accMap: Map[String, List[Account]],
    acc: Account
  ) = {
    val x: Account = addSubAccounts(acc, accMap) // List(acc)
    val y          = getAllSubBalances(x, pacs)
    val z          = removeSubAccounts(y.copy(id = acc.id))
    z
  }

  def flattenTailRec(ls: Set[Account]): Set[Account] = {
    // @tailrec
    def flattenR(res: List[Account], rem: List[Account]): List[Account] = rem match {
      case Nil                     => res
      case (head: Account) :: tail => flattenR(res ++ List(head), head.subAccounts.toList ++ tail)
    }
    flattenR(List.empty[Account], ls.toList).toSet
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
  def debiting(amount: BigDecimal)   = copy(debit = debit.add(amount))
  def crediting(amount: BigDecimal)  = copy(credit = credit.add(amount))
  def idebiting(amount: BigDecimal)  = copy(idebit = idebit.add(amount))
  def icrediting(amount: BigDecimal) = copy(icredit = icredit.add(amount))
  def fdebit                         = debit.add(idebit)
  def fcredit                        = credit.add(icredit)
  def dbalance                       = fdebit.subtract(fcredit)
  def cbalance                       = fcredit.subtract(fdebit)
  def balance                        = if (isDebit) dbalance else cbalance

}

sealed trait IWS {
  def id: String
}
final case class Costcenter(
  id: String,
  name: String = "",
  description: String = "",
  account: String = "",
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  modelid: Int = 6,
  company: String
) extends IWS
final case class Bank(
  id: String,
  name: String = "",
  description: String = "",
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  modelid: Int = 11,
  company: String
) extends IWS

final case class BankAccount(id: String, bic: String, owner: String, company: String, modelid: Int /*= 12 */ )
object BankAccount    {
  import scala.math.Ordering
  implicit def ordering[A <: BankAccount]: Ordering[A] = Ordering.by(e => (e.id, e.bic, e.owner, e.company))
}

final case class BankStatement_(
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
object BankStatement_ {
  def apply(bs: BankStatement): BankStatement_ = new BankStatement_(
    bs.depositor,
    bs.postingdate,
    bs.valuedate,
    bs.postingtext,
    bs.purpose,
    bs.beneficiary,
    bs.accountno,
    bs.bankCode,
    bs.amount,
    bs.currency,
    bs.info,
    bs.company,
    bs.companyIban,
    bs.posted,
    bs.modelid
  )
}
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
object BankStatement  {
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
    val amount      = new BigDecimal(NUMBER_FORMAT.parse(amount_).toString)
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
final case class Module(
  id: String,
  name: String = "",
  description: String = "",
  path: String = "",
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  modelid: Int = 300,
  company: String
)
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
    self.debit.get.flatMap(balance => self.debit.set(balance.add(amount)))
  }

  def crediting(amount: BigDecimal) = STM.atomically {
    self.credit.get.flatMap(balance => self.credit.set(balance.add(amount)))
  }

  def idebiting(amount: BigDecimal) = STM.atomically {
    self.idebit.get.flatMap(balance => self.idebit.set(balance.add(amount)))
  }

  def icrediting(amount: BigDecimal) = STM.atomically {
    self.icredit.get.flatMap(balance => self.icredit.set(balance.add(amount)))
  }

  def fdebit = for {
    debitx  <- self.debit.get.commit
    idebitx <- self.idebit.get.commit
  } yield (debitx.add(idebitx))

  def fcredit = for {
    creditx  <- self.credit.get.commit
    icreditx <- self.icredit.get.commit
  } yield creditx.add(icreditx)

  def dbalance = for {
    fdebitx  <- self.fdebit
    fcreditx <- self.fcredit
  } yield fdebitx.subtract(fcreditx)

  def cbalance = for {
    fdebitx  <- self.fdebit
    fcreditx <- self.fcredit
  } yield fcreditx.subtract(fdebitx)

  def transfer(to: TPeriodicAccountBalance, amount: BigDecimal): UIO[Unit] =
    STM.atomically {
      to.debit.update(_.add(amount)).*>(self.credit.update(_.add(amount)))
    }
}
object TPeriodicAccountBalance {
  def apply(pac: PeriodicAccountBalance): UIO[TPeriodicAccountBalance] = for {
    idebit  <- TRef.makeCommit(pac.idebit)
    icredit <- TRef.makeCommit(pac.icredit)
    debit   <- TRef.makeCommit(pac.debit)
    credit  <- TRef.makeCommit(pac.credit)
  } yield TPeriodicAccountBalance(pac.id, pac.account, pac.period, idebit, icredit, debit, credit, pac.currency, pac.company, pac.modelid)

  def debitAndCredit(pac: TPeriodicAccountBalance, poac: TPeriodicAccountBalance, amount: BigDecimal) = STM.atomically {
    pac.debit.get.flatMap(debit => pac.debit.set(debit.add(amount))) *>
      poac.credit.get.flatMap(credit => poac.credit.set(credit.add(amount)))

  }

  def debitAndCreditAll(pacs: List[TPeriodicAccountBalance], poacs: List[TPeriodicAccountBalance], amount: BigDecimal) =
    pacs.zip(poacs).map { case (pac, poac) => debitAndCredit(pac, poac, amount) }

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
  def debiting(amount: BigDecimal)         = copy(debit = debit.add(amount))
  def crediting(amount: BigDecimal)        = copy(credit = credit.add(amount))
  def idebiting(amount: BigDecimal)        = copy(idebit = idebit.add(amount))
  def icrediting(amount: BigDecimal)       = copy(icredit = icredit.add(amount))
  def fdebit                               = debit.add(idebit)
  def fcredit                              = credit.add(icredit)
  def dbalance                             = fdebit.subtract(fcredit)
  def cbalance                             = fcredit.subtract(fdebit)
  override def equals(other: Any): Boolean = other match {
    case pac: PeriodicAccountBalance =>
      this.id == pac.id
    case _                           => false
  }

}

object PeriodicAccountBalance {

  type PAC_Type = (String, String, Int, BigDecimal, BigDecimal, BigDecimal, BigDecimal, String, String, Int)

  val MODELID                                   = 106
  def init(paccs: List[PeriodicAccountBalance]) =
    paccs.foreach(
      _.copy(idebit = zeroAmount, debit = zeroAmount, icredit = zeroAmount, credit = zeroAmount)
    )

  def createId(period: Int, accountId: String) = period.toString.concat(accountId)
  val dummy                                    =
    PeriodicAccountBalance("", "", 0, zeroAmount, zeroAmount, zeroAmount, zeroAmount, "EUR", "1000")

  def create(accountId: String, period: Int, currency: String, company: String): PeriodicAccountBalance =
    PeriodicAccountBalance.apply(
      PeriodicAccountBalance.createId(period, accountId),
      accountId,
      period,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      company,
      currency,
      PeriodicAccountBalance.MODELID
    )

  def create(model: FinancialsTransaction): List[PeriodicAccountBalance] =
    model.lines.flatMap { line =>
      List(
        PeriodicAccountBalance.apply(
          PeriodicAccountBalance.createId(model.period, line.account),
          line.account,
          model.period,
          zeroAmount,
          zeroAmount,
          zeroAmount,
          // line.amount,
          zeroAmount,
          model.company,
          line.currency,
          PeriodicAccountBalance.MODELID
        ),
        PeriodicAccountBalance.apply(
          PeriodicAccountBalance.createId(model.period, line.oaccount),
          line.oaccount,
          model.period,
          zeroAmount,
          zeroAmount,
          zeroAmount,
          zeroAmount,
          // line.amount,
          model.company,
          line.currency,
          PeriodicAccountBalance.MODELID
        )
      )
    }
  def applyX(p: PAC_Type)                                                = PeriodicAccountBalance(p._1, p._2, p._3, p._4, p._5, p._6, p._7, p._8, p._9, p._10)

  def applyT(tpac: TPeriodicAccountBalance): ZIO[Any, Nothing, PeriodicAccountBalance] = for {
    idebit  <- tpac.debit.get.commit
    icredit <- tpac.debit.get.commit
    debit   <- tpac.debit.get.commit
    credit  <- tpac.debit.get.commit
  } yield PeriodicAccountBalance(tpac.id, tpac.account, tpac.period, idebit, icredit, credit, debit, tpac.currency, tpac.company, tpac.modelid)
}

sealed trait BusinessPartner         {
  def id: String
  def name: String
  def description: String
  def street: String
  def city: String
  def state: String
  def zip: String
  // def country: String
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
final case class Supplier_(
  id: String,
  name: String,
  description: String,
  street: String,
  zip: String,
  city: String,
  state: String,
  country: String,
  phone: String,
  email: String,
  account: String,
  oaccount: String,
  iban: String,
  vatcode: String,
  company: String,
  modelid: Int = Supplier.MODELID,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now()
)
final case class Supplier(
  id: String,
  name: String,
  description: String,
  street: String,
  zip: String,
  city: String,
  state: String,
  country: String,
  phone: String,
  email: String,
  account: String,
  oaccount: String,
  iban: String,
  vatcode: String,
  company: String,
  modelid: Int = Supplier.MODELID,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  bankaccounts: List[BankAccount] = List.empty[BankAccount]
) extends BusinessPartner
object Supplier                      {
  val MODELID = 1
  type TYPE = (
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    Int,
    Instant,
    Instant,
    Instant
  )
  def apply(c: TYPE): Supplier =
    Supplier(
      c._1,
      c._2,
      c._3,
      c._4,
      c._5,
      c._6,
      c._7,
      c._8,
      c._9,
      c._10,
      c._11,
      c._12,
      c._13,
      c._14,
      c._15,
      c._16,
      c._17,
      c._18,
      c._19,
      List.empty[BankAccount]
    )
}
final case class Customer_(
  id: String,
  name: String,
  description: String,
  street: String,
  zip: String,
  city: String,
  state: String,
  country: String,
  phone: String,
  email: String,
  account: String,
  oaccount: String,
  iban: String,
  vatcode: String,
  company: String,
  modelid: Int = Customer.MODELID,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now()
)
final case class Customer(
  id: String,
  name: String,
  description: String,
  street: String,
  zip: String,
  city: String,
  state: String,
  country: String,
  phone: String,
  email: String,
  account: String,
  oaccount: String,
  iban: String,
  vatcode: String,
  company: String,
  modelid: Int = Customer.MODELID,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  bankaccounts: List[BankAccount] = List.empty[BankAccount]
) extends BusinessPartner
object Customer                      {
  val MODELID = 3
  type TYPE = (
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    String,
    Int,
    Instant,
    Instant,
    Instant
  )

  def apply(c: TYPE): Customer =
    Customer(
      c._1,
      c._2,
      c._3,
      c._4,
      c._5,
      c._6,
      c._7,
      c._8,
      c._9,
      c._10,
      c._11,
      c._12,
      c._13,
      c._14,
      c._15,
      c._16,
      c._17,
      c._18,
      c._19,
      List.empty[BankAccount]
    )
}

final case class FinancialsTransactionDetails(
  id: Long,
  transid: Long,
  account: String,
  side: Boolean,
  oaccount: String,
  amount: BigDecimal,
  duedate: Instant = Instant.now(),
  text: String,
  currency: String
)

final case class FinancialsTransactionDetails_(
  transid: Long,
  account: String,
  side: Boolean,
  oaccount: String,
  amount: BigDecimal,
  duedate: Instant = Instant.now(),
  text: String,
  currency: String
)
object FinancialsTransactionDetails_ {
  def apply(tr: FinancialsTransactionDetails): FinancialsTransactionDetails_ =
    new FinancialsTransactionDetails_(tr.transid, tr.account, tr.side, tr.oaccount, tr.amount, tr.duedate, tr.text, tr.currency)
}
final case class FinancialsTransactionx(
  id: Long,
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
  file_content: Int = 0
)
final case class FinancialsTransaction_(
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
  file_content: Int = 0
)
object FinancialsTransaction_        {
  def apply(tr: FinancialsTransaction): FinancialsTransaction_ = new FinancialsTransaction_(
    tr.oid,
    tr.costcenter,
    tr.account,
    tr.enterdate,
    tr.transdate,
    tr.postingdate,
    tr.period,
    tr.posted,
    tr.modelid,
    tr.company,
    tr.text,
    tr.typeJournal,
    tr.file_content
  )
}
final case class FinancialsTransaction(
  id: Long,
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
  def month: String = common.getMonthAsString(transdate)
  def year: Int     = common.getYear(transdate)
  def getPeriod     = common.getPeriod(transdate)

  def total: BigDecimal = lines.map(_.amount) reduce ((l1, l2) => l2.add(l1).setScale(2, RoundingMode.HALF_UP))
  def toDerive()    = lines.map(l =>
    DerivedTransaction(
      id,
      oid,
      l.account,
      transdate,
      enterdate,
      postingdate,
      period,
      posted,
      modelid,
      company,
      text,
      file_content,
      l.id,
      l.side,
      l.oaccount,
      l.amount,
      l.currency,
      l.text
    )
  )

}
object FinancialsTransactionDetails  {
  import FinancialsTransaction.FinancialsTransaction_Type2
  val dummy                                                   = FinancialsTransactionDetails(0, 0, "", true, "", zeroAmount, Instant.now(), "", "EUR")
  implicit val monoid: Identity[FinancialsTransactionDetails] =
    new Identity[FinancialsTransactionDetails] {
      def identity                                                                          = dummy
      def combine(m1: => FinancialsTransactionDetails, m2: => FinancialsTransactionDetails) =
        m2.copy(amount = m2.amount.add(m1.amount))
    }

  type FinancialsTransactionDetails_Type = (Long, Long, String, Boolean, String, BigDecimal, Instant, String, String)
  type FTX2                              = FinancialsTransaction_Type2
  def apply(tr: FinancialsTransactionDetails_Type): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9)
  def apply(tr: FinancialsTransactionDetails.FTX2): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(tr._15, tr._1, tr._16, tr._17, tr._18, tr._19, tr._20, tr._21, tr._22)

  def apply(x: DerivedTransaction_Type): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(x._13, x._1, x._3, x._14, x._15, x._16, x._4, x._18, x._17)
}
object FinancialsTransaction         {
  type FinancialsTransaction_Type =
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
  type DerivedTransaction_Type     = (
    Long,
    Long,
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
    Long,
    Boolean,
    String,
    BigDecimal,
    String,
    String
  )
  def apply(tr: FinancialsTransactionx): FinancialsTransaction = FinancialsTransaction(
    tr.id,
    tr.oid,
    tr.costcenter,
    tr.account,
    tr.transdate,
    tr.enterdate,
    tr.postingdate,
    tr.period,
    tr.posted,
    tr.modelid,
    tr.company,
    tr.text,
    tr.typeJournal,
    tr.file_content
  )

  def apply(tr: FinancialsTransaction_Type): FinancialsTransaction =
    new FinancialsTransaction(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11, tr._12, tr._13, tr._14)

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

  def applyD(transactions: List[DerivedTransaction_Type]): List[FinancialsTransaction]           =
    transactions
      .groupBy(rc => (rc._1, rc._2, rc._3, rc._4, rc._5, rc._6, rc._7, rc._8, rc._9, rc._10, rc._11, rc._12))
      .map { case (k, v) =>
        new FinancialsTransaction(k._1, k._2, k._3, k._3, k._4, k._5, k._6, k._7, k._8, k._9, k._10, k._11, k._12)
          .copy(lines = v.filter(p => p._13 != -1).map(FinancialsTransactionDetails.apply))
      }
      .toList
  def applyL(transactions: List[FinancialsTransactionDetails.FTX2]): List[FinancialsTransaction] =
    transactions
      .groupBy(rc => (rc._1, rc._2, rc._3, rc._4, rc._5, rc._6, rc._7, rc._8, rc._9, rc._10, rc._11, rc._12, rc._13, rc._14))
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
      .copy(lines =
        List(
          FinancialsTransactionDetails(
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
        )
      )
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
final case class Journal_(
  transid: Long,
  oid: Long,
  account: String,
  oaccount: String,
  transdate: Instant,
  enterdate: Instant,
  postingdate: Instant,
  period: Int,
  amount: BigDecimal,
  idebit: BigDecimal,
  debit: BigDecimal,
  icredit: BigDecimal,
  credit: BigDecimal,
  currency: String,
  side: Boolean,
  text: String = "",
  month: Int,
  year: Int,
  company: String,
  // file: Int = 0,
  modelid: Int
)
object Journal_                      {
  def apply(j: Journal): Journal_ = new Journal_(
    j.transid,
    j.oid,
    j.account,
    j.oaccount,
    j.transdate,
    j.postingdate,
    j.enterdate,
    j.period,
    j.amount,
    j.idebit,
    j.debit,
    j.icredit,
    j.credit,
    j.currency,
    j.side,
    j.text,
    j.month,
    j.year,
    j.company,
    j.modelid
  )
}
final case class Journal(
  id: Long,
  transid: Long,
  oid: Long,
  account: String,
  oaccount: String,
  transdate: Instant,
  postingdate: Instant,
  enterdate: Instant,
  period: Int,
  amount: BigDecimal,
  idebit: BigDecimal,
  debit: BigDecimal,
  icredit: BigDecimal,
  credit: BigDecimal,
  currency: String,
  side: Boolean,
  text: String = "",
  month: Int,
  year: Int,
  company: String,
  modelid: Int
)

final case class Role(roleRepr: String)

object Role extends Enumeration { // SimpleAuthEnum[Role, String] {
  val Admin: Role      = Role("Admin")
  val DevOps: Role     = Role("DevOps")
  val Dev: Role        = Role("Developer")
  val Customer: Role   = Role("Customer")
  val Supplier: Role   = Role("Supplier")
  val Logistics: Role  = Role("Logistics")
  val Accountant: Role = Role("Accountant")
  val Tester: Role     = Role("Tester")

  // override val values: AuthGroup[Role] = AuthGroup(Admin, Customer, Accountant, Tester)

  def getRepr(t: Role): String = t.roleRepr
}

final case class User(
  id: Int,
  userName: String,
  firstName: String,
  lastName: String,
  hash: String,
  phone: String,
  email: String,
  department: String, // Role,
  menu: String = "",
  company: String = "1000",
  modelid: Int = 111
)

final case class User_(
  userName: String,
  firstName: String,
  lastName: String,
  hash: String,
  phone: String,
  email: String,
  department: String,
  menu: String = "",
  company: String = "1000",
  modelid: Int = 111
)
object User_ {
  def apply(u: User): User_ = new User_(u.userName, u.firstName, u.lastName, u.hash, u.phone, u.email, u.department, u.menu, u.company, u.modelid)
}
final case class LoginRequest(userName: String, password: String, company: String, language:String)
