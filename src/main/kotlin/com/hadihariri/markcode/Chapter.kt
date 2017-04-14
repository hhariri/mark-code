package com.hadihariri.markcode


import java.io.File
import java.util.*

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