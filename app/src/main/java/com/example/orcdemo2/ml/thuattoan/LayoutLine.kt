package com.example.orcdemo2.ml.thuattoan

import android.graphics.RectF
import com.google.mlkit.vision.text.Text
import java.util.Locale

object LayoutLine {

    data class LayoutLine(
        val text: String,
        val midY: Float,
        val minX: Float,
        val maxX: Float
    )

    enum class LayoutSectionType {
        HEADER, ITEM_LIST, VAT, TOTAL, FOOTER, UNKNOWN
    }

    data class LayoutSection(
        val type: LayoutSectionType,
        val lines: List<LayoutLine>
    )

    class LayoutExtractor {
        fun extractSections(from: List<Text.Line>): List<LayoutSection> {
            val lines = from.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                val rectF = RectF(box)
                LayoutLine(
                    text = line.text,
                    midY = rectF.top + rectF.height() / 2,
                    minX = rectF.left,
                    maxX = rectF.right
                )
            }

            val sortedLines = lines.sortedByDescending { it.midY }

            val sections = mutableListOf<List<LayoutLine>>()
            var current = mutableListOf<LayoutLine>()
            var lastY: Float? = null

            val verticalThreshold = 25f   // Adjust according to image resolution
            val horizontalGapThreshold = 50f

            for (line in sortedLines) {
                if (lastY != null && kotlin.math.abs(lastY - line.midY) > verticalThreshold) {
                    if (current.isNotEmpty()) sections.add(current)
                    current = mutableListOf(line)
                } else if (current.lastOrNull()?.let { kotlin.math.abs(it.minX - line.minX) > horizontalGapThreshold } == true) {
                    if (current.isNotEmpty()) sections.add(current)
                    current = mutableListOf(line)
                } else {
                    current.add(line)
                }
                lastY = line.midY
            }

            if (current.isNotEmpty()) {
                sections.add(current)
            }

            return sections.map { LayoutSection(LayoutSectionType.UNKNOWN, it) }
        }
    }

    class SectionClassifier {

        fun classify(sections: List<LayoutSection>): List<LayoutSection> {
            return sections.map { section ->
                val type = classifySection(section.lines)
                section.copy(type = type)
            }
        }

        private fun classifySection(lines: List<LayoutLine>): LayoutSectionType {
            val textBlock = lines.joinToString(" ") { it.text.lowercase(Locale.getDefault()) }

            val headerKeywords = listOf("mã số thuế", "mst", "steuer", "station", "gmbh", "firma", "obj.-nr", "hansastr")
            val totalKeywords = listOf("gesamtbetrag", "tổng", "total", "summe", "amount")
            val vatKeywords = listOf("mwst", "umsatzsteuer", "vat", "brutto", "netto", "a:19,00%")
            val footerKeywords = listOf("kartenzahlung", "terminalnummer", "emv", "zahlung erfolgt", "danke", "thank", "mastercard")

            fun containsAny(keywords: List<String>, block: String): Boolean {
                return keywords.any { block.contains(it) }
            }

            return when {
                containsAny(headerKeywords, textBlock) -> LayoutSectionType.HEADER
                containsAny(totalKeywords, textBlock) && lines.size <= 3 -> LayoutSectionType.TOTAL
                containsAny(vatKeywords, textBlock) -> LayoutSectionType.VAT
                containsAny(footerKeywords, textBlock) -> LayoutSectionType.FOOTER
                isItemListBlock(lines) -> LayoutSectionType.ITEM_LIST
                else -> LayoutSectionType.UNKNOWN
            }
        }

        private fun isItemListBlock(lines: List<LayoutLine>): Boolean {
            val currencyRegex = Regex("[0-9]+([.,][0-9]{2})?\\s*(eur|€)", RegexOption.IGNORE_CASE)
            val productWordRegex = Regex("(diesel|fuel|artikel|service|produkt|item|zp|typ|liter)", RegexOption.IGNORE_CASE)
            val wordRegex = Regex("[a-z]{3,}", RegexOption.IGNORE_CASE)

            for (line in lines) {
                val lower = line.text.lowercase(Locale.getDefault())
                if (lower.contains("gesamt") || lower.contains("total") || lower.contains("summe")) continue

                val hasPrice = currencyRegex.containsMatchIn(lower)
                val hasProductWord = productWordRegex.containsMatchIn(lower)
                val hasWords = wordRegex.containsMatchIn(lower)

                if (hasPrice && (hasProductWord || hasWords)) {
                    return true
                }
            }
            return false
        }
    }
}