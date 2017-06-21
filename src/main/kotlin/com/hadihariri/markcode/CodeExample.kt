package com.hadihariri.markcode

import com.hadihariri.markcode.Synthetics
import com.hadihariri.markcode.annotationRegex
import com.hadihariri.markcode.Chapter
import com.hadihariri.markcode.ExampleLanguage
import com.hadihariri.markcode.KotlinLanguage
import com.hadihariri.markcode.SourceMetadata
import java.io.File
import java.util.*

class CodeExample(val chapter: Chapter,
                  var filename: String,
                  val language: ExampleLanguage,
                  val prependExamples: List<CodeExample>,
                  val mainExample: CodeExample?,
                  val skip: Boolean,
                  val sourceMetadata: SourceMetadata) {

    private val rawLines = TreeMap<Int, String>()
    private val text = StringBuilder()
    private val textWithoutMain = StringBuilder()
    private val textOfMain = StringBuilder()
    val expectedOutput = StringBuilder()
    var hasErrors: Boolean = false
        private set
    var packageName: String? = null
    var expectedOutputStartLine: Int? = null

    fun updateOutputFileName(filename: String) {
        if (language == KotlinLanguage) {
            this.filename = filename
        }
    }

    fun addLine(lineNumber: Int, line: String) {
        rawLines.put(lineNumber, line)
    }

    fun processLines() {
        text.setLength(0)
        val everythingInsideMain = (!skip && mainExample == null && rawLines.values.none { it.startsWith(">>>") } &&
            !language.hasAnyDeclarations(rawLines.values))

        var insideMain = false
        for ((lineNumber, line) in rawLines) {
            if (line.startsWith(">>>") || line.startsWith("...") || everythingInsideMain) {
                if (!insideMain) {
                    appendProcessedLine("\n${language.formatMainFunction(filename)}", true)
                    insideMain = true
                }
                val lineContent = line.removePrefix(">>> ").removePrefix("... ")
                appendSourceLine(language.formatMainFunctionIndent() + lineContent, lineNumber, insideMain)
            }
            else if (!insideMain) {
                appendSourceLine(line, lineNumber, insideMain)
            }
            else if (line.isNotEmpty()) {
                if (expectedOutputStartLine == null) {
                    expectedOutputStartLine = lineNumber
                }
                expectedOutput.append(checkRemoveAnnotation(line, lineNumber)).append("\n")
            }
        }

        if (insideMain) {
            appendProcessedLine(language.formatMainFunctionEnd(), true)
        }
    }

    private fun appendSourceLine(line: String, lineNumber: Int, insideMain: Boolean) {
        appendProcessedLine(checkRemoveAnnotation(line, lineNumber), insideMain)
    }

    private fun checkRemoveAnnotation(line: String, lineNumber: Int): String {
        val matchResult = annotationRegex.find(line)
        val processedLine: String
        if (matchResult != null) {
            val lineBeforeAnno = line.substring(0, matchResult.range.start).trimEnd()
            if (lineBeforeAnno.length > 150) {
                reportLineError(lineNumber, "Code line before annotation is too long (55 characters max)")
            }
            processedLine = lineBeforeAnno
        } else {
            if (line.trimEnd().length > 175) {
                reportLineError(lineNumber, "Code line is too long (76 characters max)")
            }
            processedLine = line.trimEnd()
        }
        return processedLine
    }

    private fun appendProcessedLine(processedLine: String, insideMain: Boolean) {
        text.append(processedLine).append("\n")
        if (!insideMain) {
            textWithoutMain.append(processedLine).append("\n")
        } else {
            textOfMain.append(processedLine).append("\n")
        }
    }

    fun process(outputDir: File, writeExpectedOutput: Boolean) {
        processLines()

        if (!skip) {
            outputDir.mkdir()
            val outputFile = getOutputFile(outputDir)
            val result = generateOutputText(outputDir.name)
            outputFile.writeText(result.toString())
            if ("fun main" !in result && outputFile.extension == "kt") {
                System.err.println("No 'main' function in ${outputFile.name}")
            }

            if (writeExpectedOutput && hasOutputToVerify) {
                val expectedOutputFile = getExpectedOutputFile(outputDir)
                if (expectedOutput.isNotEmpty()) {
                    expectedOutputFile.writeText(expectedOutput.toString())
                }
                else {
                    mainExample?.expectedOutput?.let {
                        expectedOutputFile.writeText(it.toString())
                    }
                }
            }
        }
    }

    val hasOutputToVerify: Boolean
        get() = (expectedOutput.isNotEmpty() || mainExample?.expectedOutput?.isNotEmpty() == true) && !sourceMetadata.noverify

    private fun generateOutputText(dirName: String): String {
        val result = buildString {
            if (sourceMetadata.jvmname) {
                append("@file:JvmName(\"${sourceMetadata.filename!!.substringBefore('.')}\")\n")
            }

            if (!rawLines.values.any { it.startsWith("package")}) {
                packageName = language.getPackageName(dirName, sourceMetadata.filename ?: filename)
                append(language.formatPackageStatement(packageName!!) + "\n\n")
            }

            var anyPrefixes = false
            for (prefix in collectSyntheticElements(Synthetics.imports)) {
                append(language.formatImportStatement(prefix)).append("\n")
                anyPrefixes = true
            }
            for (prefix in collectSyntheticElements(Synthetics.prefixes)) {
                append(prefix).append("\n")
                anyPrefixes = true
            }
            for (import in sourceMetadata.imports) {
                append(language.formatImportStatement(import) + "\n")
            }
            if (anyPrefixes || sourceMetadata.imports.isNotEmpty()) append("\n")

            for (prependExample in prependExamples) {
                append(prependExample.textWithoutMain).append("\n")
            }
            append(text)
            if (mainExample != null) {
                append(mainExample.textOfMain)
            }
        }
        return result.replace("\n\n\n", "\n\n")
    }

    private fun collectSyntheticElements(prefixMap: Map<String, String>): Collection<String> {
        return prefixMap.filter {
            entry -> entry.key in text.toString() ||
                prependExamples.any { entry.key in it.text.toString() } ||
                (mainExample?.textOfMain?.contains(entry.key) ?: false)
        }.values
    }

    fun willWriteOutputFile() = !skip

    fun reportLineError(lineNumber: Int, message: String) {
        System.err.println("${chapter.chapterFile.name} line $lineNumber: $message")
        hasErrors = true
    }

    fun getOutputFile(outputDir: File): File = File(outputDir, filename)
    fun getExpectedOutputFile(outputDir: File) = File(outputDir, expectedOutputFileName)
    val expectedOutputFileName: String
        get() = filename.replaceAfterLast(".", "txt")
}