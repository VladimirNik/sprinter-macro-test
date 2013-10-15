package ch.epfl.lamp.sprintertest

import scala.reflect.macros.Context
import language.experimental.macros

import scala.reflect.runtime.{universe => ru}

object Lifter {
  def m  = macro implementations.m

  object implementations {
    def m(c: Context): c.Expr[Unit] = {
      import c.universe._

      val testTree: Tree = c.resetAllAttrs(c.enclosingUnit.body)

      val testAny: Any = testTree
      println("testAny.isInstanceOf[PrettyPrinter.global.Tree]: " + testAny.isInstanceOf[PrettyPrinter.global.Tree])
      testAny match {
        case gt: PrettyPrinter.global.Tree => println("testAny - global.Tree - Match")
        case _ => println("testAny - global.Tree - Doesn't match")
      }

      val testGlobal: PrettyPrinter.global.Tree = testAny.asInstanceOf[PrettyPrinter.global.Tree]
      println("testGlobal.isInstanceOf[PrettyPrinter.global.Tree]: " + testTree.isInstanceOf[PrettyPrinter.global.Tree])
      testGlobal match {
        case gt: PrettyPrinter.global.Tree => println("testGlobal - global.Tree - Match")
        case _ => println("testGlobal - global.Tree - Doesn't match")
      }

      println("=========")
      println()

      println(PrettyPrinter.show(testTree))

      c.literalUnit
    }
  }
}