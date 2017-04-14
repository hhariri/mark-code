package com.hadihariri.markcode

data class SourceMetadata(val language: String?,
                          val filename: String?,
                          val prependFilenames: List<String>,
                          val append: Boolean,
                          val mainFilename: String?,
                          val skip: Boolean,
                          val noverify: Boolean,
                          val jvmname: Boolean,
                          val imports: List<String>)