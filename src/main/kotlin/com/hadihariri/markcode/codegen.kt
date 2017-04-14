package com.hadihariri.markcode

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

val EMPTY_SOURCE_METADATA = SourceMetadata(null, null, emptyList(), false, null, false, false, false, emptyList())

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

fun extractChapterNumber(filename: String): Int {
    if (filename.startsWith("ch")) {
        return Integer.parseInt(filename.removePrefix("ch").substringBeforeLast("."))
    }
    return 0
}

fun writeVerifyAllSamples(chapters: List<Chapter>, outputDir: File) {
    BufferedWriter(FileWriter(File(outputDir, "VerifyAllSamples.kt"))).use { outputFile ->
        outputFile.write("import com.hadihariri.markcode\n")

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

