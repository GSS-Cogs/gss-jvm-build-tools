package uk.gsscogs.build

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import scopt.OParser

import java.io.File

case class CliConfig(configFile: Option[File] = None, verbose: Boolean = false)

object Main extends App {

  val cliBuilder = OParser.builder[CliConfig]
  val cliParser = {
    import cliBuilder._
    OParser.sequence(
      programName("gss-jvm-tools"),
      head("gss-jvm-tools", "Helps transform your CSV-Ws into RDF and augment the metadata with SPARQL queries."),

      opt[File]('c', "config-file")
        .action((x, c) => c.copy(configFile = Some(x)))
        .required()
        .text("Location of the JSON configuration file specifying actions which should be performed."),

      opt[Unit]('v', "verbose")
        .action((_, c) => c.copy(verbose = true))
        .text("Whether output is verbose or not."),
    )
  }

  OParser.parse(cliParser, args, CliConfig()) match {
    case Some(config) => {
      config.configFile match {
        case Some(configFile) => {
          val objectMapper = new ObjectMapper()
          val fileOperations = objectMapper.readValue(configFile, new TypeReference[Array[FileOperation]] {})
          Operator.performOperations(fileOperations, config.verbose)
        }
        case _ => throw new IllegalArgumentException("Config File not set")
      }
    }
    case _ => // arguments are bad, error message will have been displayed
  }

}
