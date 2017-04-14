package com.hadihariri.markcode

import org.jetbrains.kotlinBook.codegen.Chapter
import org.jetbrains.kotlinBook.codegen.writeVerifyAllSamples
import java.io.File


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


