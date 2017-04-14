package com.hadihariri.markcode

class SectionCounter(chapterNumber: Int) {
    private val sectionNumber = intArrayOf(chapterNumber, 0, 0, 0)

    fun increment(level: Int) {
        if (level > 1) {
            sectionNumber[level - 1]++
            for (i in level..3) {
                sectionNumber[i] = 0
            }
        }
    }

    fun currentSectionNumber(): String {
        return sectionNumber.filter { it != 0 }.joinToString(separator = ".")
    }
}