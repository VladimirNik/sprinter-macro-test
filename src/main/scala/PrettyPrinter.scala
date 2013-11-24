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

    trait TestGlobalSettings extends Refactoring with CompilerAccess {
      val global: nsc.Global
      import global._

      def cleanTree(t: global.Tree) = {
        //global.ask{ () =>
        val removeAuxiliaryTrees = ↓(transform {
          case t: global.Tree if (t.pos == global.NoPosition || t.pos.isRange) => t
          case t: global.ValDef => global.emptyValDef
          // We want to exclude "extends AnyRef" in the pretty printer tests
          case t: global.Select if t.name.isTypeName && t.name.toString != "AnyRef" => t
          case t => global.EmptyTree
        })

        (removeAuxiliaryTrees &> topdown(setNoPosition))(t).get
        //}
      }

      def compilationUnitOfFile(f: AbstractFile): Option[global.CompilationUnit] = Option(unit.asInstanceOf[global.CompilationUnit])

      def generatePrint(tree: Tree, changeset: ChangeSet = AllTreesHaveChanged, sourceFile: Option[scala.reflect.internal.util.SourceFile]): String = {

        val initialIndentation = if(tree.hasExistingCode) indentationString(tree) else ""
        val in = new Indentation(defaultIndentationStep, initialIndentation)
        //            scala.sprinter.printers.PrettyPrinters.apply(global).show(tree)
        print(tree, PrintingContext(in, changeset, tree, sourceFile)).asText
      }

      def print(tree: global.Tree): String = {
        val res = generatePrint(cleanTree(tree), sourceFile = None)
//        val res = generatePrint(tree, sourceFile = None)
        res
      }
    }

    trait TestInterGlobalSettings extends TestGlobalSettings {
      val global: nsc.interactive.Global


//      val testTree = treeFrom(sourceStr)

      override def cleanTree(t: global.Tree) = {
        global.ask{ () =>
          super.cleanTree(t)
        }
      }

      def shutdown() =
        global.askShutdown()
    }

    object TestGlobal extends TestGlobalSettings {
      val global = getCompiler
    }

    object TestInterGlobal extends TestInterGlobalSettings {
      val global: nsc.interactive.Global = getInteractiveCompiler(getCompiler)

      val file = new scala.reflect.internal.util.BatchSourceFile("fileName", sourceStr)
      val testTree = global.parseTree(file)
    }

//    printers.show(tree.asInstanceOf[Global#Tree], PrettyPrinters.AFTER_NAMER)
    val res = TestGlobal.print(tree.asInstanceOf[TestGlobal.global.Tree])
    TestInterGlobal.shutdown()
    res
  }

  val sourceStr = """
      case class Test {
        import scala.collection.mutable

//        val (x, y) = (5, "ggg")
//
//        val List(a, _*) = List(1,2,3)

        val z: List[Int] = null
        val f = List(1,2,3)
        z match {
          case Nil => println("1")
          case List(x) => x
          case List(x,y) => y
          case List(x,y,z) => z
          case List(x, _*) => x
          case _ =>
        }

        val x: mutable.Map[Int, Int] = null
      }
                  """
}
