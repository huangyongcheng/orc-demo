package com.example.orcdemo2.ml.refactor

import android.util.Log
import com.example.orcdemo2.ml.refactor.model.LayoutLine

object VATExtractor {

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

    private fun containsVatPercentage(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed == "%") return true
        val regex = Regex("""\b\d{1,3}([.,]\d+)?\s*%""")
        return regex.containsMatchIn(trimmed)
    }

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