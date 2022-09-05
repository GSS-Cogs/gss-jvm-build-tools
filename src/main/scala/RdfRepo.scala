package uk.gsscogs.build

import org.eclipse.rdf4j.query.{BindingSet, TupleQueryResult}
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter
import org.eclipse.rdf4j.repository.base.AbstractRepository

import java.io.{File, FileOutputStream, OutputStream}
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.nativerdf.NativeStore
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio

import java.net.URI

object RdfRepo {
  def getRepoForFile(file: File, graphUri: Option[URI] = None): AbstractRepository = {
    /**
     * "The NativeStore saves data to disk in a binary format which is optimized for compact storage and fast retrieval.
     * If there is sufficient physical memory, the Native store will act like the MemoryStore on most operating systems
     * because the read/write commands will be cached by the OS.
     *
     * It is therefore an efficient, scalable and fast solution for datasets with up to 100 million triples
     * (and probably even more)."
     *
     * https://rdf4j.org/documentation/programming/repository/#native-rdf-repository
     */
    val db = new SailRepository(new NativeStore)
    db.init()
    try {
      val conn = db.getConnection
      val f = conn.getValueFactory
      try {
        graphUri match {
          case Some(uri) => {
            val graphIri = f.createIRI(uri.toString)
            conn.add(file, graphIri)
          }
          case None => conn.add(file)
        }
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

  def queryToJson(repo: AbstractRepository, query: String, jsonOutputStream: OutputStream): Unit = {
    val conn = repo.getConnection
    try {
      conn
        .prepareTupleQuery(query)
        .evaluate(new SPARQLResultsJSONWriter(jsonOutputStream))
    } finally {
      conn.close()
    }
  }

  private def getSingleBindingSet(queryResult: TupleQueryResult): BindingSet = {
    if (queryResult.hasNext) {
      val firstBindingSet = queryResult.next()
      if (queryResult.hasNext) {
        throw new UnsupportedOperationException(s"Found >1 results for SPARQL query when 1 was expected.")
      }
      return firstBindingSet
    } else {
      throw new UnsupportedOperationException(s"Found 0 results for SPARQL query when 1 was expected.")
    }
  }

  def querySingleStringValue(repo: AbstractRepository, query: String): String = {
    val conn = repo.getConnection
    try {
      val results = conn
        .prepareTupleQuery(query)
        .evaluate()

      val singleResult = getSingleBindingSet(results)

      val bindingNames = singleResult.getBindingNames()
      if (bindingNames.size() != 1){
        throw new UnsupportedOperationException(s"Found ${bindingNames.size()} bindings on SPARQL query when 1 was expected.")
      }
      val bindingName = bindingNames.iterator().next()

      return singleResult.getValue(bindingName).stringValue()
    } finally {
      conn.close()
    }
  }


  def writeToOutputFile(repo: AbstractRepository,
                        outputFile: File): Unit = {
    val conn = repo.getConnection
    try {
      val outputStream = new FileOutputStream(outputFile)
      try {
        val outputFormat = Rio.getParserFormatForFileName(outputFile.getName)
          .orElse(RDFFormat.RDFXML)
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
