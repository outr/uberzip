package com.outr.uberzip

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.{ZipEntry, ZipFile}

import org.powerscala.io._

import scala.collection.JavaConversions._

object UberZip {
  def main(args: Array[String]): Unit = {
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
        10
      }
      val zipFile = new File(args(0))
      unzip(zipFile, new File(directory), threadCount)
      println(s"Completed in ${(System.nanoTime() - start) / 1000000000.0f} seconds.")
    }
  }

  def unzip(file: File, directory: File, threadCount: Int): Unit = {
    directory.mkdirs()

    val executor = Executors.newFixedThreadPool(threadCount)

    val counter = new AtomicInteger()

    class UnzipWorker(zip: ZipFile, entry: ZipEntry, directory: File) extends Runnable {
      override def run(): Unit = UberZip.unzip(zip, entry, directory)
    }

    val zip = new ZipFile(file)
    zip.entries().foreach { entry =>
      if (entry.getName.endsWith("/")) {
        // Create directory
        val dir = new File(directory, entry.getName)
        dir.mkdirs()
      } else {
        counter.incrementAndGet()
        executor.submit(new UnzipWorker(zip, entry, directory))
      }
    }

    while (counter.get() > 0) {
      Thread.sleep(10)
    }
    executor.shutdown()
  }

  def unzip(zip: ZipFile, entry: ZipEntry, directory: File): Unit = {
    val output = new File(directory, entry.getName)
    val input = zip.getInputStream(entry)
    IO.stream(input, output)
  }
}