name := "gss-jvm-build-tools"

version := "0.1"

scalaVersion := "2.13.4"

idePackagePrefix := Some("uk.gsscogs.build")

resolvers += "Clojars" at "https://repo.clojars.org"

libraryDependencies += "swirrl" % "csv2rdf" % "0.4.6"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test

libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-query" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-repository-sail" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-sail-memory" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-jsonld" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-n3" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-nquads" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-ntriples" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-rdfjson" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-rdfxml" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-trig" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-trix" % "3.5.1"
libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-rio-turtle" % "3.5.1"

libraryDependencies += "com.github.scopt" %% "scopt" % "4.0.0"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.1"

enablePlugins(JavaAppPackaging)