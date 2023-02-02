package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import zio.json.EncoderOps
//import com.kabasoft.iws.domain.Account
//import com.kabasoft.iws.repository.AccountRepository
//import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.repository.{ BankRepository, BankRepositoryImpl}
import zio._
import zio.http.api.RouteCodec._
import zio.http.api.{EndpointSpec, Endpoints}
import zio.http.{Request, URL}
import zio.schema.DeriveSchema.gen
import zio.sql.ConnectionPool
import zio.test._

import java.time.Instant


object ApiSpec extends ZIOSpecDefault {
  val company = "1000"
  val bankId = "COLSDE33"
  val bankName = "SPARKASSE KOELN-BONN"
  val accId = "9900"
  val accName = "Bilanz"
  val bank = Bank(bankId, bankName, bankName, instantFromStr("2018-01-01T00:00:00.00Z"),
    instantFromStr("2018-01-01T00:00:00.00Z"), instantFromStr("2018-01-01T00:00:00.00Z"),11,company)
 // val acc = Account(accId, accName, accName, instantFromStr("2017-12-31T23:00:00Z"), instantFromStr("2017-12-31T23:00:00Z")
 //   , instantFromStr("2020-03-01T23:00:00Z"), company, 9, "", true, true, "EUR", zeroAmount, zeroAmount, zeroAmount, zeroAmount, Nil.toSet)


  def instantFromStr(str:String)=Instant.parse(str)
    def spec = suite("APISpec")(
      suite("handler")(
        test("simple request") {
/*        val accAll = EndpointSpec.get(literal("acc") ).out[Int]
                     .implement { id => AccountRepository.all("1000").map(_.size)}
        val accById =  EndpointSpec.get(literal("acc") / string("id")).out[Account]
                         .implement { id => AccountRepository.getBy(id, "1000")}*/
          val bankById =    EndpointSpec.get(literal("bank") / string("id")).out[Bank]
                            .implement { id =>BankRepository.getBy(id, "1000")}
          val bankAll =EndpointSpec.get(literal("bank")).out[Int]
                                .implement { case () => BankRepository.all("1000").map(_.size)}
          val testRoutes = testApi(
            bankAll++bankById
            //accAll++accById++
          ) _
          //testRoutes("/acc", "326") &&
          //  testRoutes("/acc/0654", "1") &&
          testRoutes("/bank", "56") &&
          testRoutes("/bank/COLSDE33", bank.toJson)
        }.provide( ConnectionPool.live, connectionPoolConfig, DbConfig.layer,
          BankRepositoryImpl.live),
      )
    )
  def testApi[R, E](service: Endpoints[R, E, _])(
    url: String, expected: String): ZIO[R, E, TestResult] = {
    val request = Request.get(url = URL.fromString(url).toOption.get)
    for {
      response <- service.toHttpRoute.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }
}