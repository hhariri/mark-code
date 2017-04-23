package com.hadihariri.markcode

class DocMetadata : ExampleMetadata {
    override fun parse(line: String): SourceMetadata {
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
            } else if (part.startsWith("main=")) {
                mainFilename = part.removePrefix("main=")
            } else if (part.startsWith("import=")) {
                imports.add(part.removePrefix("import="))
            } else if (part == "append") {
                append = true
            } else if (part == "skip") {
                skip = true
            } else if (part == "noverify") {
                noverify = true
            } else if (part == "jvmname") {
                jvmname = true
            } else {
                fileName = part
            }
        }

        return SourceMetadata(language, fileName, prependFilenames, append, mainFilename, skip, noverify, jvmname, imports)
    }
}