package ch.epfl.lamp.sprintertest

import scala.reflect.macros.Context
import language.experimental.macros

import scala.reflect.runtime.{universe => ru}

object Lifter {
  def m  = macro implementations.m

  object implementations {
    def m(c: Context): c.Expr[Unit] = {
      import c.universe._

      val testTree: c.universe.Tree = c.resetAllAttrs(c.enclosingUnit.body)

      println(PrettyPrinter.show(testTree.asInstanceOf[PrettyPrinter.global.Tree], c.enclosingUnit))

      c.literalUnit
    }
  }
}