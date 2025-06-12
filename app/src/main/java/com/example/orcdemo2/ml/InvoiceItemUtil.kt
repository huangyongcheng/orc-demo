package com.example.orcdemo2.ml

import android.text.TextUtils
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

//→ "Do not check based on the ':' character"
//→ "Do not check if the name is allowed to start with a number"
// dấu gạch ngang (-) trong name vẫn giữa lại
object InvoiceItemUtil {

    const val SEPARATE_ITEM_PART = "   |  "

    // test12 => true
    private fun hasMoreLettersThanDigits(input: String): Boolean {
        val letters = input.count { it.isLetter() }
        val digits = input.count { it.isDigit() }
        return letters > digits
    }

    private fun containsDate(text: String): Boolean {
        val datePatterns = listOf(
            "dd.MM.yyyy",
            "dd/MM/yyyy",
            "MM.dd.yyyy",
            "MM/dd/yyyy",
            "yyyy.MM.dd",
            "yyyy/MM/dd",
            "dd.MM.yyyy HH:mm",
            "dd/MM/yyyy HH:mm",
            "MM.dd.yyyy HH:mm",
            "MM/dd/yyyy HH:mm",
            "yyyy.MM.dd HH:mm",
            "yyyy/MM/dd HH:mm"
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
            val match = regex.find(text)
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

    private fun containsTime(text: String): Boolean {
        val timeRegex = Regex("""\b([01]?[0-9]|2[0-3]):[0-5][0-9](\s?(Uhr|AM|PM|am|pm))?\b""")
        return timeRegex.containsMatchIn(text)
    }

    private fun containsInValidNumbers(text: String): Boolean {
        // Find all number groups separated by spaces
        val tokens = text.trim().split(Regex("\\s+"))

        val firstToken = tokens.first()
        val tailTokens = if (firstToken.matches(Regex("^\\d+$"))) {
            tokens.drop(1)
        } else {
            tokens
        }

        val numberTokens = tailTokens.filter { it.matches(Regex("\\d+")) }

        // 0,94 14,40 return true (invalid)
        val decimalRegex = Regex("""\d+[,.]\d+""")
        val matches = decimalRegex.findAll(text)
        return numberTokens.size >= 2 || matches.count() >= 2
    }

    // 20%   |  13.85 => true
    private fun isNumericWithoutLetters(text: String): Boolean {
        // If there is any letter, return false
        if (text.contains(Regex("[a-zA-Z]"))) return false

        // Find all decimal number strings (allowing ',' or '.')
        val numberMatch = Regex("""\d+[.,]?\d*""").find(text)

        // If there is at least one valid number string, return true
        return numberMatch != null
    }




    fun isInvoiceItem(text: String): Boolean {

        if (text.contains(SEPARATE_ITEM_PART)) {
            if(text.contains("We Charge"))
            {
                Log.e("Suong",text)
            }

            // Rule 1: exclude keywords related to payment and totals
            val nonItemKeywords = listOf(
                "gesamtsumme", "zahlung", "gegeben", "betrag", "summe",
                "kartenzahlung", "total", "wechselgeld", "bezahlt", "change", "gesamt", "mwst", "datum",
                "visa", "beleg-nr.", "beleg nummer", "belegnummer", "genehmigung",
                "terminalnummer", "zurück", "tax:", "lieferung", "ust.", "umsatzsteuer",
                "credit",
                "=",
                "%" // remove if need check VAT
            )
            if (nonItemKeywords.any { text.lowercase(Locale.ROOT).contains(it) }) return false


            // Rule 2 If the line contains this list of keywords, it is not an invoice item
            // Tax: => false
            val equalKeyWord = listOf("tax:", "tax", "cash", "cash tendered:", "net","netto")
            val parts = text.split(Regex(SEPARATE_ITEM_PART)).map { it.trim() }
            val hasEqualKeyWord = parts.any { part -> equalKeyWord.any { it.equals(part.trim(), true) } }
            if (hasEqualKeyWord) {
                return false
            }

            // Rule 3: contains format of quantity + unit price + total price (with comma or dot)
            val hasCurrency = Regex("\\d{1,3}[.,]\\d{2}").containsMatchIn(text)
            if (hasCurrency) {

                // val parts = text.split(Regex("""\s+\|\s+""")).map { it.trim() }
                if (parts.size > 1) {
                    val hasMoreLettersThanDigits = parts.all { hasMoreLettersThanDigits(it) }
                    if (hasMoreLettersThanDigits) {  // If the string contains more letters than digits, it is not an invoice item
                        return false
                    } else if (containsDate(text)) { // If it contains a date, it is not an invoice item
                        return false
                    } else if (containsTime(text)) { // If it contains a time, it is not an invoice item
                        return false

                    } else {
                        val hasContainsInValidNumbers = parts.any { containsInValidNumbers(it.trim()) }
                        // If there are invalid numbers, it is not an invoice item
                        if (hasContainsInValidNumbers) {
                            return false
                        } else if (parts.all { isNumericWithoutLetters(it.trim()) }) {
                            // If no text is found in the string, it is not an invoice item
                            return false
                        } else {
                            return true
                        }

                    }
                } else {
                    return false
                }
            }

        }

        return false
    }

    private fun isValidNumber(text: String?): Boolean {
        val regex = Regex("""^\d+(?:[.,]\d+)?$""")
        if (text != null) {
            return regex.matches(text.trim())
        }
        return false
    }

    //EUR 65.54 = false
    private fun isProductNameValid(name: String?): Boolean {
        if (name.isNullOrBlank() || name.length < 4) return false

        val currencyKeywords = listOf("EUR")

        return currencyKeywords.none { keyword ->
            Regex("""\b${Regex.escape(keyword)}\b""").containsMatchIn(name)
        }

    }

    private fun extractNumberOnly(text: String?): String? {
        val regex = Regex("""\d+[.,]?\d*""")
        return text?.let { regex.find(it)?.value }
    }


    // *000005 Diesel Fuel Save 53,15 EUR #A* => Diesel Fuel Save 53,15 EUR A
    private fun cleanTextKeepName(text: String?): String? {

        // Keep letters, digits, whitespace, comma, dot
        val noSpecialChars = text?.replace(Regex("""[^\w\sÀ-ỹ,.\-]"""), "")
        // Split into words
        val words = noSpecialChars?.trim()?.split(Regex("\\s+"))

        // Drop leading words if they are only numbers
        val resultWords = words?.dropWhile { it.matches(Regex("""\d+""")) }

        // Join back to a string
        return resultWords?.joinToString(" ")
    }

    // Pflanzen erm. 6,99 x 3 Posten 3 => Pflanzen erm
    // 3K 2,35 Prof. kehrgarnitur  => kehrgarnitur
    private fun extractTextBeforeFirstNumber(text: String?): String {

        if (text.isNullOrBlank()) return ""

        val words = text.trim().split(Regex("\\s+"))
        val result = mutableListOf<String>()

        for (word in words) {
            if (word.contains(Regex("""\d"""))) {
                break
            }
            result.add(word)
        }

        if (result.isNotEmpty()) {
            return cleanDotsAndCommas(result.joinToString(" "))
        }

        val indexDot = text.lastIndexOf('.')
        val indexComma = text.lastIndexOf(',')

        val splitIndex = maxOf(indexDot, indexComma)
        if (splitIndex != -1 && splitIndex < text.length - 1) {
            val after = text.substring(splitIndex + 1).trim()
            val cleaned = after.split(Regex("\\s+"))
                .dropWhile { it.matches(Regex("""[\d.,\W_]+""")) }
                .joinToString(" ")

            return cleanDotsAndCommas(cleaned)
        }

        return ""
    }

    // reiniger PowerGel, => reiniger PowerGel
    private fun cleanDotsAndCommas(text: String?): String {
        if (text.isNullOrBlank()) return ""

        return text
            .replace(Regex("^[.,\\s]+"), "")
            .replace(Regex("[.,\\s]+$"), "")
    }

    // Check quantity
    // "2 Stück" =>true, "Stück 2" =>true
    private fun extractQuantityIfPresent(text: String): Pair<Boolean, String?> {

        val unitKeywords = listOf("stück", "kg", "flasche", "packung", "einheit", "dose", "päckchen", "posten:","km")
        val unitPattern = unitKeywords.joinToString("|")

        val quantityRegex = Regex(
            """\b(?:($unitPattern)\s*(\d{1,3}(?:[.,]\d{1,2})?)|(\d{1,3}(?:[.,]\d{1,2})?)\s*($unitPattern))\b""",
            RegexOption.IGNORE_CASE
        )

        val match = quantityRegex.find(text)

        return if (match != null) {
            val quantity = match.groupValues[2].ifEmpty { match.groupValues[3] }
            true to quantity
        } else {
            false to null
        }
    }

    fun isPureInteger(input: String): Boolean {
        val regex = Regex("^\\d+$")
        return regex.matches(input)
    }

    fun convertToInvoiceItem(line: String): InvoiceItem {
        val parts = line.split(Regex("""\s+\|\s+""")).map { it.trim() }
        if (parts.size < 2) return InvoiceItem()

        if (line.contains("CVM   |  EUR 65.54")) {
            Log.e("Suong", line)
        }

        var price: String? = null
        var indexPrice: Int = parts.size - 1
        // Check price is invalid number if not get the next last one
        for (i in 1..parts.size) {
            val candidate = extractNumberOnly(parts[parts.size - i])
            if (isValidNumber(candidate)) {
                price = candidate
                indexPrice = i
                break
            }
        }

        var name: String? = ""
        var indexLastName:Int = 0
        var quantity: String? = null
        parts.forEachIndexed { index, item ->
            val extractQuantity = extractQuantityIfPresent(item) // check part have quantity
            if (extractQuantity.first) {
                quantity = extractQuantity.second
            } else {
                if (index <= indexPrice) {
                    if (!isValidNumber(item) && isProductNameValid(item)) { // only add with letter, ignore number
                        name = "$name $item"
                        indexLastName = index
                    }
                }
            }
        }

        // ooperation SHFV Namingright   |  1   |  1.875,00   |  19   |  1.875,00
        // get quantity : 1
        if (TextUtils.isEmpty(quantity)) {
            if (indexLastName < (parts.size - 1)) {
                if (isPureInteger(parts[indexLastName + 1])) {
                    quantity = parts[indexLastName + 1]
                }
            }
        }

        if (name?.contains("Namingright") == true) {
            Log.e("Suong", name.toString())
        }

        val cleanedName = extractTextBeforeFirstNumber(cleanTextKeepName(name?.trim()))
        return InvoiceItem(
            name = cleanedName,
            quantity = quantity?.trim(),
            totalPrice = extractNumberOnly(price?.trim())
        )
    }

    data class InvoiceItem(
        val name: String? = null,
        val quantity: String? = null,
        val totalPrice: String? = null,
    )

    data class InvoiceData(
        val vat: String? = null,
        val total: String? = null,
        val items: List<InvoiceItem>? = null
    )


    private fun convert2InvoiceItem(itemInvoices: List<MLActivity.LayoutLine>): List<InvoiceItem> {
        val listItems = mutableListOf<InvoiceItem>()
        itemInvoices.forEach {
            val itemInvoice = convertToInvoiceItem(it.text)
            if(!TextUtils.isEmpty(itemInvoice.name)) {
                listItems.add(itemInvoice)
            }
        }
        return listItems
    }

    private fun mergeVATLine(lineVATUnit: MLActivity.LayoutLine, lineVATValue: MLActivity.LayoutLine):MLActivity.LayoutLine{
        val headerParts = lineVATUnit.text.split(SEPARATE_ITEM_PART).map { it.trim() }
        val valueParts = lineVATValue.text.split(SEPARATE_ITEM_PART).map { it.trim() }

        val merged = valueParts.mapIndexed { index, value ->
            val suffix = headerParts.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: ""
            "$value$suffix"
        }
        return MLActivity.LayoutLine(
            text =  merged.joinToString(SEPARATE_ITEM_PART),
            midY = lineVATUnit.midY,
            minX = lineVATUnit.minX,
            maxX = lineVATUnit.maxX
        )

    }

    fun convert2InvoiceData(itemInvoices: List<MLActivity.LayoutLine>,
                            ocrTexts: List<MLActivity.LayoutLine>
    ):InvoiceData{

        // logic handle VAT not the same line
        // check if exist "%" then go to the next line to get VAT
        val index = ocrTexts.indexOfFirst { getVatFromLine(it.text) != null }
        var lineVat = ocrTexts.getOrNull(index)
        val cleanVat = getVatFromLine(lineVat?.text)
        if (lineVat !=null && cleanVat.equals("%")) {
            lineVat = mergeVATLine(lineVat,ocrTexts[index + 1])
        }
        val total = ocrTexts.find { isTotalLine(it.text) }
        return InvoiceData(
            items = convert2InvoiceItem(itemInvoices),
            vat = getVatFromLine(lineVat?.text),
            total = cleanTotalText(total?.text)
        )

    }

    // VAT
    private fun getVatFromLine(line: String?): String? {

        if(line?.contains("%") ==true){
            Log.e("Suong",line)
        }
        val parts = line?.split(Regex(SEPARATE_ITEM_PART))?.map { it.trim() }
        val vat = parts?.find { containsVatPercentage(it) }
        if(vat !=null) {
            return cleanVatText( vat)
        }
        return null
    }

    private fun containsVatPercentage(text: String): Boolean {
        val trimmed = text.trim()

        if (trimmed == "%") return true
        val regex = Regex("""\b\d{1,3}([.,]\d+)?\s*%""")
        return regex.containsMatchIn(trimmed)
    }

    //A:19,00% => 19,00%
    private fun cleanVatText(input: String): String {

        val trimmed = input.trim()
        if (trimmed == "%") {
            return "%"
        }
        val startFromDigit = input.dropWhile { !it.isDigit() }
        val cleaned = startFromDigit.replace(Regex("""[^0-9,%.]"""), "")
        val percentIndex = cleaned.indexOf('%')
        return if (percentIndex != -1) {
            cleaned.substring(0, percentIndex + 1)
        } else {
            cleaned
        }
    }

    /***
     * Process get total
     */

    //"GesamtsUmMe:   |  14,40" => true
    private fun isTotalLine(line: String): Boolean {

        if(line.contains("GesamtsUmMe")){
            Log.e("Suong",line)
        }
        val totalKeywords = listOf(
            "gesamtsumme",
            "summe",
            "gesamtbetrag",
            "zu zahlen",
            "endbetrag",
            "rechnungsbetrag",
            "bruttobetrag",
            "zahlbetrag",
            "total"
        )

        val normalizedLine = line.lowercase()

        return totalKeywords.any { keyword ->
            normalizedLine.contains(keyword)
        }
    }

    private fun getTotalFromLine(line: String?): String? {

        val parts = line?.split(Regex(SEPARATE_ITEM_PART))?.map { it.trim() }
        val vat = parts?.find { isTotalLine(it) }
        if(vat !=null) {
            return cleanTotalText( vat)
        }
        return null
    }

    //"GesamtsUmMe:   |  14,40" => 14,40
    private fun cleanTotalText(input: String?): String? {
        var cleaned = input?.replace("\\s+".toRegex(), "")
        cleaned = cleaned?.replace("[a-zA-Z]\\d+|\\d+[a-zA-Z]".toRegex(), "")
        cleaned = cleaned?.replace("[a-zA-Z]".toRegex(), "")
        cleaned = cleaned?.replace("[^0-9,\\.]".toRegex(), "")
        return cleaned
    }








}