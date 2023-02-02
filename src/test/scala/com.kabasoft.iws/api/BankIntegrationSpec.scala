package com.kabasoft.iws.api

object BankIntegrationSpec {//extends ZIOSpecDefault {
  /*
    def testApi[R, E](service: Endpoints[R, E, _])(
      url: String,
      expected: String,
    ): ZIO[R, E, TestResult] = {
      val request = Request.get(url = URL.fromString(url).toOption.get)
      for {
        response <- service.toHttpRoute.runZIO(request).mapError(_.get)
        body <- response.body.asString.orDie
      } yield assertTrue(body == "\"" + expected + "\"") // TODO: Real JSON Encoding
    }

    def spec =
      suite("BankIntegrationSpec")(
          test("server and client bank integration") {
          val testRoutes = testApi(
            allBankHandler++getBankByIdEndpoint
          ) _
          testRoutes("http://127.0.0.1:9090/bank/", "route(users, 123)") &&
          testRoutes("http://127.0.0.1:9090/bank/123", "(users, 123, posts, 555) query(name=adam, age=9000)")
        }
      ).provideSome(SConnectionPool.live, connectionPoolConfig, DbConfig.layer, BankRepositoryImpl.live)

   */
}
