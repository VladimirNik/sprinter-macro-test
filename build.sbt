scalaVersion := "2.11.0-M7"

name := "Sprinter Test"

scalaHome := Some(file("/home/vova/scala-projects/GSoC/scala/scala/build/pack"))

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
