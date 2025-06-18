package com.example.orcdemo2.ml.ocr.extractor

import android.util.Log
import com.example.orcdemo2.ml.ocr.Constants.SEPARATE_ITEM_PART
import java.text.SimpleDateFormat
import java.util.Locale

object DateExtractor {

    val datePatterns = listOf(
        "dd.MM.yyyy", "dd/MM/yyyy", "MM.dd.yyyy", "MM/dd/yyyy",
        "yyyy.MM.dd", "yyyy/MM/dd",
        "dd.MM.yyyy HH:mm", "dd/MM/yyyy HH:mm",
        "MM.dd.yyyy HH:mm", "MM/dd/yyyy HH:mm",
        "yyyy.MM.dd HH:mm", "yyyy/MM/dd HH:mm"
    )



    /**
     * Detects whether the input text contains a valid date in common formats.
     *
     * This function first normalizes the text to correct common OCR misread characters
     * (e.g., "O" → "0", "l1" → "11", "S" → "5", etc.). Then, it tries to match the cleaned
     * text against multiple common date formats using regular expressions.
     * If a match is found and successfully parsed into a valid date, it returns `true`.
     *
     * Supported date formats (with optional time):
     * - dd.MM.yyyy
     * - dd/MM/yyyy
     * - MM.dd.yyyy
     * - MM/dd/yyyy
     * - yyyy.MM.dd
     * - yyyy/MM/dd
     * - Each of the above with optional `HH:mm` time part
     *
     * @param text The raw input string (possibly OCR output).
     * @return `true` if a valid date is detected after normalization; `false` otherwise.
     *
     * Examples:
     * - "Rechnungsdatum: 12.06.2024" => true
     * - "Datum 0b.06.2023" => true (normalized to 06.06.2023)
     * - "Lieferung: 2023/11/05 13:45" => true
     * - "Artikelnummer 456Z21" => false
     */
    fun containsDate(text: String): Boolean {
        if(text?.contains("2025") ==true){
            Log.e("Suong","df")
        }
        val normalizedText = text
            .replace("0b", "06", ignoreCase = true)
            .replace("o6", "06", ignoreCase = true)
            .replace("O6", "06", ignoreCase = true)
            .replace("l1", "11", ignoreCase = true)
            .replace("I1", "11", ignoreCase = true)
            .replace("1l", "11", ignoreCase = true)
            .replace("1I", "11", ignoreCase = true)
            .replace("0O", "00", ignoreCase = true)
            .replace("O0", "00", ignoreCase = true)
            .replace("S", "5", ignoreCase = true)
            .replace("B", "8", ignoreCase = true)
            .replace("Q", "0", ignoreCase = true)
            .replace("Z", "2", ignoreCase = true)

        val datePatterns = listOf(
            "dd.MM.yyyy", "dd/MM/yyyy", "MM.dd.yyyy", "MM/dd/yyyy",
            "yyyy.MM.dd", "yyyy/MM/dd", "dd.MM.yyyy HH:mm", "dd/MM/yyyy HH:mm",
            "MM.dd.yyyy HH:mm", "MM/dd/yyyy HH:mm", "yyyy.MM.dd HH:mm", "yyyy/MM/dd HH:mm"
        )

        for (pattern in datePatterns) {
            val regex = Regex(
                pattern
                    .replace(".", "\\.")
                    .replace("/", "\\/")
                    .replace("dd", "\\d{2}")
                    .replace("MM", "\\d{2}")
                    .replace("yyyy", "\\d{4}")
                    .replace("HH", "\\d{2}")
                    .replace("mm", "\\d{2}")
            )
            val match = regex.find(normalizedText)
            if (match != null) {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                try {
                    sdf.parse(match.value)
                    return true
                } catch (_: Exception) {
                }
            }
        }
        return false
    }

    fun extractDate(text: String?): String? {
        if(text?.contains("2025") ==true){
            Log.e("Suong","df")
        }
        if (text == null) return null
        val normalizedText = text
            .replace("0b", "06", ignoreCase = true)
            .replace("o6", "06", ignoreCase = true)
            .replace("O6", "06", ignoreCase = true)
            .replace("l1", "11", ignoreCase = true)
            .replace("I1", "11", ignoreCase = true)
            .replace("1l", "11", ignoreCase = true)
            .replace("1I", "11", ignoreCase = true)
            .replace("0O", "00", ignoreCase = true)
            .replace("O0", "00", ignoreCase = true)
            .replace("S", "5", ignoreCase = true)
            .replace("B", "8", ignoreCase = true)
            .replace("Q", "0", ignoreCase = true)
            .replace("Z", "2", ignoreCase = true)


        for (pattern in datePatterns) {
            val regex = Regex(
                pattern
                    .replace(".", "\\.")
                    .replace("/", "\\/")
                    .replace("dd", "\\d{2}")
                    .replace("MM", "\\d{2}")
                    .replace("yyyy", "\\d{4}")
                    .replace("HH", "\\d{2}")
                    .replace("mm", "\\d{2}")
            )
            val match = regex.find(normalizedText)
            if (match != null) {
                val matchedDate = match.value.take(10) // chỉ lấy phần ngày
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                sdf.isLenient = false
                try {
                    sdf.parse(matchedDate)
                    return matchedDate
                } catch (_: Exception) {
                    val fallbackFormats = listOf("dd/MM/yyyy", "MM.dd.yyyy", "MM/dd/yyyy", "yyyy.MM.dd", "yyyy/MM/dd")
                    for (fmt in fallbackFormats) {
                        try {
                            val trySdf = SimpleDateFormat(fmt, Locale.getDefault())
                            trySdf.isLenient = false
                            trySdf.parse(matchedDate)
                            return matchedDate
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        return null
    }


    fun normalizeDateToStandardFormat(
        rawText: String,
        separator: Char = '.'
    ): String? {

        val normalizedText = rawText
            .replace("0b", "06", ignoreCase = true)
            .replace("o6", "06", ignoreCase = true)
            .replace("O6", "06", ignoreCase = true)
            .replace("l1", "11", ignoreCase = true)
            .replace("I1", "11", ignoreCase = true)
            .replace("1l", "11", ignoreCase = true)
            .replace("1I", "11", ignoreCase = true)
            .replace("0O", "00", ignoreCase = true)
            .replace("O0", "00", ignoreCase = true)
            .replace("S", "5", ignoreCase = true)
            .replace("B", "8", ignoreCase = true)
            .replace("Q", "0", ignoreCase = true)
            .replace("Z", "2", ignoreCase = true)

        for (pattern in datePatterns) {
            val regex = Regex(
                pattern
                    .replace(".", "\\.")
                    .replace("/", "\\/")
                    .replace("dd", "(\\d{2})")
                    .replace("MM", "(\\d{2})")
                    .replace("yyyy", "(\\d{4})")
                    .replace("HH", "(\\d{2})?")
                    .replace("mm", "(\\d{2})?")
            )
            val match = regex.find(normalizedText)
            if (match != null) {
                return try {
                    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                    sdf.isLenient = false
                    val parsed = sdf.parse(match.value)

                    val outFormat = SimpleDateFormat("dd${separator}MM${separator}yyyy", Locale.getDefault())
                    outFormat.format(parsed!!)
                } catch (_: Exception) {
                    null
                }
            }
        }

        return null
    }


}