package com.example.orcdemo2.ml.refactor

import android.text.TextUtils
import android.util.Log
import com.example.orcdemo2.ml.refactor.Constants.SEPARATE_ITEM_PART
import com.example.orcdemo2.ml.refactor.InvoiceItemProcessor.extractNumberOnly
import com.example.orcdemo2.ml.refactor.InvoiceItemProcessor.isProductNameValid
import com.example.orcdemo2.ml.refactor.InvoiceItemProcessor.isValidNumber
import com.example.orcdemo2.ml.refactor.TotalExtractor.cleanTotalText
import com.example.orcdemo2.ml.refactor.VATExtractor.getVatFromLine
import com.example.orcdemo2.ml.refactor.VATExtractor.mergeVATLine
import com.example.orcdemo2.ml.refactor.model.InvoiceData
import com.example.orcdemo2.ml.refactor.model.InvoiceItem
import com.example.orcdemo2.ml.refactor.model.LayoutLine

object InvoiceConverter {

    private fun getInvoiceItem(line: String): InvoiceItem {
        if (line.contains("son parking tie")) {
            Log.e("Suong", line)
        }
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

        if (name?.contains("Namingright") == true) {
            Log.e("Suong", name.toString())
        }

        val cleanedName = ProductNameExtractor.extractTextBeforeFirstNumber(ProductNameExtractor.cleanTextKeepName(name?.trim()))
        return InvoiceItem(
            name = cleanedName,
            quantity = quantity?.trim(),
            totalPrice = extractNumberOnly(price?.trim())
        )
    }

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
        return InvoiceData(
            items = convert2InvoiceItem(itemInvoices),
            vat = getVatFromLine(lineVat?.text),
            total = cleanTotalText(total?.text)
        )
    }
}