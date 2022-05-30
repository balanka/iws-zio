package com.kabasoft.iws.service
import zio.stream._
import com.kabasoft.iws.domain._
import java.nio.file.{ Files, Paths }

object BankStmtImportService {

  def importFromPath(path: String, HEADER: String, CHAR: String, extension: String, build: String => BankStatement) =
    ZStream
      .fromJavaStream(Files.walk(Paths.get(path)))
      .filter(p => !Files.isDirectory(p) && p.toString.endsWith(extension))
      .flatMap { files =>
        ZStream
          .fromPath(files)
          .via(ZPipeline.utfDecode >>> ZPipeline.splitLines)
          .filterNot(p => p.replaceAll(CHAR, "").startsWith(HEADER))
          .map(p => build(p.replaceAll(CHAR, "")))
      // .tap(bs=>ZIO.debug(s"  BS ${bs}"))
      }
      .runCollect
      .map(_.toList)

}
