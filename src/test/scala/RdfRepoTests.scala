package uk.gsscogs.build

import org.eclipse.rdf4j.repository.base.AbstractRepository
import org.scalatest.FunSuite

import java.io.File
import java.nio.file.{Files, Paths}

class RdfRepoTests extends FunSuite {
  val tempDir = Files.createTempDirectory("rdf-repo-unit-tests").toFile

  test("Test Update SPARQL query followed by Write to File works") {
    val in = getClass.getResource("/RdfRepoTests/existing-data.ttl")
    val outFile = Paths.get(tempDir.getAbsolutePath, "test.output.ttl").toFile

    val repo = RdfRepo.getRepoForFile(new File(in.getPath))
    try {
      assert(!getNarrowerConceptsExist(repo))

      RdfRepo.updateOrInsertOrDelete(repo,
        """
          |INSERT {
          |    ?broaderConcept <http://www.w3.org/2004/02/skos/core#narrower> ?concept.
          |}
          |WHERE {
          |    ?conceptScheme
          |        <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#ConceptScheme>.
          |    ?concept
          |        <http://www.w3.org/2004/02/skos/core#inScheme> ?conceptScheme;
          |        <http://www.w3.org/2004/02/skos/core#broader> ?broaderConcept.
          |        FILTER NOT EXISTS {
          |            ?broaderConcept <http://www.w3.org/2004/02/skos/core#narrower> ?concept.
          |        }
          |}
          |""".stripMargin)

      assert(getNarrowerConceptsExist(repo))

      RdfRepo.writeToOutputFile(repo, outFile)
    } finally {
      RdfRepo.disposeOfRepo(repo)
    }

    val updatedRepo = RdfRepo.getRepoForFile(outFile)
    try {
      assert(getNarrowerConceptsExist(updatedRepo))
    } finally {
      RdfRepo.disposeOfRepo(updatedRepo)
    }

  }

  private def getNarrowerConceptsExist(repo: AbstractRepository) = {
    RdfRepo.ask(repo,
      """
        |ASK
        |WHERE {
        |   ?broaderConcept <http://www.w3.org/2004/02/skos/core#narrower> ?concept.
        |}
        |""".stripMargin)
  }
}
