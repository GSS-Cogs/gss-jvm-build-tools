package uk.gsscogs.build

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.FunSuite

import java.nio.file.{Files, Paths}
import scala.io.Source

class OperatorTests extends FunSuite {
  val tempDir = Files.createTempDirectory("operator-unit-tests").toFile

  test("Test basic conversion") {
    val testResourcesDir = getClass.getResource("/OperatorUnitTests")

    val configFile = Paths.get(testResourcesDir.getPath, "config.json").toFile
    val configJson = Source.fromFile(configFile)
      .getLines()
      .mkString("\n")
      .replaceAll("\\{resources_dir\\}", testResourcesDir.getPath)
      .replaceAll("\\{tmp_dir\\}", tempDir.getPath)

    val objectMapper = new ObjectMapper()
    val fileOperations = objectMapper.readValue(configJson, new TypeReference[Array[FileOperation]] {})
    Operator.performOperations(fileOperations, true)

    val outputFile = Paths.get(tempDir.getPath, "test.out.ttl").toFile
    val outputRepo = RdfRepo.getRepoForFile(outputFile)
    val rdfHasBeenCreatedAndAugmented = RdfRepo.ask(outputRepo,
      """
        |ASK
        |WHERE {
        |   ?broaderConcept
        |     <http://www.w3.org/2004/02/skos/core#narrower> ?concept;
        |     <http://www.w3.org/2004/02/skos/core#inScheme> ?conceptScheme.
        |
        |   ?concept
        |     <http://www.w3.org/2004/02/skos/core#broader> ?broaderConcept;
        |     <http://www.w3.org/2004/02/skos/core#inScheme> ?conceptScheme.
        |
        |   FILTER EXISTS {
        |     ?conceptScheme <http://www.w3.org/2004/02/skos/core#hasTopConcept> ?topConcept.
        |   }
        |}
        |""".stripMargin)

    assert(rdfHasBeenCreatedAndAugmented)
  }

}
