package com.example.orcdemo2.ml.ocr

import android.text.TextUtils
import android.util.Log
import com.example.orcdemo2.ml.ocr.Constants.SEPARATE_ITEM_PART
import com.example.orcdemo2.ml.ocr.processor.InvoiceItemProcessor.extractNumberOnly
import com.example.orcdemo2.ml.ocr.processor.InvoiceItemProcessor.isProductNameValid
import com.example.orcdemo2.ml.ocr.processor.InvoiceItemProcessor.isValidNumber
import com.example.orcdemo2.ml.ocr.extractor.ProductNameExtractor
import com.example.orcdemo2.ml.ocr.extractor.TotalExtractor.cleanTotalText
import com.example.orcdemo2.ml.ocr.extractor.VATExtractor.getVatFromLine
import com.example.orcdemo2.ml.ocr.extractor.VATExtractor.mergeVATLine
import com.example.orcdemo2.ml.ocr.extractor.QuantityExtractor
import com.example.orcdemo2.ml.ocr.extractor.TotalExtractor
import com.example.orcdemo2.ml.ocr.model.InvoiceData
import com.example.orcdemo2.ml.ocr.model.InvoiceItem
import com.example.orcdemo2.ml.ocr.model.LayoutLine
import com.example.orcdemo2.ml.ocr.processor.InvoiceItemProcessor

object InvoiceConverter {

    /**
     * Extracts structured item data (name, quantity, total price) from a line of OCR invoice text.
     *
     * This method is designed to parse a single line from an invoice which is expected to contain
     * a product/service name, optional quantity, and a price — separated by a standardized delimiter (e.g. `" | "`).
     *
     * ### Parsing strategy:
     * - The line is normalized by replacing `"-"` with the item separator (`SEPARATE_ITEM_PART`).
     * - Attempts to locate a valid number (price) starting from the right-most part.
     * - Attempts to detect quantity using keywords like `"Stück"`, `"Flasche"` etc., using `QuantityExtractor`.
     * - Builds the item name from parts before the price, skipping numeric-only or invalid strings.
     * - If quantity is not found via keyword, it checks the token after the last name-like segment.
     * - Cleans and normalizes the product name via `ProductNameExtractor`.
     *
     * @param line A single line from the invoice, expected to be formatted like: `"1 Stück | Apfelsaft 1L | 1,50"`
     * @return An [InvoiceItem] object containing:
     * - `name`: cleaned product name (e.g. `"Apfelsaft"`),
     * - `quantity`: number of units (e.g. `"1"`),
     * - `totalPrice`: numeric string representing total price (e.g. `"1.50"`).
     *
     */
    private fun getInvoiceItem(line: String): InvoiceItem {

        val normalizedLine = line.replace("-", SEPARATE_ITEM_PART)
        val parts = normalizedLine.split(Regex("""\s+\|\s+""")).map { it.trim() }
        if (parts.size < 2) return InvoiceItem()

        var price: String? = null
        var indexPrice: Int = parts.size - 1
        for (i in 1..parts.size) {
            val candidate = extractNumberOnly(parts[parts.size - i])
            if (isValidNumber(candidate)) {
                price = candidate
                indexPrice = i
                break
            }
        }

        var name: String? = ""
        var indexLastName: Int = 0
        var quantity: String? = null
        parts.forEachIndexed { index, item ->
            val extractQuantity = QuantityExtractor.extractQuantityByKeyword(item)
            if (extractQuantity != null) {
                quantity = extractQuantity.first
                if (extractQuantity.second != null) {
                    name = extractQuantity.second
                }
            } else {
                if (index <= indexPrice) {
                    if (!isValidNumber(item) && isProductNameValid(item)) {
                        name = "$name $item"
                        indexLastName = index
                    }
                }
            }
        }

        if (TextUtils.isEmpty(quantity)) {
            if (indexLastName < (parts.size - 1)) {
                if (InvoiceItemProcessor.isPureInteger(parts[indexLastName + 1])) {
                    quantity = parts[indexLastName + 1]
                }
            }
        }


        val cleanedName = ProductNameExtractor.extractTextBeforeFirstNumber(ProductNameExtractor.cleanTextKeepName(name?.trim()))
        return InvoiceItem(
            name = cleanedName,
            quantity = quantity?.trim(),
            totalPrice = extractNumberOnly(price?.trim())
        )
    }

    /**
     * Converts a list of OCR-extracted `LayoutLine` objects (representing invoice item lines)
     * into a list of structured [InvoiceItem]s by extracting the name, quantity, and total price.
     *
     * @param itemInvoices A list of [LayoutLine] objects, each representing one line of an invoice,
     * typically containing information like product name, quantity, and price.
     *
     * @return A list of [InvoiceItem] objects with valid names and extracted data.
     *
     * @see getInvoiceItem for parsing logic
     */
    private fun convert2InvoiceItem(itemInvoices: List<LayoutLine>): List<InvoiceItem> {
        val listItems = mutableListOf<InvoiceItem>()
        itemInvoices.forEach {
            val itemInvoice = getInvoiceItem(it.text)
            if (!TextUtils.isEmpty(itemInvoice.name)) {
                listItems.add(itemInvoice)
            }
        }
        return listItems
    }


    /**
     * Converts OCR-extracted invoice lines into structured [InvoiceData], including:
     * - a list of [InvoiceItem]s,
     * - detected VAT percentage (if available),
     * - and total invoice amount.
     *
     * @param itemInvoices A list of [LayoutLine]s believed to be invoice item rows.
     * @param ocrTexts The full list of OCR-detected lines from the invoice document.
     *
     * @return An [InvoiceData] object containing:
     * - [items]: Parsed product lines,
     * - [vat]: VAT percentage as a string (e.g. `"19%"`),
     * - [total]: Total invoice amount as a string (e.g. `"15,99"`).
     *
     *
     * @see getVatFromLine
     * @see mergeVATLine
     * @see convert2InvoiceItem
     * @see cleanTotalText
     */
    fun convert2InvoiceData(
        itemInvoices: List<LayoutLine>,
        ocrTexts: List<LayoutLine>
    ): InvoiceData {
        val index = ocrTexts.indexOfFirst { getVatFromLine(it.text) != null }
        var lineVat = ocrTexts.getOrNull(index)
        val cleanVat = getVatFromLine(lineVat?.text)
        if (lineVat != null && cleanVat.equals("%")) {
            lineVat = mergeVATLine(lineVat, ocrTexts[index + 1])
        }
        val sortOrcTexts = ocrTexts.sortedByDescending { it.midY }
        val total = sortOrcTexts.find { TotalExtractor.isTotalLine(it.text) }

        val invoiceItems = convert2InvoiceItem(itemInvoices)
        val cleanVAT = getVatFromLine(lineVat?.text)
        val cleanTotal = cleanTotalText(total?.text)
        return InvoiceData(
            items = invoiceItems,
            vat = cleanVAT,
            total = cleanTotal
        )
    }
}