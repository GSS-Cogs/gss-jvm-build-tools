package uk.gsscogs.build
import clojure.java.api.Clojure
import clojure.lang.IFn

import java.io.File

object Csv2Rdf {
  private val require = Clojure.`var`("clojure.core", "require")
  require.invoke(Clojure.read("csv2rdf.csvw"))

  private val csv2rdf2File: IFn = Clojure.`var`("csv2rdf.csvw", "csv->rdf->file")

  def convertFile(metadataFilePath: File, outputFile: File): Unit = {
    csv2rdf2File.invoke(null, metadataFilePath, outputFile, null)
  }
}