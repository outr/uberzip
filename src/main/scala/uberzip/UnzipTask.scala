package uberzip

import java.io.File
import java.util.zip.{ZipEntry, ZipFile}

import scala.concurrent.{ExecutionContext, Future}

class UnzipTask(zip: ZipFile, val entry: ZipEntry, directory: File) {
  def execute()(implicit ec: ExecutionContext): Future[Unit] = UberZip.unzipFile(zip, entry, directory)
}