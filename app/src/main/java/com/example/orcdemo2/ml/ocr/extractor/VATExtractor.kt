package com.example.orcdemo2.ml.ocr.extractor

import android.util.Log
import com.example.orcdemo2.ml.ocr.Constants
import com.example.orcdemo2.ml.ocr.model.LayoutLine

object VATExtractor {

    /**
     * Merges a VAT header line (containing units or labels) with a VAT value line.
     *
     * This function is used when VAT values (e.g., "5,09") are located in one OCR line,
     * and their corresponding VAT labels or suffixes (e.g., "(20%)") are in another line,
     * usually aligned by column. It merges both lines column by column, appending
     * each header part as a suffix to the corresponding value part.
     *
     * If the header has fewer parts than the values, empty suffixes are used for unmatched parts.
     *
     * @param lineVATUnit The line containing VAT-related labels (e.g., "%", "(20%)", "7%", etc.).
     * @param lineVATValue The line containing VAT numeric values (e.g., "5,09", "3,15").
     * @return A new `LayoutLine` where each value is combined with its corresponding VAT suffix.
     *
     * Example:
     * ```
     * lineVATUnit.text  = "%  |   |  (20%)"
     * lineVATValue.text = "     | 5,09 |"
     * =>
     * merged.text = "     | 5,09(20%) |"
     * ```
     */
     fun mergeVATLine(lineVATUnit: LayoutLine, lineVATValue: LayoutLine): LayoutLine {
        val headerParts = lineVATUnit.text.split(Constants.SEPARATE_ITEM_PART).map { it.trim() }
        val valueParts = lineVATValue.text.split(Constants.SEPARATE_ITEM_PART).map { it.trim() }
        val merged = valueParts.mapIndexed { index, value ->
            val suffix = headerParts.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: ""
            "$value$suffix"
        }
        return LayoutLine(
            text = merged.joinToString(Constants.SEPARATE_ITEM_PART),
            midY = lineVATUnit.midY,
            minX = lineVATUnit.minX,
            maxX = lineVATUnit.maxX
        )
    }

    /**
     * Checks whether a given text contains a VAT (value-added tax) percentage.
     *
     * This function looks for patterns representing a percentage value, typically used
     * for VAT rates in invoices (e.g., "19%", "7.00 %"). It handles both dot and comma
     * decimal separators, and also matches the standalone "%" symbol.
     *
     * @param text The input text line to be checked.
     * @return True if the text contains a percentage value; false otherwise.
     *
     * Examples:
     * - "19%" => true
     * - "7.00 %" => true
     * - "%" => true
     * - "MwSt 20,0%" => true
     * - "Total amount: 100 EUR" => false
     */
    private fun containsVatPercentage(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed == "%") return true
        val regex = Regex("""\b\d{1,3}([.,]\d+)?\s*%""")
        return regex.containsMatchIn(trimmed)
    }

    /**
     * Extracts the VAT (value-added tax) amount or percentage from a line of text.
     *
     * This function splits the given OCR line using a defined separator (e.g., pipe `|`),
     * then searches for the part that contains a VAT percentage (e.g., "5,09 (20%)").
     * If such a part is found, it returns the cleaned VAT value using `cleanVatText()`.
     *
     * Internally, it uses `containsVatPercentage()` to identify relevant parts of the line.
     *
     * @param line The OCR line possibly containing VAT information.
     * @return A cleaned VAT string if found, or null otherwise.
     *
     * Examples:
     * - "Zwischensumme | 25,48 | 5,09 (20%) | 30,57" => "20%"
     * - "MwSt: 19%" => "19%"
     * - "Netto | 100,00 | 7,00 % MwSt | 107,00" => "7%"
     * - "Totalbetrag | 142,30" => null
     */
    fun getVatFromLine(line: String?): String? {
        if (line?.contains("5,09") == true) {
            Log.e("Suong", line)
        }
        val parts = line?.split(Regex(Constants.SEPARATE_ITEM_PART))?.map { it.trim() }
        val vat = parts?.find { containsVatPercentage(it) }
        if (vat != null) {
            return cleanVatText(vat)
        }
        return null
    }

    /**
     * Cleans and extracts a VAT percentage string from a noisy or formatted input.
     *
     * This function is used to normalize various formats of VAT values extracted from OCR text,
     * returning a clean percentage string such as "19%" or "20%".
     *
     * It handles several VAT formats:
     * - Values inside parentheses: e.g., "5,09 (20%)" → "20%"
     * - Key-value style strings: e.g., "A:19,00%", "1=19,00%" → "19,00%"
     * - Strips non-numeric/non-VAT characters before or after the percentage.
     *
     * @param input The raw input string suspected to contain a VAT percentage.
     * @return A cleaned VAT string like "19%", "7%", or "%" (if that's the only symbol found).
     *
     * Examples:
     * - "A:19,00%" => "19,00%"
     * - "5,09 (20%)" => "20%"
     * - "1=19,00%" => "19,00%"
     * - "%" => "%"
     */
    private fun cleanVatText(input: String): String {
        val trimmed = input.trim()
        if (trimmed == "%") {
            return "%"
        }
        val percentInParentheses = Regex("""\((\d{1,3}(,\d{1,2})?)%\)""")
        val match = percentInParentheses.find(input)
        if (match != null) {
            return match.groupValues[1] + "%"
        }
        var target = input
        if (input.contains('=') && input.contains('%')) {
            val percentIndex = input.indexOf('%')
            val equalIndex = input.indexOf('=')
            target = if (percentIndex > equalIndex) {
                input.substring(equalIndex + 1)
            } else {
                input.substring(0, equalIndex)
            }
        }
        val startFromDigit = target.dropWhile { !it.isDigit() }
        val cleaned = startFromDigit.replace(Regex("""[^0-9,%.]"""), "")
        val percentIndex = cleaned.indexOf('%')
        return if (percentIndex != -1) {
            cleaned.substring(0, percentIndex + 1)
        } else {
            cleaned
        }
    }

}