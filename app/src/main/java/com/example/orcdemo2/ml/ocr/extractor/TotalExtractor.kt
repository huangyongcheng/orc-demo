package com.example.orcdemo2.ml.ocr.extractor

import android.util.Log
import com.example.orcdemo2.ml.ocr.Constants.SEPARATE_ITEM_PART

object TotalExtractor {

    fun isTotalLine(line: String): Boolean {
        if (line.contains("Radwechsel Komfort Plus")) {
            Log.e("Suong", line)
        }
        val totalKeywords = listOf(
            "gesamtsumme", "summe", "sunne eur", "gesamtbetrag", "zu zahlen",
            "endbetrag", "rechnungsbetrag", "bruttobetrag", "zahlbetrag",
            "total", "übertrag", "t0tal"
        )
        val normalizedLine = line.lowercase()
        return totalKeywords.any { keyword ->
            normalizedLine.contains(keyword)
        }
    }

    private fun getTotalFromLine(line: String?): String? {
        val parts = line?.split(Regex(SEPARATE_ITEM_PART))?.map { it.trim() }
        val vat = parts?.find { isTotalLine(it) }
        if (vat != null) {
            return cleanTotalText(vat)
        }
        return null
    }

    fun cleanTotalText(input: String?): String? {
        if (input == null) return null
        val lastPart = input.split(SEPARATE_ITEM_PART).last().trim()
        var cleaned = lastPart.replace("\\s+".toRegex(), "")
        cleaned = cleaned.replace("[a-zA-Z]\\d+|\\d+[a-zA-Z]".toRegex(), "")
        cleaned = cleaned.replace("[a-zA-Z]".toRegex(), "")
        cleaned = cleaned.replace("[^0-9,\\.]".toRegex(), "")
        return cleaned
    }
}