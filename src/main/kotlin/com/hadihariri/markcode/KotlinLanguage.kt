package com.hadihariri.markcode

import com.hadihariri.markcode.ExampleLanguage
import com.hadihariri.markcode.startsWithAny

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