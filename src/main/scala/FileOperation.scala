package uk.gsscogs.build

/**
 * For a given file, run the following operations.
 * file
 * opType Should be one of Operation.OpTypes
 * arguments Arguments to be passed to the operation.
 */
class FileOperation() {
  private var file: String = null
  private var opType: String = null
  private var arguments: Array[String] = Array()

  def getFile(): String = this.file

  def setFile(file: String): Unit = {
    this.file = file
  }

  def getOpType(): String = this.opType

  def setOpType(opType: String): Unit = {
    this.opType = opType
  }

  def getArguments(): Array[String] = this.arguments

  def setArguments(arguments: Array[String]): Unit = {
    this.arguments = arguments
  }
}

object Operation {
  object OpTypes {
    val CSV_2_RDF: String = "CSV2RDF"
    val SPARQL_UPDATE: String = "SPARQL Update"
    val SPARQL_QUERY: String = "SPARQL Query"
  }
}