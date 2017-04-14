package com.hadihariri.markcode

import com.hadihariri.markcode.ExampleLanguage

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