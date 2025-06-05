package com.example.orcdemo2.ml.demo

import com.google.mlkit.vision.text.Text

data class LayoutLine(
    val text: String,
    val midY: Float,
    val minX: Float,
    val maxX: Float
) {
    companion object {
        fun from(line: Text.Line): LayoutLine? {
            val box = line.boundingBox ?: return null
            val text = line.text
            val midY = box.centerY().toFloat()
            val minX = box.left.toFloat()
            val maxX = box.right.toFloat()
            return LayoutLine(text, midY, minX, maxX)
        }
    }
}

enum class LayoutSectionType {
    HEADER, ITEM_LIST, VAT, TOTAL, FOOTER, UNKNOWN
}

data class LayoutSection(
    val type: LayoutSectionType,
    val lines: List<LayoutLine>
)

data class ItemLine(
    val productName: String,
    val quantity: Double?,
    val unitPrice: Double?,
    val lineTotal: Double?
)

class LayoutExtractor {
    fun extractSections(text: Text): List<LayoutSection> {
        val lines = text.textBlocks
            .flatMap { it.lines }
            .mapNotNull { LayoutLine.from(it) }

        val sortedLines = lines.sortedByDescending { it.midY }

        val sections = mutableListOf<List<LayoutLine>>()
        var currentSection = mutableListOf<LayoutLine>()
        var lastY: Float? = null
        val verticalThreshold = 20f // Điều chỉnh theo độ phân giải ảnh

        for (line in sortedLines) {
            if (lastY != null && kotlin.math.abs(line.midY - lastY) > verticalThreshold) {
                if (currentSection.isNotEmpty()) {
                    sections.add(currentSection)
                }
                currentSection = mutableListOf(line)
            } else {
                currentSection.add(line)
            }
            lastY = line.midY
        }
        if (currentSection.isNotEmpty()) {
            sections.add(currentSection)
        }

        return sections.map { LayoutSection(LayoutSectionType.UNKNOWN, it) }
    }
}

class SectionClassifier {

    fun classify(sections: List<LayoutSection>): List<LayoutSection> {
        return sections.map { section ->
            val type = classifySection(section.lines)
            LayoutSection(type, section.lines)
        }
    }

    private fun classifySection(lines: List<LayoutLine>): LayoutSectionType {
        val textBlock = lines.joinToString(" ") { it.text.lowercase() }

        val headerKeywords = listOf("mã số thuế", "mst", "station", "gmbh", "firma", "invoice", "kundennr", "customer")
        val totalKeywords = listOf("tổng", "total", "sum", "final amount", "amount due")
        val vatKeywords = listOf("vat", "netto", "steuer", "tax amount", "mwst")
        val footerKeywords = listOf("danke", "thank", "paypal", "visa", "transaction id", "emv")

        fun containsAny(keywords: List<String>, block: String): Boolean {
            return keywords.any { block.contains(it) }
        }

        return when {
            isItemListBlock(lines) -> LayoutSectionType.ITEM_LIST
            containsAny(headerKeywords, textBlock) -> LayoutSectionType.HEADER
            containsAny(totalKeywords, textBlock) && lines.size <= 4 -> LayoutSectionType.TOTAL
            containsAny(vatKeywords, textBlock) -> LayoutSectionType.VAT
            containsAny(footerKeywords, textBlock) -> LayoutSectionType.FOOTER
            else -> LayoutSectionType.UNKNOWN
        }
    }

    private fun isItemListBlock(lines: List<LayoutLine>): Boolean {
        if (lines.size < 2) return false

        val joinedText = lines.joinToString(" ") { it.text.lowercase() }

        val productKeywords = Regex("(diesel|fuel|super|benzin|artikel|liter|service|produkt|product|item)")
        val currencyRegex = Regex("[0-9]+([.,][0-9]{2})?\\s*(eur|€)")
        val unitPriceRegex = Regex("[0-9]+([.,][0-9]{2})\\s*(eur|€)?/\\s*[0-9]+")
        val quantityRegex = Regex("[0-9]+([.,][0-9]{1,3})?\\s*(l|kg|stk|pcs|x)?")

        val hasCurrency = currencyRegex.containsMatchIn(joinedText)
        val hasUnitPrice = unitPriceRegex.containsMatchIn(joinedText)
        val hasQuantity = quantityRegex.containsMatchIn(joinedText)
        val hasProduct = productKeywords.containsMatchIn(joinedText)

        val priceLines = lines.count { currencyRegex.containsMatchIn(it.text.lowercase()) }
        val productLines = lines.count { productKeywords.containsMatchIn(it.text.lowercase()) }

        return (hasProduct && hasCurrency) || (productLines >= 2 && priceLines >= 1) || (hasUnitPrice && hasQuantity)
    }

    fun parseItemList(lines: List<LayoutLine>): List<ItemLine> {
        return lines.map { line ->
            val text = line.text.trim()
            val prices = extractPrices(text)
            val quantity = extractQuantity(text)
            val productName = TextPreprocessor.cleanProductName(text)
            val unitPrice = prices.firstOrNull()
            val lineTotal = if (prices.size > 1) prices.lastOrNull() else null
            ItemLine(productName, quantity, unitPrice, lineTotal)
        }
    }

    private fun extractPrices(text: String): List<Double> {
        val pattern = Regex("[0-9]+([.,][0-9]{2})?\\s*(eur|€)?", RegexOption.IGNORE_CASE)
        return pattern.findAll(text).mapNotNull { matchResult ->
            matchResult.value.replace("[^0-9.,]".toRegex(), "")
                .replace(",", ".")
                .toDoubleOrNull()
        }.toList()
    }

    private fun extractQuantity(text: String): Double? {
        val pattern = Regex("[0-9]+([.,][0-9]+)?")
        return pattern.find(text)?.value?.replace(",", ".")?.toDoubleOrNull()
    }

    object TextPreprocessor {
        fun cleanProductName(text: String): String {
            return text
                .replace("[0-9]+([.,][0-9]{2})?\\s*(eur|€)?".toRegex(), "")
                .replace("[0-9]+([.,][0-9]+)?".toRegex(), "")
                .trim()
        }
    }
}
