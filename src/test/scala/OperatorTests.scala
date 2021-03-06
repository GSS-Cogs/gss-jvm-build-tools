package uk.gsscogs.build

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.FunSuite

import java.nio.file.{Files, Paths}
import scala.io.Source

class OperatorTests extends FunSuite {
  val tempDir = Files.createTempDirectory("operator-unit-tests").toFile

  test("Test conversion with CSV2RDF, SPARQL Update, SPARQL Query & SPARQL to Quads") {
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

    val quadsOutput = Paths.get(tempDir.getPath, "test.out.nq").toFile
    val quadsRepo = RdfRepo.getRepoForFile(quadsOutput)

    val rdfHasBeenCreatedAndAugmented = RdfRepo.ask(quadsRepo,
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

    val outputGraphNameJsonFile = Paths.get(tempDir.getPath, "test.graph.name.json").toFile

    val jsonResult = new ObjectMapper().readTree(outputGraphNameJsonFile)
    val bindingsArray = jsonResult
        .get("results")
        .get("bindings")
        .elements()

    assert(bindingsArray.hasNext)

    val graphUri = bindingsArray.next()
      .get("graph")
      .get("value")
      .textValue()

    assert(!bindingsArray.hasNext)

    assert(graphUri == "http://some-uri/#scheme/stuff")

    try {
      val triplesExistWithoutCorrectGraphName = RdfRepo.ask(quadsRepo,
        """
          |ASK
          |WHERE {
          | ?s ?p ?o
          | FILTER NOT EXISTS {
          |   GRAPH <http://some-uri/#scheme/stuff> {
          |     ?s ?p ?o.
          |   }
          | }
          |}
          |""".stripMargin)
      assert(!triplesExistWithoutCorrectGraphName)
    } finally {
      RdfRepo.disposeOfRepo(quadsRepo)
    }
  }

}
