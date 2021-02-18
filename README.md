# gss-jvm-build-tools

This tool is designed to bring together many of the JVM-based tools used as part of the GSS-COGS CSV-W -> RDF build pipeline. It accepts a JSON file 
detailing the actions which should be performed and then it runs a *single instance* of the JVM which executes all of the operations specified.

This helps dramatically improve performance in build pipelines where there are many calls to JVM CLI tools, such as `csv2rdf` and `sparql`, by
removing the overhead associated with creating JVM instances for each of the CLI calls; it also pushes the JVM to more efficiently JIT compile the
associated bytecode for functions which are run many times; N.B. the previous many-JVMs approach meant each JVM was unable to track how often functions 
in different JVM instances were called, and so left it unable to optimise bytecode compilation as well as it now can.
