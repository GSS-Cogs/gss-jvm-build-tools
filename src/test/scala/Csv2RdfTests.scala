package uk.gsscogs.build

import org.scalatest.FunSuite

import java.io.File
import java.nio.file.{Files, Paths}

class Csv2RdfTests extends FunSuite {
  val tempDir = Files.createTempDirectory("csv2rdf-unit-tests").toFile

  test("Test basic conversion") {
    val in = getClass.getResource("/Csv2RdfTests/test.csv-metadata.json")
    val outputFile = Paths.get(tempDir.getAbsolutePath, "test.output.ttl").toFile

    Csv2Rdf.convertFile(
      new File(in.getPath),
      outputFile
    )

    val outputFileRepo = RdfRepo.getRepoForFile(outputFile)
    try {
      val labelsExistInOutput = RdfRepo.ask(outputFileRepo,
        """
          | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          |
          | ASK
          | WHERE {
          |   ?firstItem rdfs:label "First Label".
          |   ?secondItem rdfs:label "Second Label".
          | }
          |""".stripMargin)
      assert(labelsExistInOutput)
    } finally {
      RdfRepo.disposeOfRepo(outputFileRepo)
    }
  }
}
