package com.example.orcdemo2.ml.ocr.extractor

import android.util.Log
import com.example.orcdemo2.ml.ocr.Constants.SEPARATE_ITEM_PART
import java.text.SimpleDateFormat
import java.util.Locale

object CurrencyExtractor {


    fun extractCurrencyCode(text: String?): Pair<Boolean, String?> {
        if (text == null) return Pair(false, null)
        val currencyMap = mapOf(
            "€" to "EUR",
            "EUR" to "EUR",
            "$" to "USD",
            "USD" to "USD",
            "£" to "GBP",
            "GBP" to "GBP",
            "¥" to "JPY",
            "JPY" to "JPY",
            "₫" to "VND",
            "VND" to "VND"
        )

        val tokens = text.split("\\s+".toRegex())
        for (token in tokens) {
            val cleaned = token.trim().replace(",", "").replace(".", "")
            for ((symbol, code) in currencyMap) {
                if (cleaned.equals(symbol, ignoreCase = true)) {
                    return Pair(true, code)
                }
            }
        }

        return Pair(false, null)
    }
}