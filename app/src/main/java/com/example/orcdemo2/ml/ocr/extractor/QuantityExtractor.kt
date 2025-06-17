package com.example.orcdemo2.ml.ocr.extractor

object QuantityExtractor {

    private val quantityPatterns = listOf(
        Regex("""(?i)\bqty[:\s]*([0-9]+[.,]?[0-9]*)\s+(.+)"""),
        Regex("""([0-9]+[.,]?[0-9]*)\s*[xX]\s*(.+)"""),
        Regex("""([0-9]+[.,]?[0-9]*)\s+(.+)""")
    )

    /**
     * Extracts a quantity value from a line of text using known unit keywords.
     *
     * This function is designed to detect and extract quantities that are written
     * either before or after common unit words in invoices (e.g., "2 Stück", "Stück 2").
     * If a matching quantity is found, it returns a pair with the quantity as a string
     * and `null` as the second value. If no match is found, it falls back to `extractQuantityByX()`.
     *
     * Recognized unit keywords include: "stück", "kg", "flasche", "packung",
     * "einheit", "dose", "päckchen", "posten:"
     *
     * Matching supports both formats:
     * - Quantity before unit: "2 Stück"
     * - Unit before quantity: "Stück 2"
     *
     * @param text The input text from an OCR line that may contain quantity and unit.
     * @return A Pair of (quantity, null) if found; otherwise, the result from `extractQuantityByX`.
     *
     * Examples:
     * -  "2 Stück" =>true, "Stück 2" =>true
     */
    fun extractQuantityByKeyword(text: String, index:Int): Pair<String?, String?>? {
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
            val extractQuantityByX = extractQuantityByX(text)
            if (extractQuantityByX == null) {
                return extractQuantityByPart(text,index)
            } else {
                return extractQuantityByX
            }
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



    private fun extractQuantityByPart(text: String,index: Int): Pair<String, String?>? {

        if(index!=0) return null
        val trimmed = text.trim()
        for (pattern in quantityPatterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size >= 3) {
                val quantity = match.groupValues[1]
                if(isValidQuantity(quantity)) {
                    val name = match.groupValues[2].trim()
                    return Pair(quantity, name)
                } else{
                    return null
                }
            }
        }
        return null
    }

    private fun isValidQuantity(input: String): Boolean {
        if (input.contains(',') || input.contains('.')) return false

        if (input.length > 1 && input.startsWith("0")) return false

        if (!input.matches(Regex("""^\d+$"""))) return false

        return try {
            val value = input.toInt()
            value in 0..100
        } catch (e: NumberFormatException) {
            false
        }
    }
}