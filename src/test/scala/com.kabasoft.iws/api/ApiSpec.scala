package com.kabasoft.iws.api

import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import zio.json.EncoderOps
import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.repository.{BankRepository, BankRepositoryImpl, CostcenterRepository, CostcenterRepositoryImpl, SupplierRepository, SupplierRepositoryImpl}
import com.kabasoft.iws.api.CostcenterEndpoint.ccByIdAPI
import zio.http.api.HttpCodec.literal
import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.api.SupplierEndpoint.supByIdAPI
import com.kabasoft.iws.domain.CostcenterBuilder.cc
import com.kabasoft.iws.domain.SupplierBuilder.sup
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


  def instantFromStr(str:String)=Instant.parse(str)
    def spec = suite("APISpec")(
      suite("handler")(
        test("Bank integration test ") {
/*        val accAll = EndpointSpec.get(literal("acc") ).out[Int]
                     .implement { id => AccountRepository.all("1000").map(_.size)}
        val accById =  EndpointSpec.get(literal("acc") / string("id")).out[Account]
                         .implement { id => AccountRepository.getBy(id, "1000")}*/
          val bankById =    EndpointSpec.get(literal("bank") / string("id")).out[Bank]
                            .implement { id =>BankRepository.getBy(id, "1000")}
          val bankAll =EndpointSpec.get(literal("bank")).out[Int]
                                .implement (_ => BankRepository.all("1000").map(_.size))
          val testRoutes = testApi(bankAll++bankById) _
          testRoutes("/bank", "56") && testRoutes("/bank/COLSDE33", bank.toJson)
        },
        test("Supplier integration test") {
          val supAllEndpoint = EndpointSpec.get[Unit](literal("sup")).out[Int]
            .implement(_ => SupplierRepository.all("1000").map(_.size))
          val supByIdEndpoint = supByIdAPI.implement(id => SupplierRepository.getBy(id, "1000"))
          val testRoutes = testApi(supByIdEndpoint ++ supAllEndpoint) _
          testRoutes("/sup", "67") && testRoutes("/sup/70000", sup.toJson)
        },
        test("Cost center integration test") {
          val ccAllEndpoint = EndpointSpec.get(literal("cc")).out[Int].implement(_ => CostcenterRepository.all("1000").map(_.size))
          val ccByIdEndpoint = ccByIdAPI.implement(id => CostcenterRepository.getBy(id, "1000"))
          val testRoutes = testApi(ccByIdEndpoint ++ ccAllEndpoint) _
          testRoutes("/cc", "13") && testRoutes("/cc/300", cc.toJson)
        }
    ).provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, BankRepositoryImpl.live,
        CostcenterRepositoryImpl.live, SupplierRepositoryImpl.live)
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