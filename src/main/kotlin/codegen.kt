package org.jetbrains.kotlinBook.codegen

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

val syntheticImports = mapOf(
        "Comparator" to "java.util.Comparator",
        "TreeMap" to "java.util.TreeMap",
        "ArrayList" to "java.util.ArrayList",
        "BufferedReader" to "java.io.BufferedReader",
        "FileReader" to "java.io.FileReader",
        "StringReader" to "java.io.StringReader",
        "InputStreamReader" to "java.io.InputStreamReader",
        "File" to "java.io.File",
        "HashSet" to "java.util.HashSet",
        "Date" to "java.util.Date",
        "Collections" to "java.util.Collections",
        "BigDecimal" to "java.math.BigDecimal",
        "Serializable" to "java.io.Serializable",
        "PropertyChangeSupport" to "java.beans.PropertyChangeSupport",
        "PropertyChangeListener" to "java.beans.PropertyChangeListener",
        "Delegates" to "kotlin.properties.Delegates",
        "KClass" to "kotlin.reflect.KClass",
        "KProperty" to "kotlin.reflect.KProperty",
        "memberProperties" to "kotlin.reflect.memberProperties",
        "compareValuesBy" to "kotlin.comparisons.compareValuesBy",
        "@Before" to "org.junit.Before",
        "@Test" to "org.junit.Test",
        "Assert" to "org.junit.Assert",
        "Period" to "java.time.Period",
        "LocalDate" to "java.time.LocalDate"
)

val syntheticPrefixes = mapOf(
        "getFacebookName" to "fun getFacebookName(accountId: Int) = \"fb:\$accountId\""
)

fun captionToFilename(caption: String, captionIndex: Int): String {
    val name = StringBuilder()
    var capitalize = true
    caption.forEach {
        if (it == ' ') {
            capitalize = true
        } else if (it.isLetterOrDigit()) {
            name.append(if (capitalize) it.toUpperCase() else it)
            capitalize = false
        }
    }
    return name.toString() + (if (captionIndex == 0) "" else "$captionIndex")
}

data class SourceMetadata(val language: String?,
                          val filename: String?,
                          val prependFilenames: List<String>,
                          val append: Boolean,
                          val mainFilename: String?,
                          val skip: Boolean,
                          val noverify: Boolean,
                          val jvmname: Boolean,
                          val imports: List<String>)

val EMPTY_SOURCE_METADATA = SourceMetadata(null, null, emptyList(), false, null, false, false, false, emptyList())

fun parseMetadata(line: String): SourceMetadata {
    val parts = line.trimStart('[').trimEnd(']').split(',')
    val language = parts.getOrNull(1)
    var fileName: String? = null
    val prependFilenames = arrayListOf<String>()
    var mainFilename: String? = null
    var append = false
    var skip = false
    var noverify = false
    var jvmname = false
    val imports = arrayListOf<String>()
    for (part in parts.drop(2)) {
        if (part.startsWith("prepend=")) {
            prependFilenames.add(part.removePrefix("prepend="))
        }
        else if (part.startsWith("main=")) {
            mainFilename = part.removePrefix("main=")
        }
        else if (part.startsWith("import=")) {
            imports.add(part.removePrefix("import="))
        }
        else if (part == "append") {
            append = true
        }
        else if (part == "skip") {
            skip = true
        }
        else if (part == "noverify") {
            noverify = true
        }
        else if (part == "jvmname") {
            jvmname = true
        }
        else {
            fileName = part
        }
    }

    return SourceMetadata(language, fileName, prependFilenames, append, mainFilename, skip, noverify, jvmname, imports)
}

val annotationRegex = Regex("<(\\d+)>")

fun String.startsWithAny(vararg prefixes: String) = prefixes.any { startsWith(it) }

interface ExampleLanguage {
    fun getPackageName(dirName: String, fileName: String): String
    fun formatPackageStatement(packageName: String): String
    fun formatImportStatement(importName: String): String
    fun formatMainFunction(fileName: String): String
    fun formatMainFunctionIndent(): String
    fun formatMainFunctionEnd(): String
    fun hasAnyDeclarations(lines: Collection<String>): Boolean
}

object JavaLanguage : ExampleLanguage {
    override fun getPackageName(dirName: String, fileName: String) = dirName
    override fun formatPackageStatement(packageName: String) = "package $packageName;"
    override fun formatImportStatement(importName: String) = "import $importName;"

    override fun formatMainFunction(fileName: String): String {
        val className = fileName.substringBefore('.')
        return "public class $className {\n    public static void main(String[] args) {"
    }

    override fun formatMainFunctionIndent(): String = "        "
    override fun formatMainFunctionEnd(): String = "    }\n}"

    override fun hasAnyDeclarations(lines: Collection<String>) = true
}

object KotlinLanguage : ExampleLanguage {
    override fun getPackageName(dirName: String, fileName: String): String {
        if (fileName[0] in '0'..'9')
            return dirName + ".ex" + fileName.substringAfter('.').removeSuffix(".kt").replace('.', '_')
        return dirName + "." + fileName.removeSuffix(".kt")
    }

    override fun formatPackageStatement(packageName: String) = "package $packageName"

    override fun formatImportStatement(importName: String) = "import $importName"

    override fun formatMainFunction(fileName: String) = "fun main(args: Array<String>) {"
    override fun formatMainFunctionIndent() = "    "
    override fun formatMainFunctionEnd() = "}"

    override fun hasAnyDeclarations(lines: Collection<String>) = lines.any {
        it.startsWithAny("fun", "inline fun", "operator fun", "class", "enum class", "interface", "open class", "data class", "abstract class")
    }
}

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
            if (lineBeforeAnno.length > 55) {
                reportLineError(lineNumber, "Code line before annotation is too long (55 characters max)")
            }
            processedLine = lineBeforeAnno
        } else {
            if (line.trimEnd().length > 76) {
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
            for (prefix in collectSyntheticElements(syntheticImports)) {
                append(language.formatImportStatement(prefix)).append("\n")
                anyPrefixes = true
            }
            for (prefix in collectSyntheticElements(syntheticPrefixes)) {
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

class SectionCounter(chapterNumber: Int) {
    private val sectionNumber = intArrayOf(chapterNumber, 0, 0, 0)

    fun increment(level: Int) {
        if (level > 1) {
            sectionNumber[level - 1]++
            for (i in level..3) {
                sectionNumber[i] = 0
            }
        }
    }

    fun currentSectionNumber(): String {
        return sectionNumber.filter { it != 0 }.joinToString(separator = ".")
    }
}

fun extractChapterNumber(filename: String): Int {
    if (filename.startsWith("ch")) {
        return Integer.parseInt(filename.removePrefix("ch").substringBeforeLast("."))
    }
    return 0
}

class Chapter(val chapterFile: File, val chapterCodeDir: File) {
    val examples = LinkedHashMap<String, CodeExample>()
    val examplesBySection = mutableMapOf<String, MutableList<CodeExample>>()
    val sectionCounter = SectionCounter(extractChapterNumber(chapterFile.name))

    fun process(writeExpectedOutput: Boolean) {
        var currentCaption = chapterFile.nameWithoutExtension
        var currentExample: CodeExample? = null
        var lastExample: CodeExample? = null
        var exampleIndex = 1
        var inBlockComment = false

        var currentCaptionIndex = 0
        var sourceMetadataLine: String? = null
        var expectedExampleLineNumber = -1
        var prevLine: String? = null

        for ((lineNumber, line) in chapterFile.readLines().withIndex()) {
            if (currentExample != null) {
                if (line.startsWith("----")) {
                    lastExample = currentExample
                    currentExample = null
                }
                else {
                    currentExample.addLine(lineNumber + 1, line)
                }
            }
            else {
                if (line.startsWith("////")) {
                    inBlockComment = !inBlockComment
                }
                else if (!inBlockComment) {
                    if (line.startsWith("=")) {
                        val caption = parseCaption(lineNumber, line, prevLine)
                        if (!caption.isEmpty()) {
                            currentCaption = caption
                            currentCaptionIndex = 0
                        }
                    }
                    else if (line.startsWith("[source,")) {
                        sourceMetadataLine = line
                        expectedExampleLineNumber = lineNumber + 1
                    }
                    else if (line.startsWith(".") && lineNumber == expectedExampleLineNumber) {
                        expectedExampleLineNumber++
                    }
                    else if (line.startsWith("----")) {
                        val sourceMetadata = if (expectedExampleLineNumber == lineNumber)
                            parseMetadata(sourceMetadataLine!!)
                        else
                            null

                        if (sourceMetadata?.append == true) {
                            currentExample = lastExample
                        }
                        else {
                            val filename = sourceMetadata?.filename ?: (captionToFilename(currentCaption, currentCaptionIndex) + ".kt")

                            val prependExamples = sourceMetadata?.prependFilenames?.map {
                                examples [it] ?: throw IllegalStateException("Can't find example to prepend: $it")
                            } ?: emptyList()
                            val mainExample = sourceMetadata?.mainFilename?.let {
                                examples[it] ?: throw IllegalStateException("Can't find example to take 'main' from: $it")
                            }

                            val skip = if (sourceMetadata?.language == "kotlin")
                                sourceMetadata.skip
                            else if (sourceMetadata?.language == "java")
                                sourceMetadata.filename == null
                            else
                                true

                            val sectionNumber = sectionCounter.currentSectionNumber()

                            currentExample = CodeExample(this, filename,
                                    if (sourceMetadata?.language == "java") JavaLanguage else KotlinLanguage,
                                    prependExamples, mainExample, skip, sourceMetadata ?: EMPTY_SOURCE_METADATA)
                            if (examples[filename] != null) {
                                throw IllegalStateException("Duplicate filename $filename")
                            }
                            if (sourceMetadata?.filename != null || currentExample.willWriteOutputFile()) {
                                examples.put(filename, currentExample)
                            }
                            if (currentExample.willWriteOutputFile()) {
                                examplesBySection.getOrPut(sectionNumber, { ArrayList() }).add(currentExample)
                                exampleIndex++
                                currentCaptionIndex++
                            }
                        }
                    }
                }
            }
            prevLine = line
        }

        for ((sectionNumber, examples) in examplesBySection) {
            if (examples.size == 1) {
                examples[0].updateOutputFileName("${sectionNumber}_${examples[0].filename}")
            }
            else {
                for ((i, example) in examples.withIndex()) {
                    example.updateOutputFileName("${sectionNumber}_${i+1}_${example.filename}")
                }
            }
        }

        examples.values.forEach {
            it.process(chapterCodeDir, writeExpectedOutput)
        }
    }

    private fun parseCaption(lineNumber: Int, line: String, prevLine: String?): String {
        val level = line.takeWhile { it == '=' }.length
        if (level > 4) {
            System.err.println("${chapterFile.name}:$lineNumber: Heading level too large")
        }
        else {
            sectionCounter.increment(level)
        }
        if (prevLine?.startsWith("[exampleprefix=") == true) {
            return prevLine.substringAfter('=').removeSuffix("]")
        }
        return line.trimStart('=').trimStart()
    }

    val hasErrors: Boolean get() = examples.values.any { it.hasErrors }
}

data class ExampleOutput(val functionName: String, val expectedOutputFile: String, val location: String)

fun writeVerifyAllSamples(chapters: List<Chapter>, outputDir: File) {
    BufferedWriter(FileWriter(File(outputDir, "VerifyAllSamples.kt"))).use { outputFile ->
        outputFile.write("import org.jetbrains.kotlinBook.OutputVerifier\n")

        val examples = mutableListOf<ExampleOutput>()

        for (chapter in chapters) {
            for (example in chapter.examples.values.filter { !it.skip && it.language is KotlinLanguage && it.hasOutputToVerify }) {
                val fqName = example.packageName ?: continue
                val import = fqName.replace('.', '_')
                outputFile.write("import $fqName.main as $import\n")
                examples.add(ExampleOutput(
                        import,
                        "${chapter.chapterCodeDir.name}/${example.expectedOutputFileName}",
                        "${example.chapter.chapterFile.name}:${example.expectedOutputStartLine ?: example.mainExample?.expectedOutputStartLine}"))
            }
        }

        outputFile.write("\n\nfun main(args: Array<String>) {\n")
        outputFile.write("    val verifier = OutputVerifier()\n")
        for ((function, expectedOutput, location) in examples) {
            outputFile.write("    verifier.verifySample(::$function, \"$expectedOutput\", \"$location\")\n")
        }
        outputFile.write("    verifier.report()\n}\n")
    }
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: java -jar asciidoc-source-verifier.jar <book directory> <examples output directory> [-o]")
        return
    }

    val writeExpectedOutput = args.size > 2 && args[2] == "-o"
    val chapters = mutableListOf<Chapter>()
    File(args[0]).listFiles { file, name -> name.endsWith(".adoc") }.forEach {
        val chapterCodeDir = File(args[1], it.nameWithoutExtension )
        val chapter = Chapter(it, chapterCodeDir)
        chapters.add(chapter)
        chapter.process(writeExpectedOutput)
    }
    if (writeExpectedOutput) {
        writeVerifyAllSamples(chapters, File(args[1]))
    }
    if (chapters.any { it.hasErrors }) {
        System.exit(1)
    }
}
