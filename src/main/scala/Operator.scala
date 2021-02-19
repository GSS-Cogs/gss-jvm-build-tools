package uk.gsscogs.build

import org.eclipse.rdf4j.repository.base.AbstractRepository

import java.io.{File, FileOutputStream}
import java.net.URI
import scala.io.Source

object Operator {
  /**
   * Perform the given operations against a series of files.
   *
   * @param fileOperations
   */
  def performOperations(fileOperations: Seq[FileOperation], verbose: Boolean): Unit = {
    fileOperations match {
      case Seq() => // Done
      case op+:_ => {
        val thisFile = op.getFile()

        // Find any operations immediately following on the same file.
        // We can group them together and run more performant build operations.
        // BUT we have to maintain the same order so we don't do anything scary.
        val contiguousOpsOnSameFile = fileOperations.takeWhile(o => o.getFile() == thisFile)
        val followingOpsOnDifferentFiles = fileOperations.dropWhile(o => o.getFile() == thisFile)

        performOperationsForFile(thisFile, contiguousOpsOnSameFile, verbose)

        // Recurse through the list, continue performing operations.
        performOperations(followingOpsOnDifferentFiles, verbose)
      }
    }
  }

  private def performOperationsForFile(filePath: String, operations: Seq[FileOperation], verbose: Boolean): Unit = {
    if (verbose) {
      println(s"Operating on ${filePath}")
    }

    val file = new File(filePath)
    if (!file.exists()) {
      throw new IllegalArgumentException(s"File ${filePath} does not exist.")
    }

    def performOperationsInternal(maybeRepo: Option[AbstractRepository]) = {
      for (op <- operations) {
        notifyOpStartedIfVerbose(verbose, op)
        (op.getOpType(), maybeRepo) match {
          case (Operation.OpTypes.CSV_2_RDF, _) => validateAndPerformCsv2RdfOp(op, file)
          case (Operation.OpTypes.SPARQL_UPDATE, Some(repo)) => validateAndPerformSparqlUpdateOp(op, repo)
          case (Operation.OpTypes.SPARQL_UPDATE, _) =>
            throw new IllegalArgumentException(
              s"Repository must be provided where ${Operation.OpTypes.SPARQL_UPDATE} operations exist.")
          case (Operation.OpTypes.SPARQL_QUERY, Some(repo)) => validateAndPerformSparqlQueryOp(op, repo)
          case (Operation.OpTypes.SPARQL_QUERY, _) =>
            throw new IllegalArgumentException(
              s"Repository must be provided where ${Operation.OpTypes.SPARQL_QUERY} operations exist.")
          case (Operation.OpTypes.SPARQL_TO_QUADS, Some(repo)) => validateAndPerformSparqlToQuads(op, repo)
          case (Operation.OpTypes.SPARQL_TO_QUADS, _) =>
            throw new IllegalArgumentException(
              s"Repository must be provided where ${Operation.OpTypes.SPARQL_TO_QUADS} operations exist.")

          case _ => throw new IllegalArgumentException(s"Unmatched opType ${op.getOpType()}")
        }
      }
    }

    if (operations.exists(op =>
          op.getOpType() == Operation.OpTypes.SPARQL_UPDATE
            || op.getOpType() == Operation.OpTypes.SPARQL_QUERY
            || op.getOpType() == Operation.OpTypes.SPARQL_TO_QUADS
        )
    ) {
      // Keep repo in-memory until all SPARQL update ops completed.
      val repo = RdfRepo.getRepoForFile(file)
      try {
        performOperationsInternal(Some(repo))

        RdfRepo.writeToOutputFile(repo, file)
      } finally {
        RdfRepo.disposeOfRepo(repo)
      }
    } else {
      performOperationsInternal(None)
    }
  }

  private def notifyOpStartedIfVerbose(verbose: Boolean, op: FileOperation) = {
    if (verbose) {
      println(s"Performing ${op.getOpType()} operation with arguments ${op.getArguments().mkString(", ")}")
    }
  }

  private def validateAndPerformSparqlToQuads(operation: FileOperation, repo: AbstractRepository) = {
    validateNumArgsForOp(operation, Operation.OpTypes.SPARQL_TO_QUADS, 2)
    val args = operation.getArguments()

    val graphUriSparqlQuery: String = getSparqlQueryFromFile(operation.getArguments().apply(0))
    val outputFile = new File(args.apply(1))

    val graphUri = RdfRepo.querySingleStringValue(repo, graphUriSparqlQuery)

    val triplesFile = new File(operation.getFile())
    // Make sure that we've written any changes out to the triples file before reading it in to the quads repo.
    RdfRepo.writeToOutputFile(repo, triplesFile)

    val quadsRepo = RdfRepo.getRepoForFile(triplesFile, Some(new URI(graphUri)))
    try {
      RdfRepo.writeToOutputFile(quadsRepo, outputFile)
    } finally {
      RdfRepo.disposeOfRepo(quadsRepo)
    }
  }


  private def validateAndPerformSparqlQueryOp(operation: FileOperation, repo: AbstractRepository) = {
    validateNumArgsForOp(operation, Operation.OpTypes.SPARQL_QUERY, 2)
    val args = operation.getArguments()

    val sparqlQuery: String = getSparqlQueryFromFile(operation.getArguments().apply(0))
    val outputJsonFile = new File(args.apply(1))

    val fileOutJsonStream = new FileOutputStream(outputJsonFile)
    try {
      RdfRepo.queryToJson(repo, sparqlQuery, fileOutJsonStream)
    } finally {
      fileOutJsonStream.close()
    }
  }

  private def getSparqlQueryFromFile(sparqlQueryFilePath: String) = {
    val sparqlQueryFile = new File(sparqlQueryFilePath)
    if (!sparqlQueryFile.exists()) {
      throw new IllegalArgumentException(s"SPARQL Query file '${sparqlQueryFile.getPath}' does not exist.")
    }
    val sparqlQuery = Source.fromFile(sparqlQueryFile).getLines.mkString("\n")
    sparqlQuery
  }

  private def validateAndPerformSparqlUpdateOp(operation: FileOperation, repo: AbstractRepository) = {
    validateNumArgsForOp(operation, Operation.OpTypes.SPARQL_UPDATE, 1)
    val sparqlQuery: String = getSparqlQueryFromFile(operation.getArguments().apply(0))

    RdfRepo.updateOrInsertOrDelete(repo, sparqlQuery)
  }

  private def validateAndPerformCsv2RdfOp(operation: FileOperation, file: File) = {
    validateNumArgsForOp(operation, Operation.OpTypes.CSV_2_RDF, 1)
    val outputFile = new File(operation.getArguments().apply(0))

    Csv2Rdf.convertFile(file, outputFile)
  }

  private def validateNumArgsForOp(operation: FileOperation, opName: String, numArgsExpected: Int) = {
    if (operation.getArguments() == null) {
      throw new IllegalArgumentException(s"${opName} -- arguments not provided.")
    }
    if (operation.getArguments().length != numArgsExpected) {
      throw new IllegalArgumentException(
        s"${opName} -- Expected ${numArgsExpected} arguments, ${operation.getArguments().length} were provided"
      )
    }
  }
}
