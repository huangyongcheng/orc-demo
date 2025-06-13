package com.example.orcdemo2.ml.ocr.extractor

import android.util.Log
import com.example.orcdemo2.ml.ocr.Constants.SEPARATE_ITEM_PART

object TotalExtractor {

    /**
     * Determines whether a given line from an invoice likely represents the total amount.
     *
     * This function checks if the input string contains any keywords commonly used to denote
     * the total amount in invoices, including German terms (e.g., "gesamtbetrag", "rechnungsbetrag")
     * and English terms (e.g., "total", "amount"). It normalizes the input by converting it
     * to lowercase before performing the keyword match.
     *
     * @param line The OCR-extracted line of text from an invoice.
     * @return True if the line contains any total-related keywords; false otherwise.
     *
     * Examples:
     * - "GesamtsUmMe:   |  14,40" => true
     */
    fun isTotalLine(line: String): Boolean {
        if (line.contains("Bruttobetrag:")) {
            Log.e("Suong", line)
        }
        val totalKeywords = listOf(
            "gesamtsumme", "summe", "sunne eur", "gesamtbetrag", "zu zahlen",
            "endbetrag", "rechnungsbetrag", "bruttobetrag", "zahlbetrag",
            "total", "übertrag", "t0tal","betrag","bruttobetrag:"
        )
        val normalizedLine = line.lowercase()
        return totalKeywords.any { keyword ->
            normalizedLine.contains(keyword)
        }
    }


    /**
     * Extracts and cleans the total amount from a given OCR line.
     *
     * This function is designed to process a line of text (typically identified as containing
     * the total invoice amount) and extract the most likely numeric value representing the total.
     * It performs the following:
     * - Splits the line by the predefined separator (e.g., "|") and takes the last part.
     * - Removes spaces between digits (e.g., "1 000" → "1000").
     * - Removes letters adjacent to numbers (e.g., "EUR24,00" → "24,00").
     * - Removes all non-numeric, non-decimal characters except for ',' and '.'.
     *
     * @param input The original OCR line string suspected to contain the total amount.
     * @return A cleaned string containing the numeric value of the total, or null if input is null.
     *
     * Examples:
     * - "GesamtsUmMe:   |  14,40" => "14,40"
     * - "Gesamtbetrag   |  25,48   |  5,09 (20%)   |  30,57" => "30,57"
     */
    fun cleanTotalText(input: String?): String? {
        if(input?.contains("24,00 EUR") == true) {
            Log.e("Suong", input.toString())
        }
        if (input == null) return null
        val lastPart = input.split(SEPARATE_ITEM_PART).last().trim()
        // remove space between numbers
        var cleaned = lastPart.replace(Regex("(?<=\\d)[ \\t]+(?=\\d)"), "")
        cleaned = cleaned.replace("[a-zA-Z]\\d+|\\d+[a-zA-Z]".toRegex(), "")
        cleaned = cleaned.replace("[a-zA-Z]".toRegex(), "")
        cleaned = cleaned.replace("[^0-9,\\.]".toRegex(), "")
        return cleaned
    }
}