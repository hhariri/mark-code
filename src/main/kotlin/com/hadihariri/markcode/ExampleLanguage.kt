package com.hadihariri.markcode

interface ExampleLanguage {
    fun getPackageName(dirName: String, fileName: String): String
    fun formatPackageStatement(packageName: String): String
    fun formatImportStatement(importName: String): String
    fun formatMainFunction(fileName: String): String
    fun formatMainFunctionIndent(): String
    fun formatMainFunctionEnd(): String
    fun hasAnyDeclarations(lines: Collection<String>): Boolean
}