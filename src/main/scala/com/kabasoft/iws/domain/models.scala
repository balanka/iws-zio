package com.kabasoft.iws.domain

import zio.prelude.*
import zio.stm.*
import zio.{UIO, *}
import com.kabasoft.iws.domain.AccountClass.dummy
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{getPeriod, zeroAmount}

import java.util.Locale
import java.time.{Instant, LocalDate, LocalDateTime, OffsetDateTime, ZoneId}
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import scala.collection.immutable.{::, List, Nil}
import scala.annotation.tailrec
import java.math.{BigDecimal, RoundingMode}


object common:
  val zeroAmount: BigDecimal = BigDecimal.valueOf(0, 2)
  val dummyBalance = Balance("dummy", zeroAmount, zeroAmount, zeroAmount, zeroAmount)

  def reduce[A: Identity](all: Iterable[A], dummy: A): A =
    all.toList match
      case Nil     => dummy
      case x :: xs => NonEmptyList.fromIterable(x, xs).reduce

  given  amountAddMonoid: Identity[BigDecimal] = new Identity[BigDecimal]:
    def identity: BigDecimal                       = zeroAmount
    def combine(m1: => BigDecimal, m2: => BigDecimal): BigDecimal =
      m2.add(m1)
  given accMonoid: Identity[Account] = new Identity[Account]:
    def identity: Account                       = Account.dummy
    def combine(m1: => Account, m2: => Account): Account = {

      if(m1.id.equals(Account.dummy.id)) {
        m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit).bdebiting(m1.bdebit).bcrediting(m1.bcredit).copy(id=m2.account)
      } else m1.idebiting(m2.idebit).icrediting(m2.icredit).debiting(m2.debit).crediting(m2.credit).bdebiting(m2.bdebit).bcrediting(m2.bcredit).copy(id=m1.account)
    }

  given accountClassMonoid: Identity[AccountClass] = new Identity[AccountClass]:
    def identity: AccountClass                       = AccountClass.dummy
    def combine(m1: => AccountClass, m2: => AccountClass): AccountClass =
      if(m1.id.equals(AccountClass.dummy.id)) {
       m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit).bdebiting(m1.bdebit).bcrediting(m1.bcredit)
      } else m1.idebiting(m2.idebit).icrediting(m2.icredit).debiting(m2.debit).crediting(m2.credit).bdebiting(m2.bdebit).bcrediting(m2.bcredit)

  given  balanceMonoid: Identity[Balance] = new Identity[Balance]:
    def identity: Balance = dummyBalance
    def combine(m1: => Balance, m2: => Balance): Balance =
      m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit)

  given pacMonoid: Identity[PeriodicAccountBalance] = new Identity[PeriodicAccountBalance]:
    def identity: PeriodicAccountBalance                                      = PeriodicAccountBalance.dummy
    def combine(m1: => PeriodicAccountBalance, m2: => PeriodicAccountBalance): PeriodicAccountBalance =
      if(m1.id.equals(PeriodicAccountBalance.dummy.id)){
        m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit).bdebiting(m1.bdebit).bcrediting(m1.bcredit)
      } else {
        m1.idebiting(m2.idebit).icrediting(m2.icredit).debiting(m2.debit).crediting(m2.credit).bdebiting(m2.bdebit).bcrediting(m2.bcredit)
      }

  given stockMonoid: Identity[Stock] = new Identity[Stock]:
    def identity: Stock  = Stock.dummy
    def combine(m1: => Stock, m2: => Stock): Stock =
      if(m1.article.equals(Stock.dummy.article)) m2.copy( quantity = m2.quantity.add(m1.quantity))
      else m1.copy( quantity = m1.quantity.add(m2.quantity))
      
  given journalMonoid: Identity[Journal] = new Identity[Journal]:
    def identity: Journal                       = Journal.dummy
    def combine(m1: => Journal, m2: => Journal): Journal = m2.amounting(m1.amount)
      //m2.idebiting(m1.idebit).icrediting(m1.icredit).debiting(m1.debit).crediting(m1.credit).amounting(m1.amount)
      
  private def getMonthAsString(month: Int): String                 =
    if (month <= 9) {
      "0".concat(month.toString)
    } else month.toString
  def getYear(instant: Instant): Int = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+2")).getYear
  def getMonthAsString(instant: Instant): String           =
    getMonthAsString(LocalDateTime.ofInstant(instant, ZoneId.of("UTC+2")).getMonth.getValue)
  def getPeriod(instant: Instant): Int =
    val year = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+2")).getYear
    year.toString.concat(getMonthAsString(instant)).toInt

enum ModelId(val modelid: Int):
//type modelId = Value
  case SUPPLIER extends ModelId(1)
  case CUSTOMER extends ModelId(3)
  case COST_CEMTER extends ModelId(6)
  case ACCOUNT extends ModelId(9)
  case COMPANY extends ModelId(10)
  case BANK extends ModelId(11)
  case BANK_ACCOUNT extends ModelId(12)
  case ARTICLE_GROUP extends ModelId(13)
  case VAT extends ModelId(14)
  case QUANTITY_UNIT extends ModelId(15)
  case REMINDER_BALANCE extends ModelId(16)
  case BANK_STATEMENT extends ModelId(18)
  case ASSET extends ModelId(19)
  case STOCK extends ModelId(37)
  case EMPLOYEE extends ModelId(33)
  case ARTICLE extends ModelId(34)
  case STORE extends ModelId(35)
  case ACCOUNT_CLASS extends ModelId(36)
  case ACCOUNT_GROUP extends ModelId(31)
  case CLOSE_ACCOUNT_PERIOD extends ModelId (38)
  case CREATE_PAYROLL_TRANSACTION extends ModelId (39)
  case BOM extends ModelId (40)
  case CREATE_DEPRECIATION_TRANSACTION extends ModelId (41)
  case CURRENCY extends ModelId (99)
  case PURCHASE_ORDER extends ModelId (104)
  case GOODRECEIVING extends ModelId (105)
  case PERIODIC_ACCOUNT_BALANCE extends ModelId (106)
  case SALES_ORDER extends ModelId (109)
  case BILL_OF_DELIVERY extends ModelId (110)
  case USER extends ModelId (111)
  case CUSTOMER_INVOICE extends ModelId (111)
  case BANK_PAYMENT_SETLLEMENT extends ModelId (118)
  case ROLE extends ModelId (121)
  case USER_RIGHT extends ModelId (131)
  case PERMISSION extends ModelId (141)
  case FMODULE extends ModelId (151)
  case ROOM extends ModelId (152)
  case APARTMENT extends ModelId (153)
  case REALESTATE extends ModelId (154)
  case FLOOR extends ModelId (155)
  case USER_ROLE extends ModelId (161)
  case SALARY_ITEM extends ModelId (171)
  case PAYROLL_TAX_RANGE extends ModelId (172)
  case PARTNER extends ModelId (173)
  case IMPORT_FILE extends ModelId (181)
  case MODULE extends ModelId (400)
  case BALANCESHEET extends ModelId (1000)
  case SUPPLIER_INVOICE extends ModelId (1006)
  case FINANCIALS extends ModelId(1300)
  case TRANSACTION extends ModelId(1301)
  case LOGIN extends ModelId ( 11111)

enum TransactionModelId (val modelid:Int) :
  //type modelId = Value
  case  RQF extends TransactionModelId(100)
  case REQUISITION extends TransactionModelId(101)
  case CONTRACT extends TransactionModelId(103)
  case PURCHASE_ORDER extends TransactionModelId(104)
  case GOODRECEIVING extends TransactionModelId(105)
  case SUPPLIER_INVOICE extends TransactionModelId(1006)
  case QUOTATION extends TransactionModelId(107)
  case SALES_CONTRACT extends TransactionModelId(108)
  case SALES_ORDER extends TransactionModelId(109)
  case BILL_OF_DELIVERY extends TransactionModelId(110)
  case CUSTOMER_INVOICE extends TransactionModelId(111)
  case PAYABLES extends TransactionModelId(112)
  case PAYMENT extends TransactionModelId(114)
  case BANK extends TransactionModelId(118)
  case RECEIVABLES extends TransactionModelId(122)
  case SETTLEMENT extends TransactionModelId(124)
  case PAYMENT_SETTLEMENT extends TransactionModelId(125)
  case GENERAL_LEDGER extends TransactionModelId(134)
  case PAYROLL extends TransactionModelId(136)
  case CASH extends TransactionModelId(144)
  case STOCK_TRANSFER extends TransactionModelId(126)
  case CONSUMPTION extends TransactionModelId(127)
  case STOCK_TAKE extends TransactionModelId(128)

final case class Store(id: String,
                       name: String,
                       description: String,
                       costcenter: String,
                       account: String,
                       oaccount: String,
                       enterdate: Instant = Instant.now(),
                       changedate: Instant = Instant.now(),
                       postingdate: Instant = Instant.now(),
                       company: String,
                       modelid: Int = ModelId.STORE.modelid,
                       stocks:List[Stock]=List.empty[Stock])
object Store:
  type TYPE2 = (String, String, String, String, String, String, Int, String)
  def encodeIt2(st: Store): TYPE2 = (st.name, st.description, st.costcenter,  st.costcenter, st.oaccount, st.id, st.modelid, st.company)



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
                         account: String,
                         oaccount: String,
                         revenueAccount: String,
                         vatCode: String,
                         company: String,
                         modelid: Int = ModelId.ARTICLE.modelid,
                         enterdate: Instant = Instant.now(),
                         changedate: Instant = Instant.now(),
                         postingdate: Instant = Instant.now(),
                         bom: List[Bom] = List.empty[Bom],
                         stocks: List[Stock] = List.empty[Stock]
                        ) extends IWS
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
                          account: String,
                          oaccount: String,
                          revenueAccount: String,
                          vatCode: String,
                          company: String,
                          modelid: Int = ModelId.ARTICLE.modelid,
                          enterdate: Instant = Instant.now(),
                          changedate: Instant = Instant.now(),
                          postingdate: Instant = Instant.now(),
                          bom: List[Bom] = List.empty[Bom],
                          stocks: List[Stock] = List.empty[Stock]
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
  type TYPE2 = (scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, String, Int, String)
  private type Article_Type = (String, String, String, String, BigDecimal, BigDecimal, BigDecimal, String, Boolean, String,
    String, String, String, String, String, String, Int, Instant, Instant, Instant
    )
  type Article_Type3 = (String, String, String, String, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, String, Boolean, String, String, String, String, String, String, String, Int, LocalDateTime, LocalDateTime, LocalDateTime)
  type TYPE22 = (String, String, String, scala.math.BigDecimal,  scala.math.BigDecimal, scala.math.BigDecimal, String, Boolean, String, String, String, String, String, String, String, Int, String)
  
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
      acc._20,
      Nil,
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
    art.account,
    art.oaccount,
    art.revenueAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate,
    art.bom,
    art.stocks
  )

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
    art.account,
    art.oaccount,
    art.revenueAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate,
    art.bom,
    art.stocks
  )

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
    art.account,
    art.oaccount,
    art.revenueAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate,
    art.bom,
    art.stocks)

  def encodeIt(st: Article): Article_Type3 =
    (
      st.id,
      st.name,
      st.description,
      st.parent,
      scala.math.BigDecimal(st.sprice),
      scala.math.BigDecimal(st.pprice),
      scala.math.BigDecimal(st.avgPrice),
      st.currency,
      st.stocked,
      st.quantityUnit,
      st.packUnit,
      st.account,
      st.oaccount,
      st.revenueAccount,
      st.vatCode,
      st.company,
      st.modelid,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
    )
  def encodeIt2(st: Article): TYPE22 =
    (st.name, st.description, st.parent, st.sprice, st.pprice, st.avgPrice, st.currency, st.stocked, st.quantityUnit,
      st.packUnit, st.account, st.oaccount, st.revenueAccount, st.vatCode, st.id, st.modelid, st.company)

  def encodeIt3(st: Article):TYPE2 =
    (scala.math.BigDecimal(st.sprice), scala.math.BigDecimal(st.pprice), scala.math.BigDecimal(st.avgPrice), st.id, st.modelid, st.company)
  val dummy = Article("-1", "dummy",  "dummy", "", zeroAmount, zeroAmount, zeroAmount, "", false, "", "", "", "", "", ""
    , "-1", ModelId.ARTICLE.modelid, Instant.now(), Instant.now(), Instant.now())
}
final case class Bom(id:String, parent:String, quantity:BigDecimal, description:String, company:String, modelid: Int =ModelId.BOM.modelid)
object Bom:
  type TYPE2 =(String, String, scala.math.BigDecimal, String, Int, String)
  val dummy: Bom = Bom("-1", "", zeroAmount, "", "", ModelId.BOM.modelid)
  def encodeIt2(st:Bom):TYPE2 =(st.parent, st.description, st.quantity, st.id, st.modelid, st.company)


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
                          account: String,
                          oaccount: String,
                          balanceSheetAcc: String,
                          incomeStmtAcc: String,
                          purchasingClearingAcc:String,
                          salesClearingAcc:String,
                          cashAcc:String,
                          modelid: Int =ModelId.COMPANY.modelid,
                          bankaccounts: List[BankAccount] = List.empty[BankAccount],
                          enterdate: Instant = Instant.now(),
                          changedate: Instant = Instant.now(),
                          postingdate: Instant = Instant.now(),
                        )
object Company:
  type TYPE= (
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
      String,
      String,
      String,
      Int
    )
  type TYPE2 = (String, String, String, String, String, String, String, String, String, String, String, String, String,
    String, String, String, String, String, String, String, String, String, String, Int)
  type TYPE3=(String,String,String,String,String,String,String,String,String,String,String,String,String,String,String,String,String,String,String,String, String, Int)
  //def apply(c:TYPE):Company = Company(c._1, c._2,c._3,c._4,c._5,c._6,c._7,c._8,c._9,c._10
  //  ,c._11,c._12,c._13,c._14,c._15, c._16,c._17,c._18,c._19, c._20, c._21, c._22, c._23, List.empty[BankAccount])
  def dummy:Company = Company("-1",  "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ModelId.COMPANY.modelid, Nil)

  def encodeIt(st: Company): TYPE2 =
    (st.id, st.name,  st.street, st.zip, st.city, st.state, st.country, st.email, st.partner, st.phone, st.bankAcc,
      st.iban, st.taxCode, st.vatCode, st.currency, st.locale, st.balanceSheetAcc, st.incomeStmtAcc, st.purchasingClearingAcc,
      st.salesClearingAcc, st.cashAcc, st.account, st.oaccount, st.modelid)

  def encodeIt2(st: Company): TYPE2 =
    (st.name,  st.street, st.zip, st.city, st.state, st.country, st.email, st.partner, st.phone, st.bankAcc, st.iban
      , st.taxCode, st.vatCode, st.currency, st.locale, st.balanceSheetAcc, st.incomeStmtAcc, st.purchasingClearingAcc
      , st.salesClearingAcc, st.cashAcc, st.account, st.oaccount, st.id, st.modelid)


abstract class AppError
object AppError:
  final case class RepositoryError(message: String) extends AppError
  final case class DecodingError(message: String) extends AppError
  case class AuthenticationError(message: String, userId: Int) extends AppError

final case class Balance(id: String, idebit: BigDecimal, icredit: BigDecimal, debit: BigDecimal, credit: BigDecimal):
  def debiting(amount: BigDecimal): Balance = copy(debit = debit.add(amount))
  def crediting(amount: BigDecimal): Balance = copy(credit = credit.add(amount))
  def idebiting(amount: BigDecimal): Balance = copy(idebit = idebit.add(amount))
  def icrediting(amount: BigDecimal): Balance = copy(icredit = icredit.add(amount))
  object Balance {
    def apply(pac: PeriodicAccountBalance): Balance = new Balance(pac.account, pac.idebit, pac.icredit, pac.debit, pac.credit)
  }

trait AccountT extends IWS:
  //def parent:String
  def debiting(amount: BigDecimal):AccountT
  def crediting(amount: BigDecimal):AccountT
  def idebiting(amount: BigDecimal):AccountT
  def icrediting(amount: BigDecimal):AccountT
  def bdebiting (amount: BigDecimal): AccountT
  def bcrediting(amount: BigDecimal): AccountT
  def fdebit:BigDecimal
  def fcredit:BigDecimal
  def dbalance:BigDecimal
  def cbalance:BigDecimal
  def balance:BigDecimal
  def getBalance:Balance

final case class Account (
                           id: String,
                           name: String,
                           description: String,
                           enterdate: Instant = Instant.now(),
                           changedate: Instant = Instant.now(),
                           postingdate: Instant = Instant.now(),
                           company: String,
                           modelid: Int = ModelId.ACCOUNT.modelid,
                           account: String,
                           isDebit: Boolean,
                           balancesheet: Boolean,
                           currency: String,
                           idebit: BigDecimal = zeroAmount,
                           icredit: BigDecimal = zeroAmount,
                           debit: BigDecimal = zeroAmount,
                           credit: BigDecimal = zeroAmount,
                           bdebit: BigDecimal = zeroAmount,
                           bcredit: BigDecimal = zeroAmount,
                           subAccounts: Set[Account] = Nil.toSet
                         ) extends IWS { self:Account =>

  import com.kabasoft.iws.domain.common.{reduce, given}

  private def reportBalance(acc: Account):Unit =
    subAccounts.toList match
      case Nil => acc.idebiting(idebit).icrediting(icredit).debiting(debit).crediting(credit)
      case x1 :: xs => (x1 :: xs).foreach( _.reportBalance(self))

  def reportBalanceA(acc:Account): Account = {
    acc.subAccounts.toList match
      case Nil => acc
      case x1:: xs =>
          reportBalance(x1)
          xs.foreach(reportBalance)
          reduce(acc.subAccounts,  Account.dummy).copy(id = acc.id, name= acc.name, description = acc.description, account = acc.account )
  }
  def withPac(pac: Option[PeriodicAccountBalance]): Account =
     pac.map(p=>idebiting(p.idebit).icrediting(p.icredit).debiting(p.debit)
      .crediting(p.credit)).getOrElse(self)//.bdebiting(pac.bdebit).bcrediting(pac.bcredit)

  def withPac(pac: PeriodicAccountBalance): Account = idebiting(pac.idebit).icrediting(pac.icredit).debiting(pac.debit)
    .crediting(pac.credit)//.bdebiting(pac.bdebit).bcrediting(pac.bcredit)
  
  def withAccount(acc: Account):Account = idebiting(acc.idebit).icrediting(acc.icredit).debiting(acc.debit)
    .crediting(acc.credit)//.bdebiting(acc.bdebit).bcrediting(acc.bcredit)
  
  def report(child: List[Account]): Account =
    reduce(child.filter(acc=>acc.account == id).map(acc => acc.report(child)), Account.dummy)
    
  def debiting(amount: BigDecimal): Account = copy(debit = debit.add(amount))

  def crediting(amount: BigDecimal): Account = copy(credit = credit.add(amount))

  def idebiting(amount: BigDecimal): Account = copy(idebit = idebit.add(amount))

  def icrediting(amount: BigDecimal): Account = copy(icredit = icredit.add(amount))

  def bdebiting(amount: BigDecimal): Account = copy(bdebit = bdebit.add(amount))
  def bcrediting(amount: BigDecimal): Account = copy(bcredit = bcredit.add(amount))

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

  def updateBalance(acc: Account): Account = {
    idebiting(acc.idebit)
      .icrediting(acc.icredit)
      .debiting(acc.debit)
      .crediting(acc.credit)
      .bdebiting(acc.bcredit)
      .bcrediting(acc.bcredit)
      .remove(acc).add(acc)
  }

  //@tailrec

  @tailrec
  def updateBalanceParent( account:Account, all: List[Account]): List[Account] =
    all.find(acc => acc.id == account.account) match
      case Some(parent) =>
        val y: Account = parent.updateBalance(account)
        // replace the old parent with the updated one into the (all) list
        val z: List[Account] = all.filterNot(acc => acc.id == parent.id) :+ y
        //val newBalances: List[Account] = balances.filterNot(acc => acc.id == parent.id) :+ y
        y.updateBalanceParent(y, z)
      case None => all


  def getChildren: Set[Account] = subAccounts.toList match
    case Nil => Set(copy(id = id))
    case x :: xs => Set(x) ++ xs.flatMap(_.getChildren)

  def addSubAccounts(accounts: List[Account]): Account =
    copy(subAccounts = accounts.filter(_.account == id).map(_.addSubAccounts(accounts)).toSet)

}
object Account {
  import com.kabasoft.iws.domain.common.{reduce, given}
  type TYPE = (String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int, String, Boolean, Boolean
    , String, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal)
  //acc.name, acc.description, acc.account, acc.isDebit, acc.balancesheet, acc.currency,  acc.id, acc.modelid, acc.company
  type TYPE2 = (String, String, String, Boolean, Boolean, String, String, Int, String)
  private type Account_Type = (
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
      BigDecimal,
      BigDecimal,
      BigDecimal
    )
  private type Account_Tyoe3 =(String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int, String, Boolean, Boolean, String, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal , scala.math.BigDecimal, scala.math.BigDecimal)

  def apply(acc: Account_Type): Account =
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
      acc._17,
      acc._18,
      Nil.toSet
    )

  val dummy: Account = Account("", "", "", Instant.now(), Instant.now(), Instant.now(), "1000", ModelId.ACCOUNT.modelid, "X+"
    , false, false, "EUR", zeroAmount, zeroAmount, zeroAmount, zeroAmount, zeroAmount, zeroAmount, Nil.toSet)

  def encodeIt(acc: Account): Account_Tyoe3 =
    (
      acc.id,
      acc.name,
      acc.description,
      acc.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      acc.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      acc.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      acc.company,
      acc.modelid,
      acc.account,
      acc.isDebit,
      acc.balancesheet,
      acc.currency,
      scala.math.BigDecimal(acc.idebit),
      scala.math.BigDecimal(acc.icredit),
      scala.math.BigDecimal(acc.debit),
      scala.math.BigDecimal(acc.credit),
      scala.math.BigDecimal(acc.bdebit),
      scala.math.BigDecimal(acc.bcredit)
      
    )
  def encodeIt2(acc: Account):TYPE2 =
    (acc.name, acc.description, acc.account, acc.isDebit, acc.balancesheet, acc.currency,  acc.id, acc.modelid, acc.company)

  def group(accounts: List[Account]): List[Account] =
    accounts
      .groupBy(_.id)
      .map { case (k, v: List[Account]) => reduce(v, Account.dummy).copy(id = k) }
      .filterNot(_.id == Account.dummy.id)
      .toList

  def getParentIds(ids:List[String], all: List[Account], result:List[String]): List[String] = {
    //val parentIds = all.filter(x=>ids.contains(x.id)).map(_.account).distinct
    if(ids.isEmpty)  {
      result
    } else {
      val parentIds = ids.flatMap(x => all.filter(_.id == x)).map(_.account).distinct
      getParentIds(parentIds, all, result ++ parentIds) //.flatMap(id => all.filter(_.id == id))
    }

  }


  private def removeSubAccounts(account: Account): Account =
    account.subAccounts.toList match
      case Nil      => account
      case _ =>
        val sub = account.subAccounts.filterNot((acc => acc.balance.compareTo(new BigDecimal(0)) == 0 && acc.subAccounts.isEmpty))
        if (account.subAccounts.nonEmpty)
          account.copy(subAccounts = sub.map(removeSubAccounts))
        else account
  
  private def getInitialDebitCredit(accId: String, pacs: List[PeriodicAccountBalance], side: Boolean): BigDecimal =
    pacs.find(x => x.account == accId) match
      case Some(acc) => if (side) acc.idebit else acc.icredit
      case None      => zeroAmount

  private def getAllSubBalances(account: Account, pacs: List[PeriodicAccountBalance]): Account                    = 
    account.subAccounts.toList match
      case Nil      =>
        account.copy(idebit = getInitialDebitCredit(account.id, pacs, true), icredit = getInitialDebitCredit(account.id, pacs, false))
      case _ =>
        val sub    = account.subAccounts.map(acc => getAllSubBalances(acc, pacs))
        val subALl = reduce(sub, Account.dummy)
        account
          .idebiting(subALl.idebit)
          .icrediting(subALl.icredit)
          .debiting(subALl.debit)
          .crediting(subALl.credit)
          //.crediting(subALl.bdebit)
          //.crediting(subALl.bcredit)
          .copy(subAccounts = sub)
  
  private def unwrapData_(res: Set[Account]): Set[Account] =
    res.flatMap: acc =>
      acc.subAccounts.toList match
        case Nil => if (acc.balance.compareTo(zeroAmount) == 0) Set.empty[Account] else Set(acc)
        case (head: Account) :: tail => Set(acc, head) ++ unwrapData_(tail.toSet)
        
  def unwrapData(account: Account): List[Account] = List(account)++unwrapData_(account.subAccounts).toList

  def withChildren(accId: String, accList: List[Account]): Account =
    accList.find(x => x.id == accId) match
      case Some(acc) => List(acc).foldMap(addSubAccounts(_, accList.groupBy(_.account))).copy(id = accId)
      case None      => Account.dummy

  def consolidate(accId: String, accList: List[Account], pacs: List[PeriodicAccountBalance]): Account =
    val accMap = accList.groupBy(_.account)
    accList.find(_.id == accId) match
      case Some(acc) => updateSubAccountBalance(pacs, accMap, acc)
      case None => Account.dummy
      
  def consolidate(acc: Account, accList: List[Account], pacs: List[PeriodicAccountBalance]): Account =
    val accMap = accList.groupBy(_.account)
    updateSubAccountBalance(pacs, accMap, acc)
//    accList.find(_.id == acc.id) match
//      case Some(acc) => updateSubAccountBalance(pacs, accMap, acc)
//      case None      => Account.dummy
  private def addSubAccounts(account: Account, accMap: Map[String, List[Account]]): Account =
   accMap.get(account.id) match
    case Some(accList) => addSubAcc(account, accMap, accList)
    case None => if (account.subAccounts.isEmpty) account else {
        addSubAcc(account, accMap, account.subAccounts.toList)
      }  

  private def addSubAcc(account: Account, accMap: Map[String, List[Account]], accList: List[Account]) = {
    account.copy(subAccounts = accList.map(x => addSubAccounts(x, accMap)).toSet)
  }

  private def updateSubAccountBalance(pacs: List[PeriodicAccountBalance], accMap: Map[String, List[Account]], acc: Account) =
    val x: Account = addSubAccounts(acc, accMap) // List(acc)
    val y          = getAllSubBalances(x, pacs)
    removeSubAccounts(y.copy(id = acc.id))

  def flattenTailRec(ls: Set[Account]): Set[Account] =
    @tailrec
    def flattenR(res: List[Account], rem: List[Account]): List[Account] = rem match
      case Nil                     => res
      case (head: Account) :: tail => flattenR(res ++ List(head), head.subAccounts.toList ++ tail)
    flattenR(List.empty[Account], ls.toList).toSet

}

final case class Asset (id: String,
                        name: String,
                        description: String,
                        enterdate: Instant = Instant.now(),
                        changedate: Instant = Instant.now(),
                        postingdate: Instant = Instant.now(),
                        company: String,
                        modelid: Int = ModelId.ASSET.modelid,
                        account: String,
                        oaccount: String,
                        depMethod:Int,
                        amount:BigDecimal,
                        rate: BigDecimal,
                        lifeSpan:Int,
                        scrapValue: BigDecimal = zeroAmount,
                        frequency:Int,
                        currency: String)
object Asset:
  type TYPE=(String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int, String, String, Int, scala.math.BigDecimal, scala.math.BigDecimal, Int, scala.math.BigDecimal, Int, String)
  type TYPE2 =(String, String, String, String, Int, scala.math.BigDecimal, String, scala.math.BigDecimal, Int, scala.math.BigDecimal, Int, String, Int, String)
  def encodeIt(st: Asset): TYPE =
    (st.id, st.name, st.description, st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.company, st.modelid, st.account, st.oaccount, st.depMethod, st.amount, st.rate, st.lifeSpan, st.scrapValue
      , st.frequency, st.currency
    )
  def encodeIt2(st: Asset): TYPE2=
    (st.name, st.description, st.account, st.oaccount, st.depMethod, st.amount, st.currency, st.rate, st.lifeSpan
      , st.scrapValue, st.frequency, st.id, st.modelid, st.company)


sealed trait IWS:
  def id: String
  def modelid:Int
  def company:String

final case class Masterfile(id: String,
                            name: String = "",
                            description: String = "",
                            parent: String = "",
                            enterdate: Instant = Instant.now(),
                            changedate: Instant = Instant.now(),
                            postingdate: Instant = Instant.now(),
                            modelid: Int,
                            company: String
                           ) extends IWS
object Masterfile:
  type TYPE=(String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  type TYPE2=(String, String, String, String, Int, String)
  def encodeIt(st: Masterfile):TYPE =
    (st.id, st.name, st.description, st.parent,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company, st.modelid)

  def encodeIt2(st: Masterfile):TYPE2  = (st.name, st.description, st.parent, st.id, st.modelid, st.company)
  def encodeIt3(st: Masterfile):(String, Int, String)  = ( st.id, st.modelid, st.company)

object AccountClass:
  val dummy  = AccountClass("", "", "", "", Instant.now(), Instant.now(), Instant.now(), ModelId.ACCOUNT_CLASS.modelid,
    "1000", true, zeroAmount, zeroAmount, zeroAmount, zeroAmount)

final case  class AccountClass ( id: String,
                                 name: String = "",
                                 description: String = "",
                                 parent: String = "",
                                 enterdate: Instant = Instant.now(),
                                 changedate: Instant = Instant.now(),
                                 postingdate: Instant = Instant.now(),
                                 modelid: Int = ModelId.ACCOUNT_CLASS.modelid,
                                 company: String,
                                 isDebit: Boolean,
                                 idebit: BigDecimal = zeroAmount,
                                 icredit: BigDecimal = zeroAmount,
                                 debit: BigDecimal = zeroAmount,
                                 credit: BigDecimal = zeroAmount,
                                 bdebit: BigDecimal = zeroAmount,
                                 bcredit: BigDecimal = zeroAmount,                              
                               ) extends  AccountT{

  import com.kabasoft.iws.domain.common.{reduce, given}
  def report(child: List[AccountClass]): AccountClass =
    reduce(child.filter(acc=>acc.parent==id).map(acc => acc.report(child)), dummy)

  def debiting(amount: BigDecimal): AccountClass = copy(debit = debit.add(amount))
  def crediting(amount: BigDecimal): AccountClass = copy(credit = credit.add(amount))
  def idebiting(amount: BigDecimal): AccountClass = copy(idebit = idebit.add(amount))
  def icrediting(amount: BigDecimal): AccountClass = copy(icredit = icredit.add(amount))
  def bdebiting(amount: BigDecimal): AccountClass = copy(bdebit = bdebit.add(amount))
  def bcrediting(amount: BigDecimal): AccountClass = copy(bcredit = bcredit.add(amount))
  def fdebit: BigDecimal = debit.add(idebit)
  def fcredit: BigDecimal = credit.add(icredit)
  def dbalance: BigDecimal = fdebit.subtract(fcredit)
  def cbalance: BigDecimal = fcredit.subtract(fdebit)
  def balance: BigDecimal = if (isDebit) dbalance else cbalance
  def getBalance: Balance = Balance(id, idebit, icredit, debit, credit)
}

final case class ImportFile( id: String,
                             name: String,
                             description: String,
                             extension: String,
                             enterdate: Instant = Instant.now(),
                             changedate: Instant = Instant.now(),
                             postingdate: Instant = Instant.now(),
                             modelid: Int = ModelId.IMPORT_FILE.modelid,
                             company: String) extends IWS
object ImportFile:
  type TYPE = (String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  type TYPE2 = (String, String, String, String, Int, String)
  def encodeIt(st: ImportFile):TYPE =
    (st.id, st.name, st.description, st.extension,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company, st.modelid)
  def encodeIt2(st: ImportFile):TYPE2 = (st.name, st.description, st.extension, st.id, st.modelid, st.company)

final case class SalaryItem(id: String,
                            name: String = "",
                            description: String = "",
                            account:String,
                            amount:BigDecimal,
                            percentage:BigDecimal,
                            enterdate: Instant = Instant.now(),
                            changedate: Instant = Instant.now(),
                            postingdate: Instant = Instant.now(),
                            modelid: Int = ModelId.SALARY_ITEM.modelid,
                            company: String
                           ) extends IWS
object SalaryItem:
  type TYPE = (String, String, String, String, scala.math.BigDecimal, scala.math.BigDecimal, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  type TYPE2 = (String, String, String, scala.math.BigDecimal, scala.math.BigDecimal, String, Int, String)
  def encodeIt(st: SalaryItem): TYPE =
    (st.id, st.name, st.description, st.account, st.amount, st.percentage,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company, st.modelid)
  def encodeIt2(st: SalaryItem): TYPE2 =
    (st.name, st.description, st.account, st.amount, st.percentage, st.id, st.modelid, st.company)

final case class PayrollTaxRange (id: String, fromAmount:BigDecimal, toAmount:BigDecimal, tax:BigDecimal, taxClass:String, modelid: Int = ModelId.PAYROLL_TAX_RANGE.modelid, company: String)
object PayrollTaxRange:
  type TYPE = (String, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, String, String, Int)
  def encodeIt(st: PayrollTaxRange): TYPE = (st.id, st.fromAmount, st.toAmount, st.tax, st.taxClass, st.company, st.modelid)

final case class EmployeeSalaryItem(id: String, owner: String, account: String, amount: BigDecimal, percentage: BigDecimal, text:String, company: String)
object EmployeeSalaryItem:
  type  TYPE =(String, String, scala.math.BigDecimal, scala.math.BigDecimal, String, String, String)
  def encodeIt(st: EmployeeSalaryItem): TYPE = (st.owner,  st.account, st.amount, st.percentage, st.text, st.id, st.company)
  def apply(item:EmployeeSalaryItemDTO):EmployeeSalaryItem =EmployeeSalaryItem(item.id, item.owner, item.account, item.amount, item.percentage, item.text, item.company)

final case class EmployeeSalaryItemDTO(id: String, owner: String, account: String, accountName: String, amount: BigDecimal, percentage: BigDecimal, text:String, company: String)

object EmployeeSalaryItemDTO:
  def apply(item:EmployeeSalaryItem):EmployeeSalaryItemDTO =EmployeeSalaryItemDTO(item.id, item.owner, item.account, "", item.amount, item.percentage, item.text, item.company)

final case class BankAccount(id: String, bic: String, owner: String, company: String, modelid: Int =  ModelId.BANK_ACCOUNT.modelid )
object BankAccount:
  import scala.math.Ordering
  type TYPE = (String, String, String, String, Int)
  type TYPE2 = (String, String, String, Int, String)

  //given Ordering[A <: BankAccount]: Ordering[A] = Ordering.by(e => (e.id, e.bic, e.owner, e.company))
  implicit def ordering[A <: BankAccount]: Ordering[A] = Ordering.by(e => (e.id, e.bic, e.owner, e.company))
  def encodeIt(st: BankAccount):TYPE = (st.id, st.bic, st.owner, st.company, st.modelid)
  def encodeIt2(st: BankAccount):TYPE2 = (st.bic, st.owner, st.id, st.modelid, st.company)
  def encodeIt3 (st: BankAccount):(String, String, String, String) = (st.id, st.bic, st.owner, st.company)

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
                                modelid: Int =  ModelId.BANK_STATEMENT.modelid,
                                period: Int //= common.getPeriod(Instant.now())
                              )
object BankStatement  {
  val CENTURY         = "20"
  val zoneId          = ZoneId.of("Europe/Berlin")
  val DATE_FORMAT     = "dd.MM.yyyy"
  val FIELD_SEPARATOR = ';'
  val NUMBER_FORMAT   = NumberFormat.getInstance(Locale.GERMAN)
  val dummy:BankStatement=BankStatement(0L, "", Instant.now(), Instant.now(), "", "", "", "", "", BigDecimal.ZERO, "", "", "", "", false, ModelId.BANK_STATEMENT.modelid, 0)
  type TYPE = (Long, String, LocalDateTime, LocalDateTime, String, String, String, String, String, scala.math.BigDecimal, String, String, String, String, Boolean, Int, Int)
  type TYPE4 = (String, LocalDateTime, LocalDateTime, String, String, String, String, String, scala.math.BigDecimal, String, String, String, String, Boolean, Int, Int)
  type TYPE2 = (Boolean, Int, Long, Int, String)
  type TYPE3 = (LocalDateTime, String, String, Int, Boolean, Long, Int, String)
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

  def fullDate(partialDate: String): Instant =
    val index    = partialDate.lastIndexOf(".")
    val pYear    = partialDate.substring(index + 1)
    val year    = if (pYear.length ==2) CENTURY.concat(pYear) else pYear
    val fullDate = partialDate.substring(0, index + 1).concat(year) //concat(CENTURY.concat(pYear))
    LocalDate
      .parse(fullDate, DateTimeFormatter.ofPattern(DATE_FORMAT))
      .atStartOfDay(zoneId)
      .toInstant

  def from(s: String, company:String): BankStatement = {
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
      company,
      companyIban,
      posted,
      ModelId.BANK_STATEMENT.modelid,
      period
    )
    println ("BankStatement>>"+bs)
    bs
  }

  def encodeIt(st: BankStatement): TYPE =
    (st.id, st.depositor
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.valuedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingtext, st.purpose, st.beneficiary, st.accountno
      , st.bankCode, scala.math.BigDecimal(st.amount), st.currency, st.info, st.company
      , st.companyIban, st.posted, st.modelid, st.period
    )
  def encodeIt4(st: BankStatement): TYPE4 =
    (st.depositor
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.valuedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingtext, st.purpose, st.beneficiary, st.accountno
      , st.bankCode, scala.math.BigDecimal(st.amount), st.currency, st.info, st.company
      , st.companyIban, st.posted, st.modelid, st.period
    )
  def encode(st: BankStatement):TYPE2 = (st.posted, common.getPeriod(st.valuedate), st.id, st.modelid, st.company)
  def encodeIt2(st: BankStatement):TYPE3 = (st.valuedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
    , st.accountno, st.bankCode, common.getPeriod(st.valuedate), st.posted, st.id, st.modelid, st.company)
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
                         modelid: Int = ModelId.MODULE.modelid,
                         company: String
                       )
object Module:
  type TYPE=(String, String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  type TYPE2=(String, String, String, String, String, Int, String)
  def encodeIt(st: Module): TYPE =
    (st.id, st.name, st.description, st.path, st.parent,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company,
      st.modelid)
  def encodeIt2(st: Module): TYPE2 = (st.name, st.description, st.path, st.parent, st.id, st.modelid, st.company)
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
                      modelid: Int = ModelId.VAT.modelid
                    ) extends IWS
object Vat:
  val dummy: Vat = Vat("", "", "", zeroAmount, "", "", Instant.now(), Instant.now(), Instant.now(), "", ModelId.VAT.modelid)

  def encodeIt(st: Vat): (String, String, String, scala.BigDecimal, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int) =
    (st.id,
      st.name,
      st.description,
      st.percent,
      st.inputVatAccount,
      st.outputVatAccount,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company,
      st.modelid
    )

  def encodeIt2(st: Vat): (String, String, scala.BigDecimal, String, String,  String, Int, String) =
    (st.name, st.description, st.percent, st.inputVatAccount, st.outputVatAccount, st.id, st.modelid, st.company)

final case class Stock(id:String, store:String, article:String, quantity:BigDecimal,  price: BigDecimal
                       , charge:String, company:String, modelid: Int = ModelId.STOCK.modelid)
final case class TStock(id:String, store:String, article:String, quantity:TRef[BigDecimal], price:BigDecimal
                        , charge:String, company:String, modelid: Int = ModelId.STOCK.modelid) {
  self =>
  def transfer(from: TStock,  quantity: BigDecimal): ZIO[Any, RepositoryError, Unit] = {
    self.quantity.get.commit.map(m=>m.add(quantity)).map(m =>if (m.signum()<0) RepositoryError(s"Negative stock ${quantity}") else ())
    STM.atomically {
      for {
        _             <- self.quantity.update(_.add(quantity))
        _             <- from.quantity.update(_.subtract(quantity))
      } yield ()
    }
  }

  def add( quantity: BigDecimal): ZIO[Any, RepositoryError, Unit] =
    STM.atomically {
      for {
        _            <- self.quantity.update(_.add(quantity))
      } yield ()
    }

  def substract( quantity: BigDecimal): ZIO[Any, RepositoryError, Unit] =
    STM.atomically {
      for {
        _            <- self.quantity.update(_.subtract(quantity))
      } yield ()
    }

  def multiply( quantity: BigDecimal): ZIO[Any, RepositoryError, Unit] =
    STM.atomically {
      for {
        _            <- self.quantity.update(_.multiply(quantity))
      } yield () 
    }
}

object Stock {
  import com.kabasoft.iws.domain.common.given
  val dummy: Stock =   make("-1", "-1", zeroAmount, zeroAmount, "", "-1")
  type TYPE2 = (scala.math.BigDecimal, String)
  type TYPE3 = (scala.math.BigDecimal, String, String, Int, String)
  type TYPE4 = (String, String, String, scala.math.BigDecimal, String, String, Int)
  type TYPE = (String, String, String, scala.math.BigDecimal, scala.math.BigDecimal, String, String, Int)
  private type STOCK_Type = (String, String, String,  BigDecimal, BigDecimal, String, String, Int)
  def buildId(store:String, article:String, charge:String, company:String): String = store.concat(article).concat(company).concat(charge)
  def make (store:String, article:String, quantity:BigDecimal, price:BigDecimal, charge:String, company:String): Stock =
    Stock( buildId(store, article,  charge, company), store, article, quantity, price, charge, company, ModelId.STOCK.modelid)
  def apply(stock: STOCK_Type): Stock = Stock(stock._1, stock._2, stock._3, stock._4, stock._5, stock._6, stock._7, stock._8)

  def apply(stock: TStock): ZIO[Any, Nothing, Stock] = for {
    quantity_  <- stock.quantity.get.commit
  } yield Stock(stock.id, stock.store, stock.article, quantity_, stock.price, stock.charge,  stock.company, ModelId.STOCK.modelid)

  def create(model: Transaction): List[Stock] =
    model.lines.map(line => Stock.make(model.store, line.article, line.quantity, line.price, "", model.company))

  def create(models: List[Transaction]): List[Stock] =
    val x = models.flatMap(m=>m.lines.map(line => Stock.make(m.store, line.article, line.quantity, line.price, "", m.company)))
    groupByStock( x).toList

  private def groupByStock(r: List[Stock]) =
    (r.groupBy(st=>st.article.concat(st.store).concat(st.company)) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).toList

  def encodeIt(st: Stock): TYPE =
    (st.id, st.store, st.article, st.quantity, scala.math.BigDecimal(0), st.charge, st.company, st.modelid)

  def encodeIt4(st: Stock): TYPE4 = (st.id, st.store, st.article, st.quantity,  st.charge, st.company, st.modelid)

  def encodeIt2(st: Stock): TYPE2 = (st.quantity, st.id)
  def encodeIt3(st: Stock): TYPE3 = (st.quantity, st.charge, st.id, st.modelid, st.company)

}
object TStock:
  def make (store:String, article:String, quantity:BigDecimal, price:BigDecimal, charge:String, company:String):  UIO[TStock] =
    apply(Stock(Stock.buildId(store, article,  charge, company), store, article, quantity, price, charge, company))

  def apply(stock: Stock): UIO[TStock] = for {
    quantity  <- TRef.makeCommit(stock.quantity)
  } yield TStock(stock.id, stock.store, stock.article, quantity, stock.price, stock.charge,  stock.company, stock.modelid)

  def fromStockAndQuantity(stock: Stock, quantity:BigDecimal): ZIO[Any, RepositoryError, TStock] =
    TRef.makeCommit(stock.quantity.add(quantity)).flatMap( quantity_ =>
      if (quantity.add(stock.quantity).compareTo (zeroAmount) >= 0.00)
        ZIO.succeed(TStock(stock.id, stock.store, stock.article, quantity_, stock.price, stock.charge,  stock.company, stock.modelid))
      else ZIO.fail(RepositoryError(s"Negative stock ${quantity.add(stock.quantity)}"))
    )


final case class TPeriodicAccountBalance(
                                          id: String,
                                          account: String,
                                          period: Int,
                                          idebit: TRef[BigDecimal],
                                          icredit: TRef[BigDecimal],
                                          debit: TRef[BigDecimal],
                                          credit: TRef[BigDecimal],
                                          bdebit: TRef[BigDecimal],
                                          bcredit: TRef[BigDecimal],
                                          currency: String,
                                          company: String,
                                          name: String,
                                          modelid: Int = ModelId.PERIODIC_ACCOUNT_BALANCE.modelid
                                        ) {
  self =>
  def transfer(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance): ZIO[Any, Nothing, Unit] =
    STM.atomically {
      for {
        fidebit <- from.idebit.get
        ficredit <- from.icredit.get
        fdebit <- from.debit.get
        fcredit <- from.credit.get
        fbdebit <- from.bdebit.get
        fbcredit <- from.bcredit.get
        _ <- to.idebit.update(_.add(fidebit))
        _ <- to.icredit.update(_.add(ficredit))
        _ <- to.debit.update(_.add(fdebit))
        _ <- to.credit.update(_.add(fcredit))
        _ <- to.bdebit.update(_.add(fbdebit))
        _ <- to.bcredit.update(_.add(fbcredit))
      } yield ()
    }
}

object TPeriodicAccountBalance:
  val dummy = apply(PeriodicAccountBalance.dummy)
  def apply(pac: PeriodicAccountBalance): UIO[TPeriodicAccountBalance] = for {
    idebit  <- TRef.makeCommit(pac.idebit)
    icredit <- TRef.makeCommit(pac.icredit)
    debit   <- TRef.makeCommit(pac.debit)
    credit  <- TRef.makeCommit(pac.credit)
    bdebit   <- TRef.makeCommit(pac.bdebit)
    bcredit  <- TRef.makeCommit(pac.bcredit)
  } yield TPeriodicAccountBalance(pac.id, pac.account, pac.period, idebit, icredit, debit, credit, bdebit, bcredit, pac.currency, pac.company, pac.name, pac.modelid)

  def create(model: FinancialsTransaction): List[PeriodicAccountBalance] =
    model.lines.flatMap: line =>
      val debited = PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, line.account),
        line.account,
        model.period,
        zeroAmount,
        zeroAmount,
        line.amount,
        zeroAmount,
        line.amount,
        zeroAmount,
        line.currency,
        model.company,
        line.accountName,
        ModelId.PERIODIC_ACCOUNT_BALANCE.modelid
      )
      val credited = PeriodicAccountBalance(
        PeriodicAccountBalance.createId(model.period, line.oaccount),
        line.oaccount,
        model.period,
        zeroAmount,
        zeroAmount,
        zeroAmount,
        line.amount,
        zeroAmount,
        line.amount,
        line.currency,
        model.company,
        line.oaccountName,
        ModelId.PERIODIC_ACCOUNT_BALANCE.modelid)
      List(debited, credited)

  def transferX(from: TPeriodicAccountBalance, to: TPeriodicAccountBalance, amount: BigDecimal): IO[Nothing, Unit] = {
    STM.atomically {
      for {
        _ <- from.credit.update(_.subtract(amount))
        _ <- to.debit.update(_.add(amount))
        _ <- from.bcredit.update(_.subtract(amount))
        _ <- to.bdebit.update(_.add(amount))
      } yield ()
    }
  }
final case class ReminderBalance (id: String, period: Int, balance: BigDecimal, modelid:Int = ModelId.REMINDER_BALANCE.modelid)
final case class PeriodicAccountBalance(
                                         id: String,
                                         account: String,
                                         period: Int,
                                         idebit: BigDecimal,
                                         icredit: BigDecimal,
                                         debit: BigDecimal,
                                         credit: BigDecimal,
                                         bdebit: BigDecimal,
                                         bcredit: BigDecimal,
                                         currency: String,
                                         company: String,
                                         name: String,
                                         modelid: Int = ModelId.PERIODIC_ACCOUNT_BALANCE.modelid) {
  def debiting(amount: BigDecimal): PeriodicAccountBalance = copy(debit = debit.add(amount))
  def crediting(amount: BigDecimal): PeriodicAccountBalance = copy(credit = credit.add(amount))
  def idebiting(amount: BigDecimal): PeriodicAccountBalance = copy(idebit = idebit.add(amount))
  def icrediting(amount: BigDecimal): PeriodicAccountBalance = copy(icredit = icredit.add(amount))
  def bdebiting(amount: BigDecimal): PeriodicAccountBalance = copy(bdebit = bdebit.add(amount))
  def bcrediting(amount: BigDecimal): PeriodicAccountBalance = copy(bcredit = bcredit.add(amount))
  def fdebit: BigDecimal = debit.add(idebit)
  def fcredit: BigDecimal = credit.add(icredit)
  def dbalance: BigDecimal = fdebit.subtract(fcredit)
  def cbalance: BigDecimal = fcredit.subtract(fdebit)
  override def equals(other: Any): Boolean = other match
    case pac: PeriodicAccountBalance => this.id == pac.id
    case _                           => false


}

object PeriodicAccountBalance:
  import com.kabasoft.iws.domain.common.given
  type TYPE2 = (scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, String, Int, String)
  type TYPE = (String, String, Int, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, String, String, String, Int)
  def createId(period: Int, accountId: String) = period.toString.concat(accountId)
  val dummy                                    =
    PeriodicAccountBalance("-1", "", 0, zeroAmount, zeroAmount, zeroAmount, zeroAmount, zeroAmount, zeroAmount, "", "", "")

  def create(accountId: String, period: Int, currency: String, company: String, name: String): PeriodicAccountBalance =
    PeriodicAccountBalance.apply(
      PeriodicAccountBalance.createId(period, accountId),
      accountId,
      period,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      zeroAmount,
      currency,
      company,
      name,
      ModelId.PERIODIC_ACCOUNT_BALANCE.modelid
    )
  def create(model: FinancialsTransaction): List[PeriodicAccountBalance] =
    createx(model).groupBy(_.id).map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}.toList

  def createx(model: FinancialsTransaction): List[PeriodicAccountBalance] =
    model.lines.flatMap: line =>
      List(
        PeriodicAccountBalance.apply(
          PeriodicAccountBalance.createId(model.period, line.account),
          line.account,
          model.period,
          zeroAmount,
          zeroAmount,
          line.amount,
          zeroAmount,
          line.amount,
          zeroAmount,
          line.currency,
          model.company,
          line.accountName,
          ModelId.PERIODIC_ACCOUNT_BALANCE.modelid
        ),
        PeriodicAccountBalance.apply(
          PeriodicAccountBalance.createId(model.period, line.oaccount),
          line.oaccount,
          model.period,
          zeroAmount,
          zeroAmount,
          zeroAmount,
          line.amount,
          zeroAmount,
          line.amount,
          line.currency,
          model.company,
          line.oaccountName,
          ModelId.PERIODIC_ACCOUNT_BALANCE.modelid
        )
      )

  def applyT(tpac: TPeriodicAccountBalance): ZIO[Any, Nothing, PeriodicAccountBalance] = for {
    idebit  <- tpac.idebit.get.commit
    icredit <- tpac.icredit.get.commit
    debit   <- tpac.debit.get.commit
    credit  <- tpac.credit.get.commit
    bdebit   <- tpac.bdebit.get.commit
    bcredit  <- tpac.bcredit.get.commit
  } yield PeriodicAccountBalance(tpac.id, tpac.account, tpac.period, idebit, icredit, debit, credit, bdebit, bcredit, tpac.currency, tpac.company, tpac.name, tpac.modelid)

  def encodeIt(st: PeriodicAccountBalance): TYPE =
    (st.id, st.account, st.period, st.idebit, st.icredit, st.debit, st.credit, st.bdebit, st.bcredit, st.currency, st.company, st.name, st.modelid)

  def encodeIt2(st: PeriodicAccountBalance): TYPE2 = ( st.idebit, st.icredit, st.debit, st.credit, st.bdebit, st.bcredit, st.id, st.period, st.company)

final case class Partner (id: String,
                           name: String,
                           description: String,
                           street: String,
                           zip: String,
                           city: String,
                           state: String,
                           country: String,
                           phone: String,
                           email: String,
                           company: String,
                           modelid: Int = ModelId.PARTNER.modelid,
                           enterdate: Instant = Instant.now(),
                           changedate: Instant = Instant.now(),
                           postingdate: Instant = Instant.now()
                         )
object Partner:
  type TYPE = (String, String, String, String, String, String, String, String, String, String, String, Int, LocalDateTime, LocalDateTime, LocalDateTime)
  type TYPE2 = (String, String, String, String, String, String, String, String, String, String, Int, String)

  def encodeIt(st: Partner): TYPE =
    (st.id, st.name, st.description, st.street, st.zip, st.city, st.state, st.country, st.phone, st.email, st.company
      , st.modelid
      , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      )

  def encodeIt2(st: Partner): TYPE2 = (st.name, st.description, st.street, st.zip, st.city, st.state, st.country
        , st.phone, st.email, st.id, st.modelid, st.company)

sealed trait BusinessPartner:
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
  def taxCode: String
  def vatCode: String
  def currency: String
  def company: String
  def modelid: Int
  def enterdate: Instant
  def changedate: Instant
  def postingdate: Instant
  def bankaccounts: List[BankAccount]

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
                           taxCode:String,
                           vatCode: String,
                           currency: String,
                           company: String,
                           modelid: Int = ModelId.SUPPLIER.modelid,
                           enterdate: Instant = Instant.now(),
                           changedate: Instant = Instant.now(),
                           postingdate: Instant = Instant.now(),
                           bankaccounts: List[BankAccount] = List.empty[BankAccount]
                         ) extends BusinessPartner
object Supplier                      {
  type TYPE2 = (String, String, String, String, String, String, String, String, String, String, String, String
    , String, String, String, String, Int, LocalDateTime, LocalDateTime, LocalDateTime)
  type TYPE3 = (String, String, String, String, String, String, String, String, String, String, String, String
    , String, String, String, Int, String)
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
      c._20,
      List.empty[BankAccount]
    )

  def encodeIt(st: Supplier): TYPE2 =
    (st.id,
      st.name,
      st.description,
      st.street, st.zip, st.city, st.state, st.country, st.phone, st.email, st.account, st.oaccount
      , st.taxCode, st.vatCode, st.currency, st.company, st.modelid,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
    )

  def encodeIt2(st: Supplier): TYPE3 =
    (st.name, st.description, st.street, st.zip, st.city, st.state, st.country, st.phone, st.email, st.account, st.oaccount
      , st.taxCode, st.vatCode, st.currency, st.id, st.modelid, st.company)
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
                           taxCode: String,
                           vatCode: String,
                           currency: String,
                           company: String,
                           modelid: Int = ModelId.CUSTOMER.modelid,
                           enterdate: Instant = Instant.now(),
                           changedate: Instant = Instant.now(),
                           postingdate: Instant = Instant.now(),
                           bankaccounts: List[BankAccount] = List.empty[BankAccount]
                         ) extends BusinessPartner
object Customer                      {
  type TYPE2 = (String, String, String, String, String, String, String, String, String, String, String, String
    , String, String, String, String, Int, LocalDateTime, LocalDateTime, LocalDateTime)
  type TYPE3 = (String, String, String, String, String, String, String, String, String, String, String
    , String, String, String, String,Int, String)
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
      c._20,
      List.empty[BankAccount]
    )

  def encodeIt(st: Customer): TYPE2 =
    (st.id, st.name, st.description, st.street, st.zip, st.city, st.state, st.country, st.phone, st.email,
      st.account, st.oaccount, st.taxCode, st.vatCode, st.currency, st.company, st.modelid,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
    )
  def encodeIt2(st: Customer): TYPE3 =
    ( st.name, st.description, st.street, st.zip, st.city, st.state, st.country, st.phone, st.email, st.account, st.oaccount
      , st.taxCode, st.vatCode, st.currency, st.id, st.modelid, st.company)
}

final case class Employee(id: String, name: String, description: String, street: String, zip: String, city: String
                          , state: String, country: String, phone: String, email: String, account: String, oaccount: String
                          , taxCode: String, vatCode: String, currency: String, company: String, salary:BigDecimal, modelid: Int = ModelId.EMPLOYEE.modelid
                          , enterdate: Instant = Instant.now(), changedate: Instant = Instant.now(), postingdate: Instant = Instant.now()
                          , bankaccounts: List[BankAccount] = List.empty[BankAccount]
                          , salaryItems: List[EmployeeSalaryItemDTO] = List.empty[EmployeeSalaryItemDTO]
                         ) extends BusinessPartner
object Employee:
  type TYPE2 = (String, String, String, String, String, String, String, String, String, String, String, String, String, String
    , String,  String, scala.math.BigDecimal, Int, LocalDateTime, LocalDateTime, LocalDateTime)
  type TYPE3 = (String, String, String, String, String, String, String, String, String, String, String, String
    , String, String, scala.math.BigDecimal, String, Int, String)
  type TYPE = (String, String, String, String, String, String, String, String, String, String, String, String, String, String,
    String, String, BigDecimal, Int, Instant, Instant, Instant)

  def encodeIt(st: Employee):TYPE2 =
    (st.id, st.name, st.description, st.street, st.zip, st.city
      , st.state, st.country, st.phone, st.email, st.account
      , st.oaccount, st.taxCode, st.vatCode, st.currency, st.company, st.salary, st.modelid
      , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime)

  def encodeIt2(st: Employee): TYPE3 =
    (st.name, st.description, st.street, st.zip, st.city, st.state, st.country, st.phone, st.email, st.account, st.oaccount
      , st.taxCode, st.vatCode, st.currency, st.salary, st.id, st.modelid, st.company)

final case class TransactionDetails( id: Long, transid: Long, article: String, articleName:String, quantity: BigDecimal, unit: String, price: BigDecimal,
                                     currency: String, duedate: Instant = Instant.now(), vatCode:String, vat:BigDecimal, text: String, company: String, modelid:Int) {
  def amount: BigDecimal =quantity.multiply(price)
}

object TransactionDetails:
  val dummy = TransactionDetails(0, 0, "", "", zeroAmount, "", zeroAmount, "", Instant.now(), "", zeroAmount, "",  "", -1)
  private type D_TYPE = (Long, Long, String, String, scala.math.BigDecimal, String, scala.math.BigDecimal, String, LocalDateTime, String, scala.math.BigDecimal, String, String, Int)
  private type D_TYPE1 = (Long, String, String, scala.math.BigDecimal, String, scala.math.BigDecimal, String, LocalDateTime,  String, scala.math.BigDecimal, String, String, Int)
  type TYPE2 = (Long, String, scala.math.BigDecimal, String, scala.math.BigDecimal, String, LocalDateTime, String, String, String, scala.math.BigDecimal, Long, String, Int)
  implicit val monoid: Identity[TransactionDetails] =
    new Identity[TransactionDetails]:
      def identity: TransactionDetails = dummy
      def combine(m1: => TransactionDetails, m2: => TransactionDetails): TransactionDetails =
        m2.copy(quantity = m2.quantity.add(m1.quantity))

  def encodeIt(dt: TransactionDetails): D_TYPE =
    (dt.id, dt.transid, dt.article, dt.articleName, dt.quantity, dt.unit, dt.price, dt.currency
      , dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, dt.vatCode, dt.vat, dt.text,  dt.company, dt.modelid)
      //id, transid, article, article_name, quantity, unit, price, currency, duedate, vat_code , vat, text, company, modelid

  def encodeIt2(dt: TransactionDetails): TYPE2 =
    (dt.transid, dt.article, dt.quantity, dt.unit, dt.price, dt.currency, dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , dt.text, dt.articleName, dt.vatCode, dt.vat,  dt.id, dt.company, dt.modelid)

  def encodeIt3(dt: TransactionDetails): (Long, String, Int) = (dt.id, dt.company, dt.modelid)

  def encodeIt4(dt: TransactionDetails): D_TYPE1 =
    (dt.transid, dt.article, dt.articleName, dt.quantity, dt.unit, dt.price, dt.currency
      , dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, dt.vatCode, dt.vat, dt.text, dt.company, dt.modelid)

  private type TransactionDetails_Type = (Long, Long, String, String, BigDecimal, String, BigDecimal, String, Instant,  String, BigDecimal, String, String, Int)

  def apply(tr: TransactionDetails_Type): TransactionDetails =
    new TransactionDetails(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11,  tr._12, tr._13, tr._14)
  def apply(tr: TransactionDetails): TransactionDetails =
    new TransactionDetails(tr.id, tr.transid, tr.article, tr.articleName, tr.quantity, tr.unit, tr.price, tr.currency
      , tr.duedate, tr.vatCode, tr.vat, tr.text, tr.company, tr.modelid)


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
                                               company: String,
                                               accountName: String,
                                               oaccountName: String,
                                               modelid:Int
                                             )
trait Trans [A] {
  def lines: List[A]
}

final case class Transaction(id: Long,
                             oid: Long,
                             id1: Long,
                             store: String,
                             account: String,
                             transdate: Instant = Instant.now(),
                             enterdate: Instant = Instant.now(),
                             postingdate: Instant = Instant.now(),
                             period: Int = common.getPeriod(Instant.now()),
                             posted: Boolean = false,
                             modelid: Int,
                             company: String,
                             text: String = "",
                             footText: String = "",
                             lines: List[TransactionDetails] = Nil
                            ) extends Trans [TransactionDetails] {
  def month: String = common.getMonthAsString(transdate)
  def year: Int     = common.getYear(transdate)
  def getPeriod: Int = common.getPeriod(transdate)
  def total: BigDecimal = lines.map( l=>l.price.multiply(l.quantity)) reduce ((l1, l2) => l2.add(l1).setScale(2, RoundingMode.HALF_UP))
  def vat: BigDecimal = lines.map( l=>l.price.multiply(l.quantity).add(l.vat)) reduce ((l1, l2) => l2.add(l1).setScale(2, RoundingMode.HALF_UP))
  def cancel: Transaction = copy(oid = id, id = 0, posted = false, lines=lines.map(line=>line.copy( quantity = line.quantity.negate())))
  def duplicate: Transaction = copy(oid = id, id = 0, posted = false, lines=lines.map(line=>line.copy( id = 0, transid=0)) )
}
object Transaction:
  type TYPE = (Long, Long, Long, String, String, OffsetDateTime, OffsetDateTime, OffsetDateTime, Int, Boolean, Int, String, String, String)
  type TYPE2= (Long, String, String, LocalDateTime, String, String, Long, Int, String)
  type TYPE3 = (Long, Int, String)
  private type Transaction_Type =
    (Long, Long, Long, String, String, Instant, Instant, Instant, Int, Boolean, Int, String, String, String)

  def apply(tr: Transaction):Transaction =
    new Transaction(-1L, tr.id, -1L, tr.store, tr.account, Instant.now(), Instant.now(), Instant.now(), tr.period, false
      , tr.modelid, tr.company, tr.text).copy(lines = tr.lines.map(_.copy(id = -1L, transid = -1L, company = tr.company, modelid = tr.modelid)))
  def apply(tr: Transaction_Type): Transaction =
    new Transaction(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11, tr._12, tr._13, tr._14, Nil)
  def encodeIt(st: Transaction): TYPE = (st.id, st.oid, st.id1, st.store, st.account
    , st.transdate.atZone(ZoneId.of("Europe/Paris")).toOffsetDateTime
    , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toOffsetDateTime
    , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toOffsetDateTime
    , st.period, st.posted, st.modelid, st.company, st.text, st.footText)

  def encodeIt2(st: Transaction): TYPE2 =
    (st.oid, st.store, st.account
      , st.transdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.text, st.footText, st.id, st.modelid, st.company)
      
  def encodeIt3(st: Transaction):TYPE3= (st.id, st.modelid, st.company)

  val dummy:Transaction = Transaction(-1, 0, 0, "dummy", "dummy", Instant.now(), Instant.now()
    , Instant.now(), -1, false, -1, "0", "dummy")

final case class TransactionLog(id:Long, id1:Long, transid:Long, oid:Long, store:String, account:String, article:String,
                                quantity:BigDecimal, stock:BigDecimal, wholeStock:BigDecimal, unit:String, price:BigDecimal, avgPrice:BigDecimal,
                                currency:String, duedate:Instant, text:String, transdate:Instant, postingdate:Instant, enterdate:Instant,
                                period:Int, company:String, modelid:Int)
object TransactionLog:
  val dummy:TransactionLog=TransactionLog(0, 0, 0, 0, "", "", "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO
    , BigDecimal.ZERO, "", Instant.now(), "", Instant.now(), Instant.now(), Instant.now(), 0, "", 0)
  type TYPE = (Long, Long, Long, Long, String, String, String, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal
    , String, scala.math.BigDecimal, scala.math.BigDecimal, String, LocalDateTime, String, LocalDateTime, LocalDateTime, LocalDateTime, Int, String, Int)
  type TYPE2 = (Long, Long, Long, String, String, String, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal
    , String, scala.math.BigDecimal, scala.math.BigDecimal, String, LocalDateTime, String, LocalDateTime, LocalDateTime, LocalDateTime, Int, String, Int)
  def encodeIt(st: TransactionLog): TYPE =
    (st.id, st.id1, st.transid, st.oid, st.store, st.account, st.article, st.quantity, st.stock, st.wholeStock, st.unit, st.price, st.avgPrice, st.currency
      , st.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, st.text
      , st.transdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, st.period, st.company, st.modelid)

  def encodeIt2(st: TransactionLog): TYPE2 =
    (st.id1, st.transid, st.oid, st.store, st.account, st.article, st.quantity, st.stock, st.wholeStock, st.unit, st.price, st.avgPrice, st.currency
      , st.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, st.text
      , st.transdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, st.period, st.company, st.modelid)

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
                                        lines: List[FinancialsTransactionDetails] = Nil
                                      ) extends Trans [FinancialsTransactionDetails] {
  def month: String = common.getMonthAsString(transdate)
  def year: Int     = common.getYear(transdate)
  def getPeriod: Int = common.getPeriod(transdate)
  def total: BigDecimal = lines.map(_.amount) reduce ((l1, l2) => l2.add(l1).setScale(2, RoundingMode.HALF_UP))
  def cancel: FinancialsTransaction = copy(oid = id, id = 0, posted = false, lines=lines.map(line=>line.copy( amount = line.amount.negate())))
  def duplicate: FinancialsTransaction = copy(oid = id, id = 0, posted = false)

}
//account = $int8, side = $bool, oaccount = $varchar, amount = $numeric, duedate = $timestamp, text=$varchar, currency = $varchar
object FinancialsTransactionDetails:
  val FAKE_COMAPNY=0
  val dummy  = FinancialsTransactionDetails(0, 0, "", true, "", zeroAmount, Instant.now(), "", "EUR", "", "", "", -1)
  type D_TYPE = (Long, Long, String, Boolean, String, scala.math.BigDecimal, LocalDateTime, String, String, String, String, String, Int)
  type TYPE2 = (String, Boolean, String, scala.math.BigDecimal, LocalDateTime, String, String, String, String, Long, String, Int)
  type D_TYPE4 = (Long, String, Boolean, String, scala.math.BigDecimal, LocalDateTime, String, String, String, String, String, Int)

  implicit val monoid: Identity[FinancialsTransactionDetails] =
    new Identity[FinancialsTransactionDetails]:
      def identity: FinancialsTransactionDetails = dummy
      def combine(m1: => FinancialsTransactionDetails, m2: => FinancialsTransactionDetails): FinancialsTransactionDetails =
        m2.copy(amount = m2.amount.add(m1.amount))

  def encodeIt(dt: FinancialsTransactionDetails): D_TYPE =
    (dt.id, dt.transid, dt.account, dt.side, dt.oaccount, dt.amount
      , dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , dt.text, dt.currency, dt.company, dt.accountName, dt.oaccountName, dt.modelid)

  def encodeIt4(dt: FinancialsTransactionDetails): D_TYPE4 =
    (dt.transid, dt.account, dt.side, dt.oaccount, dt.amount
      , dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , dt.text, dt.currency, dt.company, dt.accountName, dt.oaccountName, dt.modelid)

  def encodeIt2(dt: FinancialsTransactionDetails): TYPE2 =
    (dt.account, dt.side, dt.oaccount, dt.amount, dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , dt.text, dt.currency, dt.accountName, dt.oaccountName, dt.id, dt.company, dt.modelid)

  def encodeIt3(dt: FinancialsTransactionDetails): (Long, String, Int) = (dt.id, dt.company, dt.modelid)

  private type FinancialsTransactionDetails_Type = (Long, Long,  String, Boolean, String, BigDecimal, Instant, String, String, String, String, String, Int)
  def apply(tr: FinancialsTransactionDetails_Type): FinancialsTransactionDetails =
    new FinancialsTransactionDetails(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11, tr._12, tr._13)

object FinancialsTransaction:
  private type FinancialsTransaction_Type =
    (Long, Long, Long, String, String, Instant, Instant, Instant, Int, Boolean, Int, String, String, Int, Int)
  type TYPE=(Long, Long, Long, String, String, LocalDateTime, LocalDateTime, LocalDateTime, Int, Boolean, Int, String, String, Int, Int)
  type TYPE4=(Long, Long, String, String, LocalDateTime, LocalDateTime, LocalDateTime, Int, Boolean, Int, String, String, Int, Int)
  type TYPE2= (Long, String, String, String, LocalDateTime, Int, Int, Int, Long, Int, String)

  def apply(tr: FinancialsTransaction_Type): FinancialsTransaction =
    new FinancialsTransaction(tr._1, tr._2, tr._3, tr._4, tr._5, tr._6, tr._7, tr._8, tr._9, tr._10, tr._11, tr._12, tr._13, tr._14)

  def apply(tr: FinancialsTransaction): FinancialsTransaction =
    new FinancialsTransaction(-1L, tr.id, -1L, tr.costcenter, tr.account, tr.transdate, Instant.now(), Instant.now(), tr.period
        ,false, tr.modelid,  tr.company, tr.text).copy(lines =tr.lines.map(_.copy(id = -1L, transid = -1L, company = tr.company)))

  def encodeIt(st: FinancialsTransaction): TYPE =
    (st.id, st.oid, st.id, st.costcenter, st.account
      , st.transdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.period, st.posted, st.modelid, st.company, st.text, st.typeJournal, st.file_content)

  def encodeIt4(st: FinancialsTransaction):TYPE4 =
    (   st.oid, st.id1, st.costcenter, st.account
      , st.transdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.period, st.posted, st.modelid, st.company, st.text, st.typeJournal, st.file_content)

  def encodeIt3(st: FinancialsTransaction): (Long, Int, String) = (st.id, st.modelid, st.company)
  def encodeIt2(st: FinancialsTransaction): TYPE2 =
    (st.oid, st.costcenter, st.account, st.text, st.transdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, st.period, st.typeJournal, st.file_content, st.id, st.modelid, st.company)


  val dummy: FinancialsTransaction = FinancialsTransaction(-1, 0,0, "dummy", "dummy", Instant.now(), Instant.now()
    , Instant.now(), -1, false,  -1, "0",  "dummy", -1, -1)

final case class Journal(
                          id: Long,
                          transid: Long,
                          oid: Long,
                          account: String,
                          oaccount: String,
                          parentAccount:String,
                          parentOAccount:String,
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
                          modelid: Int) {
  def debiting(amount: BigDecimal): Journal = copy(debit = debit.add(amount))
  def crediting(amount: BigDecimal): Journal = copy(credit = credit.add(amount))
  def idebiting(amount: BigDecimal): Journal = copy(idebit = idebit.add(amount))
  def icrediting(amount: BigDecimal): Journal = copy(icredit = icredit.add(amount))
  def amounting(amountx: BigDecimal): Journal = copy(amount = amount.add(amountx))
}
object Journal:
  type TYPE = ( Long, Long, String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, Int, scala.math.BigDecimal,
    scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, scala.math.BigDecimal, String, Boolean,
    String, Int, Int, String, Int)
  val dummy: Journal = Journal(0L,0L,0L, "", "", "", "", Instant.now(), Instant.now(), Instant.now(), 0, zeroAmount, zeroAmount
    , zeroAmount, zeroAmount, zeroAmount, "", true, "", 0, 0, "", -1)
  
  def encodeIt(st: Journal): TYPE =
    (st.transid, st.oid, st.account, st.oaccount, st.parentAccount, st.parentOAccount
      , st.transdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, st.period
      , st.amount, st.idebit, st.debit, st.icredit, st.credit, st.currency, st.side, st.text, st.month, st.year
      , st.company, st.modelid)

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
                       company: String = "",
                       modelid: Int = ModelId.USER.modelid,
                       roles:List[Role] = List.empty[Role],
                       rights:List[UserRight]=List.empty[UserRight],
                       modules:List[Int]=List.empty[Int]
                     )
object User:
  type TYPE = (Int, String, String, String, String, String, String, String, String, String, Int)
  type TYPE2 = (String, String, String, String, String, String, Int, Int, String)
  def apply(u: TYPE): User = new User( u._1, u._2,u._3,u._4,u._5,u._6,u._7,u._8,u._9, u._10, u._11)
  val dummy: User = new User( -1, "dummy", "dummy", "dummy", "dummy", "dummy", "dummy", "dummy", "dummy", "dummy", ModelId.USER.modelid)

  def encodeIt(st: User): TYPE =
    (st.id, st.userName, st.firstName, st.lastName, st.hash, st.phone, st.email, st.department, st.menu, st.company, st.modelid)
  def encodeIt2(st: User): TYPE2 =
    (st.firstName, st.lastName, st.phone, st.email, st.department, st.menu, st.id, st.modelid, st.company)

final case class LoginRequest(userName: String, password: String, company: String, language:String)
object LoginRequest:
  val dummy:LoginRequest = LoginRequest("dummy", "dummy", "dummy", "dummy")
final case class Role(id:Int, name:String, description:String,
                      changedate: Instant,
                      postingdate: Instant,
                      enterdate: Instant,
                      modelid:Int =ModelId.ROLE.modelid,
                      company:String,
                      rights:List[UserRight]=List.empty[UserRight])
object Role:
  type TYPE = (Int, String, String, Instant, Instant, Instant, Int, String)
  type TYPE2= (Int, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  type TYPE3= ( String, String, Int, Int, String)
  def apply(c:TYPE):Role = Role(c._1, c._2, c._3, c._4, c._5, c._6, c._7, c._8, List.empty[UserRight])
  def encodeIt(st: Role): TYPE2 =
    (st.id, st.name, st.description,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company, st.modelid)

  def encodeIt2(st: Role): TYPE3 = (st.name, st.description, st.id, st.modelid, st.company)

final case class  UserRight (moduleid:Int,  roleid:Int, short:String, company:String, modelid:Int = ModelId.USER_RIGHT.modelid)
final case class  UserRole (userid:Int,  roleid:Int, company:String, modelid:Int = ModelId.USER_ROLE.modelid)
final case class  Permission (id:Int, name:String, description:String, short:String,
                              changedate: Instant,
                              postingdate: Instant,
                              enterdate: Instant,
                              modelid:Int = ModelId.PERMISSION.modelid,
                              company:String )
object Permission:
  type TYPE = (Int, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, Int, String)
  type TYPE2 = (String, String, String, Int, Int, String)
  def encodeIt(st: Permission): TYPE =
    (st.id, st.name, st.description, st.short,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.modelid, st.company)

  def encodeIt2(st: Permission): TYPE2 = (st.name, st.description, st.short, st.id, st.modelid, st.company)

final case class  Fmodule (id:Int, name:String, description:String,
                           changedate: Instant,
                           postingdate: Instant,
                           enterdate: Instant,
                           account:String,
                           isDebit:Boolean,
                           parent:String,
                           copyFrom:String="",
                           accFilter:String="",
                           oaccFilter:String="",
                           modelid:Int = ModelId.FMODULE.modelid,
                           company:String )
object Fmodule:
  type TYPE2 = (String, String, String, Boolean, String, String, String, String, Int, Int, String)
  def encodeIt2(st: Fmodule): TYPE2 =
    (st.name, st.description, st.account, st.isDebit, st.parent, st.copyFrom, st.accFilter, st.oaccFilter, st.id, st.modelid, st.company)

final case class Room (id:String, name:String, description:String,  parent:String, changedate: Instant, postingdate: Instant,
                             enterdate: Instant, kind:Int, area:BigDecimal, company:String, modelid:Int = ModelId.ROOM.modelid )
object Room:
  type TYPE = (String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, Int, scala.math.BigDecimal, String, Int)
  type TYPE2 = (String, String, String,  Int, scala.math.BigDecimal, String, String, Int)
  def encodeIt(st: Room): TYPE =
    (st.id, st.name, st.description, st.parent,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.kind, st.area,  st.company, st.modelid)
  def encodeIt2(st: Room): TYPE2 = ( st.name, st.description, st.parent, st.kind, st.area, st.id, st.company, st.modelid)
  def encodeIt3 (st: Room):(String, Int, String) = (st.id, st.modelid, st.company)
final case class Apartment (id:String, name:String, description:String, parent:String, changedate:Instant=Instant.now(), postingdate:Instant = Instant.now(),
                            enterdate:Instant = Instant.now(), rooms:List[Room]=List.empty[Room], company:String , modelid:Int = ModelId.APARTMENT.modelid)
object Apartment:
  type TYPE2 = (String, String, String, String,  Int)
  def apply(p:Masterfile):Apartment = Apartment(p.id, p.name, p.parent, p.description, p.changedate, p.postingdate, p.enterdate, List.empty[Room], p.company)
  def toMasterfile(p:Apartment):Masterfile = Masterfile(p.id, p.name, p.parent, p.description, p.changedate, p.postingdate, p.enterdate, p.modelid, p.company)
  def encodeIt2(st: Apartment): TYPE2 = (st.id, st.name, st.description, st.company, st.modelid)
  
final case class Floor (id:String, name:String, description:String, parent:String, changedate:Instant=Instant.now(), postingdate:Instant = Instant.now(),
                             enterdate:Instant = Instant.now(), apartments:List[Apartment]=List.empty[Apartment], company:String , modelid:Int = ModelId.FLOOR.modelid)
object Floor:
  type TYPE2 = (String, String, String, String,  Int)
  def apply(p:Apartment):Floor = Floor(p.id, p.name, p.parent, p.description, p.changedate, p.postingdate, p.enterdate, List.empty[Apartment], p.company)
  def apply1(p:Masterfile):Floor = Floor(p.id, p.name, p.parent, p.description, p.changedate, p.postingdate, p.enterdate, List.empty[Apartment], p.company)
  def toMasterfile(p:Floor):Masterfile = Masterfile(p.id, p.name, p.parent, p.description, p.changedate, p.postingdate, p.enterdate, p.modelid, p.company)
  def encodeIt2(st: Floor): TYPE2 = (st.id, st.name, st.description, st.company, st.modelid)

final case class RealEstate (id:String, name:String, description:String, changedate: Instant, postingdate: Instant,
                             enterdate: Instant, apartments:List[Apartment]=List.empty[Apartment], floors:List[Floor]=List.empty[Floor], modelid:Int = ModelId.REALESTATE.modelid, company:String )
object RealEstate:
  type TYPE2 = (String, String, String, String,  Int)
  def apply(p:Masterfile):RealEstate = RealEstate(p.id, p.name, p.description, p.changedate, p.postingdate, p.enterdate
    , List.empty[Apartment], List.empty[Floor], ModelId.REALESTATE.modelid, p.company)
  def encodeIt2(st: RealEstate): TYPE2 = (st.id, st.name, st.description, st.company, st.modelid)

final case class Profile (token: String, company: String, currency:String, locale:String, language:String
                          , incomeStmtAcc:String, stockAcc:String, expenseAcc:String, revenueAcc:String,  vat: String
                          , modules: List[Module], roles: List[Role], rights:List[UserRight], error: String)
 
trait CopyFinancialsStrategy[A, B, C]:
  def copy(trans: A, account:C, modelid:Int, company: Company): B
  
object CopyFromReceavables2Bank extends CopyFinancialsStrategy[FinancialsTransaction, FinancialsTransaction, Account]:
  def copy(trans: FinancialsTransaction, account:Account, modelidx:Int, company: Company): FinancialsTransaction =
    val financials = FinancialsTransaction.apply(trans)
    financials.copy(lines = financials.lines.map(l=>l.copy(account = account.id, accountName = account.name, oaccount = l.account
      , oaccountName = l.accountName, modelid = modelidx)))

object CopyFromPayables2Bank extends CopyFinancialsStrategy[FinancialsTransaction, FinancialsTransaction, Account]:
  def copy(trans: FinancialsTransaction, account:Account, modelidx:Int, company: Company): FinancialsTransaction =
    val financials = FinancialsTransaction.apply(trans)
     financials.copy(lines = financials.lines.map(l=>l.copy(account = l.oaccount, accountName = l.oaccountName, oaccount = account.id
       , oaccountName = account.name, modelid = modelidx )) )
  
object CopySelf extends CopyFinancialsStrategy[FinancialsTransaction, FinancialsTransaction, Account]:
    def copy(trans: FinancialsTransaction, account:Account, modelidx: Int, company: Company): FinancialsTransaction =
      val financials = FinancialsTransaction.apply(trans)
      financials.copy(lines = financials.lines.map(l => l.copy(account = l.account, accountName = l.accountName
        , oaccount =l.oaccount, oaccountName = l.oaccountName, modelid = modelidx)) )


trait CopyTransactionStrategy[A, B]:
  def copy(trans: A,  modelidTo:Int, company: Company): B

object Copy2Self extends CopyTransactionStrategy[Transaction, Transaction]:
  def copy(trans: Transaction,  modelidTo: Int, company: Company): Transaction = {
    val transaction = Transaction.apply(trans).copy(period =getPeriod(Instant.now()), modelid = modelidTo)
    transaction.copy(lines = transaction.lines.map((line:TransactionDetails) =>
      line.copy(id = -1L, transid = -1L, company = company.id, modelid = modelidTo)) )
  }