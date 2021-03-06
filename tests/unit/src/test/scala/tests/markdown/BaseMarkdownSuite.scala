package tests.markdown

import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import mdoc.Reporter
import scala.meta.inputs.Input
import scala.meta.io.AbsolutePath
import scala.meta.testkit.DiffAssertions
import tests.markdown.StringSyntax._
import mdoc.internal.cli.Context
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Markdown
import mdoc.internal.markdown.MarkdownCompiler

abstract class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val tmp = AbsolutePath(Files.createTempDirectory("mdoc"))
  protected def baseSettings: Settings =
    Settings
      .default(tmp)
      .copy(
        site = Map(
          "version" -> "1.0"
        )
      )
  private val myStdout = new ByteArrayOutputStream()
  private def newReporter(): ConsoleReporter = new ConsoleReporter(new PrintStream(myStdout))
  private val compiler = MarkdownCompiler.fromClasspath("")
  private def newContext(settings: Settings, reporter: Reporter) =
    Context(settings, reporter, compiler)

  def getMarkdownSettings(context: Context): MutableDataSet = {
    myStdout.reset()
    Markdown.mdocSettings(context)
  }

  def checkError(
      name: String,
      original: String,
      expected: String,
      settings: Settings = baseSettings
  ): Unit = {
    test(name) {
      val reporter = newReporter()
      val context = newContext(settings, reporter)
      val input = Input.VirtualFile(name + ".md", original)
      Markdown.toMarkdown(input, getMarkdownSettings(context), reporter, settings)
      assert(reporter.hasErrors, "Expected errors but reporter.hasErrors=false")
      val obtainedErrors = fansi.Str(myStdout.toString).plainText.trimLineEnds
      assertNoDiffOrPrintExpected(obtainedErrors, expected)
    }
  }

  def check(
      name: String,
      original: String,
      expected: String,
      settings: Settings = baseSettings
  ): Unit = {
    test(name) {
      val reporter = newReporter()
      val context = newContext(settings, reporter)
      val input = Input.VirtualFile(name + ".md", original)
      val obtained =
        Markdown.toMarkdown(input, getMarkdownSettings(context), reporter, settings).trimLineEnds
      val stdout = fansi.Str(myStdout.toString()).plainText
      assert(!reporter.hasErrors, stdout)
      assertNoDiffOrPrintExpected(obtained, expected)
    }
  }

}
