package com.hadihariri.markcode

import java.io.File


object Synthetics {
    val imports = File("./imports.txt").readLines().map {
        Pair(it.substringBefore("="), it.substringAfter("="))
    }.toMap()
    val prefixes = File("./prefixes.txt").readLines().map {
        Pair(it.substringBefore("="), it.substringAfter("="))
    }.toMap()

}
