package com.example.utils

object TextChunker {
    data class Chunk(val text: String, val start: Int, val end: Int, val isSpeakable: Boolean)

    fun parse(text: String): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        val pattern = java.util.regex.Pattern.compile(".*?([.,;!?\\n]+(?:\\s+|$))|.+")
        val matcher = pattern.matcher(text)
        var currentIndex = 0
        while (matcher.find()) {
            val str = matcher.group()
            if (str.isNotEmpty()) {
                chunks.add(Chunk(str, currentIndex, currentIndex + str.length, str.isNotBlank()))
                currentIndex += str.length
            }
        }
        if (currentIndex < text.length) {
            val rem = text.substring(currentIndex)
            chunks.add(Chunk(rem, currentIndex, text.length, rem.isNotBlank()))
        }
        return chunks
    }
}
