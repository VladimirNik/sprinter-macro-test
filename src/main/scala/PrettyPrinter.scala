package ch.epfl.lamp.sprintertest

import scala.reflect.api.Universe
import scala.sprinter.printers.PrettyPrinters

import scala.tools.nsc
import scala.tools.nsc._
import scala.tools.nsc.util._
import scala.tools.nsc.reporters._
import scala.tools.nsc.io._
import java.io._
import scala.reflect.macros.Context

import scala.tools.refactoring.Refactoring
import scala.tools.refactoring.common.{Tracing, CompilerAccess}
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

  def getCompiler = global

  def getInteractiveCompiler(global: nsc.Global) = {
    val comp = new nsc.interactive.Global(global.settings, global.reporter)
    try {
      comp.ask { () =>
        new comp.Run
      }
    } catch {
      case e: MissingRequirementError =>
        val msg = s"""Could not initialize the compiler!""".stripMargin
        throw new Exception(msg, e)
    }
    comp
  }

  val printers = PrettyPrinters(global)
   
  def show(tree: global.Tree, unit: Context#CompilationUnit): String = {

    trait GlobalRefCompiler extends Refactoring with CompilerAccess {
      val global: nsc.Global
      import global._

      def compilationUnitOfFile(f: AbstractFile): Option[global.CompilationUnit] = Option(unit.asInstanceOf[global.CompilationUnit])

      def generatePrint(tree: Tree, changeset: ChangeSet = AllTreesHaveChanged, sourceFile: Option[scala.reflect.internal.util.SourceFile]): String = {
        val initialIndentation = if(tree.hasExistingCode) indentationString(tree) else ""
        val in = new Indentation(defaultIndentationStep, initialIndentation)

        print(tree, PrintingContext(in, changeset, tree, sourceFile)).asText
      }

      def print(tree: global.Tree): String = {
        generatePrint(tree, sourceFile = None)
      }
    }

    trait InterRefCompiler extends GlobalRefCompiler {
      val global: nsc.interactive.Global

      def cleanTree(t: global.Tree) = {
        global.ask{ () =>
        val removeAuxiliaryTrees = ↓(transform {
          case t: global.Tree if (t.pos == global.NoPosition || t.pos.isRange) => t
          case t: global.ValDef => global.emptyValDef

          case t: global.Select if t.name.isTypeName && t.name.toString != "AnyRef" => t
          case t => global.EmptyTree
        })

        (removeAuxiliaryTrees &> topdown(setNoPosition))(t).get
        }
      }

      def shutdown() =
        global.askShutdown()

      override def print(tree: global.Tree): String = {
        generatePrint(cleanTree(tree), sourceFile = None)
      }
    }

    object GlobalRefInstance extends GlobalRefCompiler {
      val global = getCompiler
    }

    object InterRefInstance extends InterRefCompiler {
      val global: nsc.interactive.Global = getInteractiveCompiler(getCompiler)
    }

    val res = InterRefInstance.print(tree.asInstanceOf[InterRefInstance.global.Tree])
    InterRefInstance.shutdown()
    res
  }

}
