package uberzip

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.{ZipEntry, ZipFile}

import io.youi.stream._

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object UberZip {
  var DefaultThreadCount = 10

  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global

    if (args.length == 0 || "/?".equals(args(0))) {
      println("Usage:")
      println("\tuberzip [zipfile] {output directory} {thread-count}")
    } else {
      val start = System.nanoTime()
      val directory = if (args.length > 1) {
        args(1)
      } else {
        "."
      }
      val threadCount = if (args.length > 2) {
        args(2).toInt
      } else {
        DefaultThreadCount
      }
      val zipFile = new File(args(0))
      val future = unzip(zipFile, new File(directory), threadCount)
      val unzipped = Await.result(future, 1.hour)
      println(s"Unzipped $unzipped entries in ${(System.nanoTime() - start) / 1000000000.0f} seconds.")
    }
  }

  def unzip(file: File, directory: File, threadCount: Int = DefaultThreadCount)
           (implicit ec: ExecutionContext): Future[Int] = {
    directory.mkdirs()

    val running = new AtomicInteger(0)
    val counter = new AtomicInteger(0)
    val tasks = new ConcurrentLinkedQueue[UnzipTask]

    val zip = new ZipFile(file)
    zip.entries().asScala.foreach { entry =>
      if (entry.getName.endsWith("/")) {
        // Create directory
        val dir = new File(directory, entry.getName)
        dir.mkdirs()
      } else {
        tasks.add(new UnzipTask(zip, entry, directory))
        counter.incrementAndGet()
      }
    }
    val total = counter.get()
    val promise = Promise[Int]
    (0 until threadCount).foreach { _ =>
      running.incrementAndGet()
      unzipNext()
    }

    def unzipNext(): Unit = {
      Option(tasks.poll()) match {
        case Some(task) => {
          val future = task.execute()
          future.failed.foreach { throwable =>
            new RuntimeException(s"Error extracting ${task.entry.getName}", throwable)
          }
          future.onComplete { _ =>
            counter.decrementAndGet()
            unzipNext()
          }
        }
        case None => if (running.decrementAndGet() == 0) {
          promise.success(total)
        }
      }
    }

    promise.future
  }

  def unzipFile(zip: ZipFile, entry: ZipEntry, directory: File)
               (implicit ec: ExecutionContext): Future[Unit] = Future {
    val output = new File(directory, entry.getName)
    val input = zip.getInputStream(entry)
    IO.stream(input, output)
  }
}