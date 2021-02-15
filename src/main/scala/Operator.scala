package uk.gsscogs.build

import org.eclipse.rdf4j.repository.base.AbstractRepository

import java.io.File
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
          case _ => throw new IllegalArgumentException(s"Unmatched opType ${op.getOpType()}")
        }
      }
    }

    if (operations.exists(op => op.getOpType() == Operation.OpTypes.SPARQL_UPDATE)) {
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

  private def validateAndPerformSparqlUpdateOp(operation: FileOperation, repo: AbstractRepository) = {
    validateNumArgsForOp(operation, Operation.OpTypes.SPARQL_UPDATE, 1)
    val sparqlQueryFile = new File(operation.getArguments().apply(0))
    if (!sparqlQueryFile.exists()){
      throw new IllegalArgumentException(s"SPARQL Query file '${sparqlQueryFile.getPath}' does not exist.")
    }
    val sparqlQuery = Source.fromFile(sparqlQueryFile).getLines.mkString("\n")

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
