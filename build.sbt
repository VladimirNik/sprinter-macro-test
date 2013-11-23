scalaVersion := "2.10.2"

name := "Sprinter Test"

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)

libraryDependencies += "org.scala-lang" %% "sprinter" % "0.2.0"

libraryDependencies += "org.scala-refactoring" % "org.scala-refactoring.library" % "0.6.1"
