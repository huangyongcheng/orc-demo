package com.example.orcdemo2.ml.ocr.extractor

object QuantityExtractor {

    fun extractQuantityByKeyword(text: String): Pair<String?, String?>? {
        val unitKeywords = listOf("stück", "kg", "flasche", "packung", "einheit", "dose", "päckchen", "posten:")
        val unitPattern = unitKeywords.joinToString("|")
        val quantityRegex = Regex(
            """\b(?:($unitPattern)\s*(\d{1,3}(?:[.,]\d{1,2})?)|(\d{1,3}(?:[.,]\d{1,2})?)\s*($unitPattern))\b""",
            RegexOption.IGNORE_CASE
        )
        val match = quantityRegex.find(text)
        val result = if (match != null) {
            match.groupValues[2].ifEmpty { match.groupValues[3] }
        } else {
            null
        }
        if (result == null) {
            return extractQuantityByX(text)
        } else {
            return Pair(result, null)
        }
    }

    private fun extractQuantityByX(text: String): Pair<String, String>? {
        val trimmed = text.trim()
        val regexPrefix = Regex("""(?:^|\s)(\d+)\s*[xX]\s+(.+)""")
        val matchPrefix = regexPrefix.find(trimmed)
        if (matchPrefix != null) {
            val quantity = matchPrefix.groupValues[1]
            val item = matchPrefix.groupValues[2].trim()
            return Pair(quantity, item)
        }
        val regexSuffix = Regex("""(.+?)\s+[xX]\s*(\d+)(?:\s|$)""")
        val matchSuffix = regexSuffix.find(trimmed)
        if (matchSuffix != null) {
            val item = matchSuffix.groupValues[1].trim()
            val quantity = matchSuffix.groupValues[2]
            return Pair(quantity, item)
        }
        return null
    }
}