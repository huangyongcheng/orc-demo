package com.example.orcdemo2.ml.ocr.extractor

object ProductNameExtractor {

    /**
     * Cleans leading and trailing dots, commas, and spaces from a string.
     *
     * This function is typically used to sanitize item names or tokens that may
     * have extra punctuation or whitespace at the beginning or end due to OCR noise.
     *
     * Only the start and end of the string are affected—internal punctuation is preserved.
     *
     * @param text The input string (can be null or blank).
     * @return A cleaned string with no leading/trailing dots, commas, or spaces. Returns
     *         an empty string if the input is null or blank.
     *
     * Examples:
     * - "reiniger PowerGel," => "reiniger PowerGel"
     * - "  ,Apfel." => "Apfel"
     * - "..Hello, world.. " => "Hello, world"
     * - null => ""
     * - "   " => ""
     */
    private fun cleanDotsAndCommas(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text
            .replace(Regex("^[.,\\s]+"), "")
            .replace(Regex("[.,\\s]+$"), "")
    }

    /**
     * Cleans an OCR text line while preserving the item name and value.
     *
     * This function removes special characters (e.g., "*", "#"), leading numeric codes,
     * and common irrelevant tokens (like standalone "X"). It is designed to clean item
     * descriptions while retaining meaningful content such as product names and prices.
     *
     * Useful in parsing item lines on receipts or invoices where product info is
     * prefixed by metadata or control characters.
     *
     * Logic steps:
     * - Remove all non-word characters except accented letters, spaces, commas, periods, and hyphens.
     * - Remove leading numeric words (e.g., product codes).
     * - Remove standalone "X" tokens (e.g., "X Kalb.Schnitzel" → "Kalb.Schnitzel").
     * - Normalize whitespace to avoid extra spaces.
     *
     * @param text The input raw OCR line (possibly noisy).
     * @return A cleaned string with product name and value preserved.
     *
     * Examples:
     * - "*000005 Diesel Fuel Save 53,15 EUR #A*" => "Diesel Fuel Save 53,15 EUR A"
     * - "X Kalb.Schnitzel Sala" => "Kalb.Schnitzel Sala"
     * - "#1234 X Milch 1L" => "Milch 1L"
     */
    fun cleanTextKeepName(text: String?): String? {
        val noSpecialChars = text?.replace(Regex("""[^\w\sÀ-ỹ,.\-]"""), "")
        val words = noSpecialChars?.trim()?.split(Regex("\\s+"))
        val resultWords = words?.dropWhile { it.matches(Regex("""\d+""")) }
        var result = resultWords?.joinToString(" ")
        result = result?.replace(Regex("""(?i)\b[x]\b"""), "")
        result = result?.trim()?.replace(Regex("\\s{2,}"), " ")
        return result
    }

    /**
     * Extracts the product name (or meaningful prefix text) before the first number in a string.
     *
     * This function is designed for use with OCR invoice or receipt lines, where numeric
     * values (quantities, prices, codes) are mixed with product names. It extracts the
     * leading non-numeric portion—typically the item description—before the first digit.
     *
     * If no such prefix exists, it attempts to extract a meaningful suffix after the last
     * dot or comma as a fallback, skipping numeric and non-word tokens.
     *
     * Additionally, it trims leading/trailing dots, commas, and spaces via `cleanDotsAndCommas()`.
     *
     * @param text The input OCR line that potentially includes item name, quantity, or price.
     * @return A cleaned string representing the item name or label.
     *
     * Examples:
     * - "Pflanzen erm. 6,99 x 3 Posten 3" => "Pflanzen erm"
     * - "3K 2,35 Prof. kehrgarnitur" => "kehrgarnitur"
     * - "   12,45 EUR Tomaten  " => "Tomaten"
     * - null or blank => ""
     */
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