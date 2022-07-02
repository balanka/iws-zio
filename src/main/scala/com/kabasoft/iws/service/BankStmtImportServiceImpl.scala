package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.BankStatementRepository
import zio.ZLayer
import zio.stream._
import zio._

import java.nio.file.{ Files, Paths }

final class BankStmtServiceImpl(bankStmtRepo: BankStatementRepository) extends BankStmtService {

  override def importBankStmt(
    path: String,
    header: String,
    char: String,
    extension: String,
    company: String,
    buildFn: String => BankStatement = BankStatement.from
  ): ZIO[Any, RepositoryError, Int] = for {
    bs <- ZStream
            .fromJavaStream(Files.walk(Paths.get(path)))
            .filter(p => !Files.isDirectory(p) && p.toString.endsWith(extension))
            .flatMap { files =>
              ZStream
                .fromPath(files)
                .via(ZPipeline.utfDecode >>> ZPipeline.splitLines)
                .filterNot(p => p.replaceAll(char, "").startsWith(header))
                .map(p => buildFn(p.replaceAll(char, "")))
            }
            .mapError(e => RepositoryError(e))
            .runCollect
            .map(_.toList)
    nr <- bankStmtRepo.create(bs)
  } yield nr
}

object BankStmtServiceImpl {
  val live: ZLayer[BankStatementRepository, RepositoryError, BankStmtService] =
    ZLayer.fromFunction(new BankStmtServiceImpl(_))
}
