package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountClass.dummy

import java.util.Locale
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import zio.prelude._
import zio.stm._
import zio.{UIO, _}

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import scala.collection.immutable.{::, List, Nil}
import scala.annotation.tailrec
import java.math.{BigDecimal, RoundingMode}

object common {

  val zeroAmount   = BigDecimal.valueOf(0, 2)
  val dummyBalance = Balance("dummy", zeroAmount, zeroAmount, zeroAmount, zeroAmount)

  val DummyUser = User(-1, "dummy", "dummy", "dummy", "dummyhash2", "dummyphone", "dummy@user.com", "dummy", "dummymenu", "0000", 111)

  def reduce[A: Identity](all: Iterable[A], dummy: A): A =
    all.toList match {
      case Nil     => dummy
      case x :: xs => NonEmptyList.fromIterable(x, xs).reduce
    }

  implicit val amountAddMonoid: Identity[BigDecimal] = new Identity[BigDecimal] {
    def identity: BigDecimal                       = zeroAmount
    def combine(m1: => BigDecimal, m2: => BigDecimal): BigDecimal =
      m2.add(m1)
  }

  implicit val accMonoid: Identity[Account] = new Identity[Account] {
    def identity: Account                       = Account.dummy
    def combine(m1: => Account, m2: => Account) =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
  }

  implicit val accountClassMonoid: Identity[AccountClass] = new Identity[AccountClass] {
    def identity: AccountClass                       = AccountClass.dummy
    def combine(m1: => AccountClass, m2: => AccountClass) =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
  }

  implicit val accBalanceMonoid: Identity[Balance] = new Identity[Balance] {
    def identity: Balance = dummyBalance

    def combine(m1: => Balance, m2: => Balance) =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
  }

  implicit val pacMonoid: Identity[PeriodicAccountBalance] = new Identity[PeriodicAccountBalance] {
    def identity: PeriodicAccountBalance                                      = PeriodicAccountBalance.dummy
    def combine(m1: => PeriodicAccountBalance, m2: => PeriodicAccountBalance): PeriodicAccountBalance = {
      if(m1.id.equals(PeriodicAccountBalance.dummy.id)){
        m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)
      }else {
        m1.idebiting(m2.idebit).icrediting(m2.icredit).debiting(m2.debit).crediting(m2.credit)
      }
    }
  }

  implicit val stockMonoid: Identity[Stock] = new Identity[Stock] {
    def identity: Stock                                      = Stock.dummy
    def combine(m1: => Stock, m2: => Stock): Stock = {
      if(m1.article.equals(Stock.dummy.article)){
        m2.copy( quantity = m2.quantity.add(m1.quantity))
      }else {
        m1.copy( quantity = m1.quantity.add(m2.quantity))
      }
    }
  }
  def getMonthAsString(month: Int): String                 =
    if (month <= 9) {
      "0".concat(month.toString)
    } else month.toString
  def getYear(instant: Instant)                            = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+2")).getYear
  def getMonthAsString(instant: Instant): String           =
    getMonthAsString(LocalDateTime.ofInstant(instant, ZoneId.of("UTC+2")).getMonth.getValue)
  def getPeriod(instant: Instant)                          = {
    val year = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+2")).getYear
    year.toString.concat(getMonthAsString(instant)).toInt
  }
}

object TransactionModelId extends Enumeration {
  type modelId = Value
  val RQF = Value(100)
  val REQUISITION = Value(101)
  val CONTRACT = Value(103)
  val PURCHASE_ORDER = Value(104)
  val GOORECEIVING = Value(105)
  val SUPPLIER_INVOICE = Value(106)
  val QUOTATION = Value(107)
  val SALES_CONTRACT = Value(108)
  val SALES_ORDER = Value(109)
  val BILL_OF_DELIVERY = Value(110)
  val CUSTOMER_INVOICE = Value(111)
  val PAYABLES = Value(112)
  val PAYMENT = Value(114)
  val RECEIVABLES = Value(122)
  val SETTLEMENT = Value(124)
  val GENERAL_LEDGER = Value(134)
  val PAYROLL = Value(136)
  val CASH = Value(144)

}

import common._
final case class Store(id: String,
                       name: String,
                       description: String,
                       account: String,
                       enterdate: Instant = Instant.now(),
                       changedate: Instant = Instant.now(),
                       postingdate: Instant = Instant.now(),
                       company: String,
                       modelid: Int = Store.MODELID)
object Store {
  val MODELID = 35
}
final case class Article_(id: String,
                          name: String,
                          description: String,
                          parent: String,
                          sprice: BigDecimal = zeroAmount,
                          pprice: BigDecimal = zeroAmount,
                          avgPrice: BigDecimal = zeroAmount,
                          currency: String,
                          stocked: Boolean = false,
                          quantityUnit: String,
                          packUnit: String,
                          stockAccount: String,
                          expenseAccount: String,
                          vatCode: String,
                          company: String,
                          modelid: Int = Article.MODELID,
                          enterdate: Instant = Instant.now(),
                          changedate: Instant = Instant.now(),
                          postingdate: Instant = Instant.now())
object Article_ {
  def apply(art: Article): Article_ = new Article_(
    art.id,
    art.name,
    art.description,
    art.parent,
    art.sprice,
    art.pprice,
    art.avgPrice,
    art.currency,
    art.stocked,
    art.quantityUnit,
    art.packUnit,
    art.stockAccount,
    art.expenseAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate)
}

final case class Article(id: String,
                         name: String,
                         description: String,
                         parent: String,
                         sprice: BigDecimal = zeroAmount,
                         pprice: BigDecimal = zeroAmount,
                         avgPrice: BigDecimal = zeroAmount,
                         currency: String,
                         stocked: Boolean = false,
                         quantityUnit:String,
                         packUnit:String,
                         stockAccount: String,
                         expenseAccount: String,
                         vatCode: String,
                         company: String,
                         modelid: Int = Article.MODELID,
                         enterdate: Instant = Instant.now(),
                         changedate: Instant = Instant.now(),
                         postingdate: Instant = Instant.now(),
                         bom: List[Bom] = List.empty[Bom]) extends IWS
final case class TArticle(id: String,
                         name: String,
                         description: String,
                         parent: String,
                         sprice: TRef[BigDecimal],
                         pprice: TRef[BigDecimal],
                         avgPrice: TRef[BigDecimal],
                         currency: String,
                         stocked: Boolean = false,
                         quantityUnit:String,
                         packUnit:String,
                         stockAccount: String,
                         expenseAccount: String,
                         vatCode: String,
                         company: String,
                         modelid: Int = Article.MODELID,
                         enterdate: Instant = Instant.now(),
                         changedate: Instant = Instant.now(),
                         postingdate: Instant = Instant.now(),
                         bom: List[Bom] = List.empty[Bom]
                        ) extends IWS
{
  self =>
  def setSalesPrice( price: BigDecimal): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        _ <- self.sprice.set(price)
      } yield ()
    }
  def setAvgPrice( price: BigDecimal): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        _ <- self.avgPrice.set(price)
      } yield ()
    }
  def setPprice( price: BigDecimal): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        _ <- self.pprice.set(price)
      } yield ()
    }
}
object Article {

  val MODELID = 34
  private type Article_Type = (
    String,
      String,
      String,
      String,
      BigDecimal,
      BigDecimal,
      BigDecimal,
      String,
      Boolean,
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

  def apply(art: Article_): Article = new Article(
    art.id,
    art.name,
    art.description,
    art.parent,
    art.sprice,
    art.pprice,
    art.avgPrice,
    art.currency,
    art.stocked,
    art.quantityUnit,
    art.packUnit,
    art.stockAccount,
    art.expenseAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate,
   List.empty[Bom]
  )
  def apply(acc: Article_Type): Article =
    new Article(
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
      acc._17,
      acc._18,
      acc._19,
      Nil
    )

  def apply(art: TArticle): ZIO[Any, Nothing, Article] = for {
    pprice  <- art.pprice.get.commit
    sprice  <- art.sprice.get.commit
    avgPrice  <- art.avgPrice.get.commit
  } yield  Article (art.id,
    art.name,
    art.description,
    art.parent,
    sprice,
    pprice,
    avgPrice,
    art.currency,
    art.stocked,
    art.quantityUnit,
    art.packUnit,
    art.stockAccount,
    art.expenseAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate,
    art.bom)

  def applyT(art: Article): UIO[TArticle] = for {
    pprice  <- TRef.makeCommit(art.pprice)
    sprice  <- TRef.makeCommit(art.sprice)
    avgPrice  <- TRef.makeCommit(art.avgPrice)
  } yield TArticle(art.id,
    art.name,
    art.description,
    art.parent,
    sprice,
    pprice,
    avgPrice,
    art.currency,
    art.stocked,
    art.quantityUnit,
    art.packUnit,
    art.stockAccount,
    art.expenseAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate,
    art.bom)

  def applyT(art: Article, avgPrice:BigDecimal): UIO[TArticle] = for {
    pprice  <- TRef.makeCommit(art.pprice)
    sprice  <- TRef.makeCommit(art.sprice)
    avgPriceNew  <- TRef.makeCommit(avgPrice)
  } yield TArticle(art.id,
    art.name,
    art.description,
    art.parent,
    sprice,
    pprice,
    avgPriceNew,
    art.currency,
    art.stocked,
    art.quantityUnit,
    art.packUnit,
    art.stockAccount,
    art.expenseAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate,
    art.bom)
}
final case class Bom(id:String, parent:String, quantity:BigDecimal, description:String, company:String, modelid: Int =Bom.MODELID)
object Bom {
  val MODELID= 34
  val dummy = Bom("-1", "", zeroAmount, "", "",36)
}
final case class Company_(
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
                          purchasingClearingAcc:String,
                          salesClearingAcc:String,
                          modelid: Int
                        )
object Company_ {
  def  apply(c:Company):Company_ = Company_(c.id, c.name, c.street, c.zip, c.city, c.state, c.country, c.email, c.partner,
    c.phone, c.bankAcc, c.iban, c.taxCode, c.vatCode, c.currency, c.locale, c.balanceSheetAcc, c.incomeStmtAcc,
    c.purchasingClearingAcc, c.salesClearingAcc, c.modelid)
}
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
  purchasingClearingAcc:String,
  salesClearingAcc:String,
  modelid: Int,
 bankaccounts: List[BankAccount] = List.empty[BankAccount]
)
object Company{
  val MODEL_ID=10;
  type TYPE=(
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
    String,
    String,
    String,
    String,
    String,
     Int
  )
  def apply(c:TYPE):Company = Company(c._1, c._2,c._3,c._4,c._5,c._6,c._7,c._8,c._9,c._10
    ,c._11,c._12,c._13,c._14,c._15, c._16,c._17,c._18,c._19, c._20, c._21,List.empty[BankAccount])

  def dummy:Company = Company("-1",  "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", MODEL_ID, Nil)

}
sealed trait AppError extends Throwable

object AppError {
  final case class RepositoryError(message: String) extends AppError
  final case class DecodingError(message: String)    extends AppError
}

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
trait AccountT extends IWS {
  //def parent:String
  def debiting(amount: BigDecimal):AccountT
  def crediting(amount: BigDecimal):AccountT
  def idebiting(amount: BigDecimal):AccountT
  def icrediting(amount: BigDecimal):AccountT
  def fdebit:BigDecimal
  def fcredit:BigDecimal
  def dbalance:BigDecimal
  def cbalance:BigDecimal
  def balance:BigDecimal

  def getBalance:Balance
}
final case class Account (
  id: String,
  name: String,
  description: String,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  company: String,
  modelid: Int = 9,
  account: String,
  isDebit: Boolean,
  balancesheet: Boolean,
  currency: String,
  idebit: BigDecimal = zeroAmount,
  icredit: BigDecimal = zeroAmount,
  debit: BigDecimal = zeroAmount,
  credit: BigDecimal = zeroAmount,
  subAccounts: Set[Account] = Nil.toSet
) extends IWS {

   def report(child: List[Account]): Account =
     reduce(child.filter(acc=>acc.account == id).map(acc => acc.report(child)), Account.dummy)
  def debiting(amount: BigDecimal): Account = copy(debit = debit.add(amount))

  def crediting(amount: BigDecimal): Account = copy(credit = credit.add(amount))

  def idebiting(amount: BigDecimal): Account = copy(idebit = idebit.add(amount))

  def icrediting(amount: BigDecimal): Account = copy(icredit = icredit.add(amount))

  def fdebit: BigDecimal = debit.add(idebit)

  def fcredit: BigDecimal = credit.add(icredit)

  def dbalance: BigDecimal = fdebit.subtract(fcredit)

  def cbalance: BigDecimal = fcredit.subtract(fdebit)

  def balance: BigDecimal = if (isDebit) dbalance else cbalance

  def getBalance: Balance = Balance(id, idebit, icredit, debit, credit)

  def add(acc: Account): Account =
    copy(subAccounts = subAccounts + acc);

  def remove(acc: Account): Account =
    copy(subAccounts = subAccounts.filterNot(_.id == acc.id))

  def filterAddSubAccounts(accSet: Set[Account]): Account =
    copy(subAccounts = accSet.filter(_.account == id).map(_.filterAddSubAccounts(accSet)))


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
        val y: Account = parent.updateBalance(this)
        val z: List[Account] = all.filterNot(acc => acc.id == parent.id) :+ y
        y.updateBalanceParent(z)
      case None => all
    }

  def getChildren: Set[Account] = subAccounts.toList match {
    case Nil => Set(copy(id = id))
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
        val sub = account.subAccounts.filterNot((acc => acc.balance.compareTo(new BigDecimal(0)) == 0 && acc.subAccounts.isEmpty))
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

final case class Asset (id: String,
                        name: String,
                        description: String,
                        enterdate: Instant = Instant.now(),
                        changedate: Instant = Instant.now(),
                        postingdate: Instant = Instant.now(),
                        company: String,
                        modelid: Int = Asset.MODELID,
                        account: String,
                        oaccount: String,
                        depMethod:Int,
                        amount:BigDecimal,
                        rate: BigDecimal,
                        lifeSpan:Int,
                        scrapValue: BigDecimal = zeroAmount,
                        frequency:Int,
                        currency: String = "EUR ")
object Asset {
  val MODELID = 19
}

sealed trait IWS {
  def id: String
}
 case class Masterfile(id: String,
                             name: String = "",
                             description: String = "",
                             parent: String = "",
                             enterdate: Instant = Instant.now(),
                             changedate: Instant = Instant.now(),
                             postingdate: Instant = Instant.now(),
                             modelid: Int,
                             company: String
                           ) extends IWS
   object AccountClass{
     val dummy                                         = AccountClass(
       "",
       "",
       "",
       "",
       Instant.now(),
       Instant.now(),
       Instant.now(),
       36,
       "1000",
       true,
       zeroAmount,
       zeroAmount,
       zeroAmount,
       zeroAmount
     )
   }
  final case  class AccountClass ( id: String,
                                  name: String = "",
                                  description: String = "",
                                  parent: String = "",
                                  enterdate: Instant = Instant.now(),
                                  changedate: Instant = Instant.now(),
                                  postingdate: Instant = Instant.now(),
                                  modelid: Int,
                                  company: String,
                                   isDebit: Boolean,
                                   idebit: BigDecimal = zeroAmount,
                                   icredit: BigDecimal = zeroAmount,
                                   debit: BigDecimal = zeroAmount,
                                   credit: BigDecimal = zeroAmount,
                                 ) extends  AccountT{

      def report(child: List[AccountClass]): AccountClass =
       reduce(child.filter(acc=>acc.parent==id).map(acc => acc.report(child)), dummy)

    def debiting(amount: BigDecimal): AccountClass = copy(debit = debit.add(amount))
    def crediting(amount: BigDecimal): AccountClass = copy(credit = credit.add(amount))
    def idebiting(amount: BigDecimal): AccountClass = copy(idebit = idebit.add(amount))
    def icrediting(amount: BigDecimal): AccountClass = copy(icredit = icredit.add(amount))
    def fdebit: BigDecimal = debit.add(idebit)
    def fcredit: BigDecimal = credit.add(icredit)
    def dbalance: BigDecimal = fdebit.subtract(fcredit)
    def cbalance: BigDecimal = fcredit.subtract(fdebit)
    def balance: BigDecimal = if (isDebit) dbalance else cbalance
    def getBalance: Balance = Balance(id, idebit, icredit, debit, credit)
  }
//final case  class AccountGroup (override val id: String,
//                                override val name: String = "",
//                                override val description: String = "",
//                                override val parent: String = "",
//                                override val enterdate: Instant = Instant.now(),
//                                override val changedate: Instant = Instant.now(),
//                                override val postingdate: Instant = Instant.now(),
//                                override val modelid: Int,
//                                override val company: String
//                               ) extends  Masterfile (id, name, description, parent, enterdate, changedate, postingdate, modelid, company)
final case class Costcenter(
  id: String,
  name: String = "",
  description: String = "",
  account: String = "",
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  modelid: Int = Costcenter.MODEL_ID,
  company: String
) extends IWS
object Costcenter {
  val MODEL_ID = 6
}

final case class ImportFile( id: String,
                             name: String,
                             description: String,
                             extension: String,
                             enterdate: Instant = Instant.now(),
                             changedate: Instant = Instant.now(),
                             postingdate: Instant = Instant.now(),
                             modelid: Int = 81,
                             company: String) extends IWS

final case class SalaryItem(id: String,
                            name: String = "",
                            description: String = "",
                            account:String,
                            amount:BigDecimal,
                            percentage:BigDecimal,
                            enterdate: Instant = Instant.now(),
                            changedate: Instant = Instant.now(),
                            postingdate: Instant = Instant.now(),
                            modelid: Int = 171,
                            company: String
                           ) extends IWS
 final case class PayrollTaxRange (id: String, fromAmount:BigDecimal, toAmount:BigDecimal, tax:BigDecimal, taxClass:String, modelid: Int = PayrollTaxRange.MODELID, company: String)
object PayrollTaxRange {
  val MODELID = 172
}
final case class EmployeeSalaryItem(id: String, owner: String, account: String, amount: BigDecimal, percentage: BigDecimal, text:String, company: String)
object EmployeeSalaryItem {
type  ESI_Type =(String, String, String, BigDecimal, BigDecimal, String, String)
  def apply(bs: ESI_Type): EmployeeSalaryItem = EmployeeSalaryItem(
    bs._1,
    bs._2,
    bs._3,
    bs._4,
    bs._5,
    bs._6,
    bs._7)
def apply(item:EmployeeSalaryItemDTO):EmployeeSalaryItem =EmployeeSalaryItem(item.id, item.owner, item.account, item.amount, item.percentage, item.text, item.company)
}
final case class EmployeeSalaryItemDTO(id: String, owner: String, account: String, accountName: String, amount: BigDecimal, percentage: BigDecimal, text:String, company: String)

object EmployeeSalaryItemDTO {
  def apply(item:EmployeeSalaryItem):EmployeeSalaryItemDTO =EmployeeSalaryItemDTO(item.id, item.owner, item.account, "", item.amount, item.percentage, item.text, item.company)
}
final case class Bank(
  id: String,
  name: String = "",
  description: String = "",
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  modelid: Int = Bank.MODEL_ID,
  company: String
) extends IWS
object Bank {
  val MODEL_ID = 11
}
final case class BankAccount(id: String, bic: String, owner: String, company: String, modelid: Int = BankAccount.MODEL_ID )
object BankAccount    {
  import scala.math.Ordering
  val MODEL_ID = 12
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
  modelid: Int = BankStatement_.MODEL_ID,
  period: Int //= common.getPeriod(Instant.now())
)
object BankStatement_ {
  val MODEL_ID = 18
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
    bs.modelid,
    bs.period
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
  modelid: Int = BankStatement.MODELID,
  period: Int //= common.getPeriod(Instant.now())
)
object BankStatement  {
  val MODELID         = 18
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
    Int,
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
    bs._16,
    bs._17
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
    val postingtext = values(3)
    val purpose     = values(4)
    val beneficiary = values(5)
    val accountno   = values(6)
    val bankCode    = values(7)
    val amount_     = values(8).trim
    val amount      = new BigDecimal(NUMBER_FORMAT.parse(amount_).toString)
    val currency    = values(9)
    val info        = values(10)
    val posted      = false
    val period      = common.getPeriod(valuedate)
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
      companyIban,
      posted,
      MODELID,
      period
    )
     println ("BankStatement>>"+bs)
    bs
  }

}
final case class Module(
  id: String,
  name: String = "",
  description: String = "",
  path: String = "",
  parent: String= "",
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now(),
  modelid: Int = 400,
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
  modelid: Int = Vat.MODEL_ID
)
object Vat {
  val MODEL_ID = 14
  val dummy: Vat = Vat("", "", "", zeroAmount, "", "", Instant.now(), Instant.now(), Instant.now(), "", Vat.MODEL_ID)
}
final case class Stock(id:String, store:String, article:String, quantity:BigDecimal, charge:String, company:String, modelid: Int = Stock.MODELID)

final case class TStock(id:String, store:String, article:String, quantity:TRef[BigDecimal], charge:String, company:String
                        , modelid: Int = Stock.MODELID) {
  self =>
  def transfer(from: TStock,  quantity: BigDecimal): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        _ <- self.quantity.update(_.add(quantity))
        _ <- from.quantity.update(_.subtract(quantity))
      } yield ()
    }

  def add( quantity: BigDecimal): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        _ <- self.quantity.update(_.add(quantity))
      } yield ()
    }

  def substract( quantity: BigDecimal): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        _ <- self.quantity.update(_.subtract(quantity))
      } yield ()
    }

  def multiply( quantity: BigDecimal): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        _ <- self.quantity.update(_.multiply(quantity))
      } yield ()
    }
}
object Stock {
  val MODELID = 37
  val dummy: Stock =   make("-1", "-1", zeroAmount, "-1",  "")
  private type STOCK_Type = (String, String, String,  BigDecimal, String, String, Int)
   def buildId(store:String, article:String, charge:String, company:String) =
    store.concat(article).concat(company).concat(charge)
    def make (store:String, article:String, quantity:BigDecimal, charge:String, company:String): Stock =
      Stock( buildId(store, article,  charge, company), store, article, quantity, charge, company)
  def apply(stock: STOCK_Type): Stock = Stock(stock._1, stock._2,stock._3,stock._4,stock._5,stock._6,stock._7)

  def apply(stock: TStock): ZIO[Any, Nothing, Stock] = for {
    quantity_  <- stock.quantity.get.commit
  } yield Stock(stock.id, stock.store, stock.article, quantity_, stock.charge,  stock.company, stock.modelid)

  def create(model: Transaction): List[Stock] =
    model.lines.map(line => Stock.make(model.store, line.article, line.quantity, "", model.company))

  def create(models: List[Transaction]): List[Stock] = {
    val x = models.flatMap(m=>m.lines.map(line => Stock.make(m.store, line.article, line.quantity, "", m.company)))
    groupByStock( x)
  }

  private def groupByStock(r: List[Stock]) =
    (r.groupBy(st=>st.article.concat(st.store).concat(st.company)) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).toList

}
object TStock {
  def make (store:String, article:String, quantity:BigDecimal, charge:String, company:String):  UIO[TStock] =
    apply(Stock(Stock.buildId(store, article,  charge, company), store, article, quantity, charge, company))

  def apply(stock: Stock): UIO[TStock] = for {
    quantity  <- TRef.makeCommit(stock.quantity)
  } yield TStock(stock.id, stock.store, stock.article, quantity, stock.charge,  stock.company, stock.modelid)

  def apply(stock: Stock, quantity:BigDecimal): UIO[TStock] = for {
    quantity_  <- TRef.makeCommit(stock.quantity.add(quantity))
  } yield TStock(stock.id, stock.store, stock.article, quantity_, stock.charge,  stock.company, stock.modelid)
}
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
  name: String,
  modelid: Int = PeriodicAccountBalance.MODELID
) {
  self =>
  def transfer(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        fidebit <- from.idebit.get
        ficredit <- from.icredit.get
        fdebit <- from.debit.get
        fcredit <- from.credit.get
        _ <- to.idebit.update(_.add(fidebit))
        _ <- to.icredit.update(_.add(ficredit))
        _ <- to.debit.update(_.add(fdebit))
        _ <- to.credit.update(_.add(fcredit))
      } yield ()
    }
}



object TPeriodicAccountBalance {

  val dummy = apply(PeriodicAccountBalance.dummy)
  def apply(pac: PeriodicAccountBalance): UIO[TPeriodicAccountBalance] = for {
    idebit  <- TRef.makeCommit(pac.idebit)
    icredit <- TRef.makeCommit(pac.icredit)
    debit   <- TRef.makeCommit(pac.debit)
    credit  <- TRef.makeCommit(pac.credit)
  } yield TPeriodicAccountBalance(pac.id, pac.account, pac.period, idebit, icredit, debit, credit, pac.currency, pac.company, pac.name, pac.modelid)

  def create(model: FinancialsTransaction): List[PeriodicAccountBalance] =
    model.lines.flatMap { line: FinancialsTransactionDetails =>
      val debited = PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, line.account),
        line.account,
        model.period,
        zeroAmount,
        zeroAmount,
        line.amount,
        zeroAmount,
        line.currency,
        model.company,
        line.accountName,
        PeriodicAccountBalance.MODELID
      )
      val credited = PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, line.oaccount),
        line.oaccount,
        model.period,
        zeroAmount,
        zeroAmount,
        zeroAmount,
        line.amount,
        line.currency,
        model.company,
        line.oaccountName,
        PeriodicAccountBalance.MODELID)
      List(debited, credited)
    }


  def transferX(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance, amount: BigDecimal): IO[Nothing, Unit] = {
    STM.atomically {
      for {
        _ <- from.credit.update(_.add(amount))
        _ <- to.debit.update(_.add(amount))
      } yield ()
    }
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
  name: String,
  modelid: Int = PeriodicAccountBalance.MODELID) {
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

  private type PAC_Type = (String, String, Int, BigDecimal, BigDecimal, BigDecimal, BigDecimal, String, String, String, Int)

  val MODELID                                   = 106
  def createId(period: Int, accountId: String) = period.toString.concat(accountId)
  val dummy                                    =
    PeriodicAccountBalance("-1", "", 0, zeroAmount, zeroAmount, zeroAmount, zeroAmount, "EUR", "1000", "")

  def create(accountId: String, period: Int, currency: String, company: String, name: String): PeriodicAccountBalance =
    PeriodicAccountBalance.apply(
      PeriodicAccountBalance.createId(period, accountId),
      accountId,
      period,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      currency,
      company,
      name,
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
          line.amount,
          zeroAmount,
          line.currency,
          model.company,
          line.accountName,
          PeriodicAccountBalance.MODELID
        ),
        PeriodicAccountBalance.apply(
          PeriodicAccountBalance.createId(model.period, line.oaccount),
          line.oaccount,
          model.period,
          zeroAmount,
          zeroAmount,
          zeroAmount,
          line.amount,
          line.currency,
          model.company,
          line.oaccountName,
          PeriodicAccountBalance.MODELID
        )
      )
    }
  def applyX(p: PAC_Type)                                                = PeriodicAccountBalance(p._1, p._2, p._3, p._4, p._5, p._6, p._7, p._8, p._9, p._10, p._11)

  def applyT(tpac: TPeriodicAccountBalance): ZIO[Any, Nothing, PeriodicAccountBalance] = for {
    idebit  <- tpac.idebit.get.commit
    icredit <- tpac.icredit.get.commit
    debit   <- tpac.debit.get.commit
    credit  <- tpac.credit.get.commit
  } yield PeriodicAccountBalance(tpac.id, tpac.account, tpac.period, idebit, icredit, debit, credit, tpac.currency, tpac.company, tpac.name, tpac.modelid)
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
  //def iban: String
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
  vatcode: String,
  company: String,
  modelid: Int = Supplier.MODELID,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now()
)
object Supplier_ {
  def apply(c:Supplier):Supplier_ = Supplier_(c.id,
    c.name,
    c.description,
    c.street,
    c.zip,
    c.city,
    c.state,
    c.country,
    c.phone,
    c.email,
    c.account,
    c.oaccount,
    c.vatcode,
    c.company,
    c.modelid,
    c.enterdate,
    c.changedate,
    c.postingdate)
}
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
      //c._19,
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
  vatcode: String,
  company: String,
  modelid: Int = Customer.MODELID,
  enterdate: Instant = Instant.now(),
  changedate: Instant = Instant.now(),
  postingdate: Instant = Instant.now()
)
object Customer_ {
  def apply(c:Customer):Customer_ = Customer_(c.id,
    c.name,
    c.description,
    c.street,
    c.zip,
    c.city,
    c.state,
    c.country,
    c.phone,
    c.email,
    c.account,
    c.oaccount,
    c.vatcode,
    c.company,
    c.modelid,
    c.enterdate,
    c.changedate,
    c.postingdate)
}
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
      //c._19,
      List.empty[BankAccount]
    )
}
final case class Employee_(
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
                            vatcode: String,
                            company: String,
                            salary:BigDecimal,
                            modelid: Int = Employee.MODELID,
                            enterdate: Instant = Instant.now(),
                            changedate: Instant = Instant.now(),
                            postingdate: Instant = Instant.now()
                          )
object Employee_ {
  def apply(c:Employee):Employee_ = Employee_(c.id,
    c.name,
    c.description,
    c.street,
    c.zip,
    c.city,
    c.state,
    c.country,
    c.phone,
    c.email,
    c.account,
    c.oaccount,
    c.vatcode,
    c.company,
    c.salary,
    c.modelid,
    c.enterdate,
    c.changedate,
    c.postingdate)
}
final case class Employee(
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
                           vatcode: String,
                           company: String,
                           salary:BigDecimal,
                           modelid: Int = Employee.MODELID,
                           enterdate: Instant = Instant.now(),
                           changedate: Instant = Instant.now(),
                           postingdate: Instant = Instant.now(),
                           bankaccounts: List[BankAccount] = List.empty[BankAccount],
                           salaryItems: List[EmployeeSalaryItemDTO] = List.empty[EmployeeSalaryItemDTO]
                         ) extends BusinessPartner
object Employee                      {
  val MODELID = 33
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
      BigDecimal,
      Int,
      Instant,
      Instant,
      Instant
    )

  def apply(c: TYPE): Employee =
    Employee(
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
final case class TransactionDetails( id: Long, transid: Long, article: String, articleName:String, quantity: BigDecimal, unit: String, price: BigDecimal,
                                     currency: String, duedate: Instant = Instant.now(), vatCode:String, text: String)
final case class TransactionDetails_(transid: Long, article: String, articleName:String, quantity: BigDecimal, unit: String, price: BigDecimal,
                                     currency: String, duedate: Instant = Instant.now(), vatCode:String, text: String)

object TransactionDetails  {

  val dummy                                                   = TransactionDetails(0, 0, "", "", zeroAmount, "", zeroAmount, "", Instant.now(), "",  "")
  implicit val monoid: Identity[TransactionDetails] =
    new Identity[TransactionDetails] {
      def identity: TransactionDetails = dummy
      def combine(m1: => TransactionDetails, m2: => TransactionDetails): TransactionDetails =
        m2.copy(quantity = m2.quantity.add(m1.quantity))
    }

  type TransactionDetails_Type = (Long, Long, String, String, BigDecimal, String, BigDecimal, String, Instant,  String, String)

  def apply(tr: TransactionDetails_Type): TransactionDetails =
    new TransactionDetails(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11)
  def apply(tr: TransactionDetails): TransactionDetails =
    new TransactionDetails(tr.id, tr.transid, tr.article, tr.articleName, tr.quantity, tr.unit, tr.price, tr.currency, tr.duedate, tr.vatCode, tr.text)
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
  currency: String,
  accountName: String,
  oaccountName: String
)

final case class FinancialsTransactionDetails_(
  transid: Long,
  account: String,
  side: Boolean,
  oaccount: String,
  amount: BigDecimal,
  duedate: Instant = Instant.now(),
  text: String,
  currency: String,
  accountName: String,
  oaccountName: String
)
object FinancialsTransactionDetails_ {
  def apply(tr: FinancialsTransactionDetails): FinancialsTransactionDetails_ =
    new FinancialsTransactionDetails_(tr.transid,  tr.account, tr.side, tr.oaccount, tr.amount, tr.duedate, tr.text, tr.currency, tr.accountName, tr.oaccountName)
}

final case class FinancialsTransactionx(
  id: Long,
  oid: Long,
  id1: Long,
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
  id1: Long,
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
    tr.id1,
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
final case class Transaction_(oid: Long,
                              id1: Long,
                              store: String,
                              costcenter: String,
                              transdate: Instant = Instant.now(),
                              enterdate: Instant = Instant.now(),
                              postingdate: Instant = Instant.now(),
                              period: Int = common.getPeriod(Instant.now()),
                              posted: Boolean = false,
                              modelid: Int,
                              company: String,
                              text: String = ""
                             )
final case class Transactionx(id: Long,
                              oid: Long,
                              id1: Long,
                              store: String,
                              costcenter: String,
                              transdate: Instant = Instant.now(),
                              enterdate: Instant = Instant.now(),
                              postingdate: Instant = Instant.now(),
                              period: Int = common.getPeriod(Instant.now()),
                              posted: Boolean = false,
                              modelid: Int,
                              company: String,
                              text: String = ""
                            )
final case class Transaction(
                              id: Long,
                              oid: Long,
                              id1: Long,
                              store: String,
                              costcenter: String,
                              transdate: Instant = Instant.now(),
                              enterdate: Instant = Instant.now(),
                              postingdate: Instant = Instant.now(),
                              period: Int = common.getPeriod(Instant.now()),
                              posted: Boolean = false,
                              modelid: Int,
                              company: String,
                              text: String = "",
                              lines: List[TransactionDetails] = Nil
                                      ) {
  def month: String = common.getMonthAsString(transdate)
  def year: Int     = common.getYear(transdate)
  def getPeriod     = common.getPeriod(transdate)

  def total: BigDecimal = lines.map( l=>l.price.multiply(l.quantity)) reduce ((l1, l2) => l2.add(l1).setScale(2, RoundingMode.HALF_UP))

  def canceln: Transaction = copy(oid = id, id = 0, posted = false, lines=lines.map(line=>line.copy( quantity = line.quantity.negate())))
  def duplicate: Transaction = copy(oid = id, id = 0, posted = false)

}
object Transaction {

  type Transaction_Type =
    (Long, Long, Long, String, String, Instant, Instant, Instant, Int, Boolean, Int, String, String)
  def apply(tr: Transaction_Type): Transaction =
    new Transaction(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11, tr._12, tr._13, Nil)
  def applyC(tr: Transaction_Type): Transaction =
    Transaction(
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
      Nil
    )
}

final case class TransactionLog(id:Long, transid:Long, oid:Long, store:String, costcenter:String, article:String,
                                quantity:BigDecimal, stock:BigDecimal, wholeStock:BigDecimal, unit:String, price:BigDecimal, avgPrice:BigDecimal,
                                currency:String, duedate:Instant, text:String, transdate:Instant, postingdate:Instant, enterdate:Instant,
                                period:Int, company:String, modelid:Int)
final case class TransactionLog_(transid:Long, oid:Long, store:String, costcenter:String, article:String,
                                 quantity:BigDecimal, stock:BigDecimal, wholeStock:BigDecimal, unit:String, price:BigDecimal, avgPrice:BigDecimal,
                                 currency:String, duedate:Instant, text:String, transdate:Instant, postingdate:Instant, enterdate:Instant,
                                 period:Int, company:String, modelid:Int)
object TransactionLog_ {
  def apply(c:TransactionLog):TransactionLog_ = TransactionLog_( c.transid,
    c.oid,
    c.store,
    c.costcenter,
    c.article,
    c.quantity,
    c.stock,
    c.wholeStock,
    c.unit,
    c.price,
    c.avgPrice,
    c.currency,
    c.duedate,
    c.text,
    c.transdate,
    c.postingdate,
    c.enterdate,
    c.period,
    c.company,
    c.modelid)
}
final case class FinancialsTransaction(
  id: Long,
  oid: Long,
  id1: Long,
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
  lines: List[FinancialsTransactionDetails] = Nil,
  //lines2: List[TransactionDetails] = Nil
  // ,copyFlag: Boolean = false
) {
  def month: String = common.getMonthAsString(transdate)
  def year: Int     = common.getYear(transdate)
  def getPeriod     = common.getPeriod(transdate)

  def total: BigDecimal = lines.map(_.amount) reduce ((l1, l2) => l2.add(l1).setScale(2, RoundingMode.HALF_UP))

//  def total2: BigDecimal = lines2.map(l=>l.quantity.multiply(l.price)) reduce ((l1, l2) =>
//    l2.add(l1).setScale(2, RoundingMode.HALF_UP))
  def canceln: FinancialsTransaction = copy(oid = id, id = 0, posted = false, lines=lines.map(line=>line.copy( amount = line.amount.negate())))
  def duplicate: FinancialsTransaction = copy(oid = id, id = 0, posted = false)

}
object FinancialsTransactionDetails  {

  val dummy                                                   = FinancialsTransactionDetails(0, 0, "", true, "", zeroAmount, Instant.now(), "", "EUR", "", "")
  implicit val monoid: Identity[FinancialsTransactionDetails] =
    new Identity[FinancialsTransactionDetails] {
      def identity                                                                          = dummy
      def combine(m1: => FinancialsTransactionDetails, m2: => FinancialsTransactionDetails) =
        m2.copy(amount = m2.amount.add(m1.amount))
    }

  type FinancialsTransactionDetails_Type = (Long, Long,  String, Boolean, String, BigDecimal, Instant, String, String, String, String)

  def apply(tr: FinancialsTransactionDetails_Type): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11)

}
object FinancialsTransaction         {
  type FinancialsTransaction_Type =
    (Long, Long, Long, String, String, Instant, Instant, Instant, Int, Boolean, Int, String, String, Int, Int)


  def apply(tr: FinancialsTransactionx): FinancialsTransaction = FinancialsTransaction(
    tr.id,
    tr.oid,
    tr.id1,
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
      tr._15,
      Nil
    )
 val dummy: FinancialsTransaction = FinancialsTransaction(-1, 0,0, "dummy", "dummy", Instant.now(), Instant.now()
                                    , Instant.now(), -1, false,  -1, "",  "dummy", -1, -1)
}
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
  modelid: Int = User.MODELID,
  roles:List[Role] = List.empty[Role],
  rights:List[UserRight]=List.empty[UserRight]
)
final case class Userx(
                       id: Int,
                       userName: String,
                       firstName: String,
                       lastName: String,
                       hash: String,
                       phone: String,
                       email: String,
                       department: String,
                       menu: String = "",
                       company: String = "1000",
                       modelid: Int = User.MODELID
                     )
object User {
  val MODELID = 111
  type TYPE = (Int, String, String, String, String, String, String, String, String, String, Int)
  def apply(u: TYPE): User = new User( u._1, u._2,u._3,u._4,u._5,u._6,u._7,u._8,u._9, u._10, u._11)
}
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
  modelid: Int = User.MODELID
)
object User_ {
  def apply(u: User): User_ = new User_( u.userName, u.firstName, u.lastName, u.hash, u.phone, u.email, u.department, u.menu, u.company, u.modelid)
}
final case class LoginRequest(userName: String, password: String, company: String, language:String)
final case class Role(id:Int, name:String, description:String,
                      changedate: Instant,
                      postingdate: Instant,
                      enterdate: Instant,
                      modelid:Int = Role.MODELID,
                      company:String,
                      rights:List[UserRight]=List.empty[UserRight]
                          )
final case class Role_(id:Int, name:String, description:String,
                       changedate: Instant,
                       postingdate: Instant,
                       enterdate: Instant,
                       modelid:Int = Role.MODELID,
                       company:String)
object Role {
  val MODELID = 121
  type TYPE = (Int, String, String, Instant, Instant, Instant, Int, String)
  def apply(c:TYPE):Role = Role(c._1, c._2, c._3, c._4, c._5, c._6, c._7, c._8, List.empty[UserRight])
}
final case class  UserRight (moduleid:Int,  roleid:Int, short:String, company:String, modelid:Int = 131)
final case class  UserRole (userid:Int,  roleid:Int, company:String, modelid:Int = 161)
final case class  Permission (id:Int, name:String, description:String,
                              changedate: Instant,
                              postingdate: Instant,
                              enterdate: Instant,
                              modelid:Int =141,
                              company:String )

final case class  Fmodule (id:Int, name:String, description:String,
                           changedate: Instant,
                           postingdate: Instant,
                           enterdate: Instant,
                           account:String,
                           isDebit:Boolean,
                           parent:String,
                           modelid:Int = Fmodule.MODEL_ID,
                           company:String )
object Fmodule {
  val MODEL_ID = 151
}
