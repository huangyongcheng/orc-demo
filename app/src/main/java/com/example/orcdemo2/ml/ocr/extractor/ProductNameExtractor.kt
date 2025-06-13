package com.example.orcdemo2.ml.ocr.extractor

object ProductNameExtractor {

    private fun cleanDotsAndCommas(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text
            .replace(Regex("^[.,\\s]+"), "")
            .replace(Regex("[.,\\s]+$"), "")
    }

    fun cleanTextKeepName(text: String?): String? {
        val noSpecialChars = text?.replace(Regex("""[^\w\sÀ-ỹ,.\-]"""), "")
        val words = noSpecialChars?.trim()?.split(Regex("\\s+"))
        val resultWords = words?.dropWhile { it.matches(Regex("""\d+""")) }
        var result = resultWords?.joinToString(" ")
        result = result?.replace(Regex("""(?i)\b[x]\b"""), "")
        result = result?.trim()?.replace(Regex("\\s{2,}"), " ")
        return result
    }

    fun extractTextBeforeFirstNumber(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val words = text.trim().split(Regex("\\s+"))
        val result = mutableListOf<String>()
        for (word in words) {
            if (word.contains(Regex("""\d"""))) {
                break
            }
            result.add(word)
        }
        if (result.isNotEmpty()) {
            return cleanDotsAndCommas(result.joinToString(" "))
        }
        val indexDot = text.lastIndexOf('.')
        val indexComma = text.lastIndexOf(',')
        val splitIndex = maxOf(indexDot, indexComma)
        if (splitIndex != -1 && splitIndex < text.length - 1) {
            val after = text.substring(splitIndex + 1).trim()
            val cleaned = after.split(Regex("\\s+"))
                .dropWhile { it.matches(Regex("""[\d.,\W_]+""")) }
                .joinToString(" ")
            return cleanDotsAndCommas(cleaned)
        }
        return ""
    }
}