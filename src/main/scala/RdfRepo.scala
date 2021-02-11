package uk.gsscogs.build

import org.eclipse.rdf4j.repository.base.AbstractRepository

import java.io.{File, FileOutputStream}
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio

object RdfRepo {
  def getRepoForFile(file: File): AbstractRepository = {
    val db = new SailRepository(new MemoryStore)
    db.init()
    try {
      val conn = db.getConnection
      try {
        conn.add(file)
      } finally {
        conn.close()
      }
      return db
    }
    catch {
      case e: Exception => {
        db.shutDown()
        throw e
      }
    }
  }

  def ask(repo: AbstractRepository, query: String): Boolean = {
    val conn = repo.getConnection
    try {
      val queryTuple = conn.prepareBooleanQuery(query)
      return queryTuple.evaluate
    } finally {
      conn.close()
    }
  }

  def updateOrInsertOrDelete(repo: AbstractRepository, query: String): Unit = {
    val conn = repo.getConnection
    try {
      val queryTuple = conn.prepareUpdate(query)
      queryTuple.execute()
    } finally {
      conn.close()
    }
  }

  def writeToOutputFile(repo: AbstractRepository,
                        outputFile: File,
                        outputFormat: RDFFormat = RDFFormat.TURTLE): Unit = {
    val conn = repo.getConnection
    try {
      val outputStream = new FileOutputStream(outputFile)
      try {
        val writer = Rio.createWriter(outputFormat, outputStream)
        conn.`export`(writer)
      } finally {
        outputStream.close()
      }
    } finally {
      conn.close()
    }
  }

  def disposeOfRepo(repo: AbstractRepository): Unit = {
    if (repo.isInitialized) {
      repo.shutDown()
    }
  }
}
