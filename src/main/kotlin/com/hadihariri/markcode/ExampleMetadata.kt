package com.hadihariri.markcode



interface ExampleMetadata {
    fun parse(line: String): SourceMetadata
}