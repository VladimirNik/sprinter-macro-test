package ch.epfl.lamp.sprintertest

import scala.reflect.api.Universe
import scala.sprinter.printers.PrettyPrinters

import scala.tools.nsc._
import scala.tools.nsc.util._
import scala.tools.nsc.reporters._
import scala.tools.nsc.io._
import java.io._
import scala.reflect.macros.Context

import scala.tools.refactoring.Refactoring
import scala.tools.refactoring.common.CompilerAccess
import scala.reflect.io.AbstractFile
import scala.tools.refactoring.sourcegen.ReusingPrinter

object PrettyPrinter {

  val global: Global = {
    val settings = new Settings()

    val COLON = System getProperty "path.separator"

    settings.classpath.value = this.getClass.getClassLoader match {
      case ctx: java.net.URLClassLoader => ctx.getURLs.map(_.getPath).mkString(COLON)
      case _                            => System.getProperty("java.class.path")
    }
    settings.bootclasspath.value = Predef.getClass.getClassLoader match {
      case ctx: java.net.URLClassLoader => ctx.getURLs.map(_.getPath).mkString(COLON)
      case _                            => System.getProperty("sun.boot.class.path")
    }

    settings.encoding.value = "UTF-8"
    settings.outdir.value = "."
    settings.extdirs.value = ""

    val reporter = new ConsoleReporter(settings, null, new PrintWriter(System.out)) //writer
    new Global(settings, reporter)
  }

  val printers = PrettyPrinters(global)
   
  def show(tree: global.Tree, unit: Context#CompilationUnit): String = {
          val compiler = global

          object RefObj extends Refactoring with CompilerAccess {
            val global = compiler
            def compilationUnitOfFile(f: AbstractFile) = Option(unit.asInstanceOf[global.CompilationUnit])
//            override def print(t: global.Tree, pc: PrintingContext) = {
//
//              val test1 = t != null
//              val test2 = !isEmptyTree(t)
//              val test3 = t.pos == global.NoPosition
//              val test4 = t.pos
//              println("test1 = " + test1)
//              println("test2 = " + test2)
//              println("test3 = " + test3)
//              println("test4 = " + test4)

//              //val printed = reusingPrinter.dispatchToPrinter(t.asInstanceOf[this.global.Tree], pc.asInstanceOf[this.PrintingContext])t
//                val printed = prettyPrinter.dispatchToPrinter(t.asInstanceOf[this.global.Tree], pc.asInstanceOf[this.PrintingContext])
////              val printed = super.print(t, pc)
//              val test5 = t.pos.isRange
//              System.out.println("test5 = " + test5)
//              printed
//            }
          }

          val initialIndentation = ""
          val in = new RefObj.Indentation(RefObj.defaultIndentationStep, initialIndentation)

          object AllTreesNotChanged extends RefObj.ChangeSet {
            def hasChanged(t: RefObj.global.Tree) = false
          }
          val printingContext = RefObj.PrintingContext(in, RefObj.AllTreesHaveChanged, tree.asInstanceOf[RefObj.global.Tree], None)
          
          //RefObj.print(tree.asInstanceOf[RefObj.global.Tree], printingContext).asText
          RefObj.prettyPrinter.dispatchToPrinter(tree.asInstanceOf[RefObj.global.Tree], printingContext).asText
//          RefObj.reusingPrinter.dispatchToPrinter(tree.asInstanceOf[RefObj.global.Tree], printingContext).asText
//          RefObj.createText(tree.asInstanceOf[RefObj.global.Tree])

//        printers.show(tree.asInstanceOf[Global#Tree], PrettyPrinters.AFTER_NAMER)
  }
}
