package com.example.orcdemo2.ml

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

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
            val regex = Regex(pattern
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
                } catch (_: Exception) { }
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
        val numberTokens = tokens.filter { it.matches(Regex("\\d+")) }

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


    //→ "Do not check based on the ':' character"
    //→ "Do not check if the name is allowed to start with a number"

    fun isInvoiceItem(text:String):Boolean {
//        if(text.contains("*6,50 EUR   |  A,1")){
//            Log.e("Suong","text: "+ text)
//        }
        if (text.contains(" | ")) {
            // Rule 1: exclude keywords related to payment and totals
            val nonItemKeywords = listOf(
                "gesamtsumme", "zahlung", "gegeben", "betrag", "summe",
                "kartenzahlung", "total", "wechselgeld", "bezahlt", "change","gesamt","mwst","datum",
                "visa","beleg-nr.","beleg nummer","belegnummer","genehmigung",
                "terminalnummer","zurück","tax:","lieferung","ust.","umsatzsteuer",
                "=",
                "%" // remove if need check VAT
            )
            if (nonItemKeywords.any { text.lowercase(Locale.ROOT).contains(it) }) return false

            // Rule 2: contains common keywords found in item lines
            val itemKeywords = listOf("stück", "einheit",  "einzel", "artikel", "qty")
            if (itemKeywords.any { text.contains(it) }) {
                return true
            }

            // Rule 3 If the line contains this list of keywords, it is not an invoice item
            // Tax: => false
            val equalKeyWord = listOf("tax:","tax","cash","cash tendered:")
            val parts = text.split(Regex(SEPARATE_ITEM_PART)).map { it.trim() }
            val hasEqualKeyWord = parts.any {part-> equalKeyWord.any { it.equals(part.trim(), true) } }
            if(hasEqualKeyWord){
                return false
            }

            // Rule 4: contains format of quantity + unit price + total price (with comma or dot)
            val hasCurrency = Regex("\\d{1,3}[.,]\\d{2}").containsMatchIn(text)
            if(hasCurrency){

               // val parts = text.split(Regex("""\s+\|\s+""")).map { it.trim() }
                if(parts.size>1) {
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
                        }
                        else {
                            return true
                        }

                    }
                } else {
                    return false
                }
            }

            // Rule 5: line contains unit indicators like “x”, “kg”, “Stück”, etc.
//            val unitIndicators = listOf("stück", "packung", "flasche")
//            if (unitIndicators.any { text.contains(it) }) {
//                return true
//            }


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

    private fun extractNumberOnly(text: String?): String? {
        val regex = Regex("""\d+[.,]?\d*""")
        return text?.let { regex.find(it)?.value }
    }


    private fun cleanTextKeepName(text: String?): String? {

        // Keep letters, digits, whitespace, comma, dot
        val noSpecialChars = text?.replace(Regex("""[^\w\sÀ-ỹ,\.]"""), "")

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
            return result.joinToString(" ")
        }

        val indexDot = text.lastIndexOf('.')
        val indexComma = text.lastIndexOf(',')

        val splitIndex = maxOf(indexDot, indexComma)
        if (splitIndex != -1 && splitIndex < text.length - 1) {
            val after = text.substring(splitIndex + 1).trim()
            val cleaned = after.split(Regex("\\s+"))
                .dropWhile { it.matches(Regex("""[\d.,\W_]+""")) }
                .joinToString(" ")

            return cleaned
        }

        return ""
    }

    // Check quantity
    private fun extractQuantityIfPresent(text: String): Pair<Boolean, String?> {
        val unitKeywords = listOf("stück", "kg", "flasche", "packung", "einheit", "dose", "päckchen")

        // Regex: tìm số nguyên hoặc thập phân có thể dùng dấu ',' hoặc '.'
        val quantityRegex = Regex("""\b(\d{1,3}(?:[.,]\d{1,2})?)\s*(${unitKeywords.joinToString("|")})\b""", RegexOption.IGNORE_CASE)

        val match = quantityRegex.find(text)
        return if (match != null) {
            val quantity = match.groupValues[1]
            true to quantity
        } else {
            false to null
        }
    }


    fun convertToInvoiceItem(line: String): InvoiceItem {
        val parts = line.split(Regex("""\s+\|\s+""")).map { it.trim() }
        if (parts.size < 2) return InvoiceItem()

        if(line.contains("reiniger")){
            Log.e("Suong",line)
        }

        var price: String? = null
        var indexPrice:Int = parts.size - 1
       // Check price is invalid number if not get the next last one
        for (i in 1..parts.size) {
            val candidate = extractNumberOnly(parts[parts.size - i])
            if (isValidNumber(candidate)) {
                price = candidate
                indexPrice = i
                break
            }
        }

        var name:String?=""
        var quantity:String? = null
        parts.forEachIndexed{index, item->
            val extractQuantity = extractQuantityIfPresent(item) // check part have quantity
            if(extractQuantity.first){
                quantity = extractQuantity.second
            } else {
                if(index <= indexPrice){
                    if(!isValidNumber(item)) { // only add with letter, ignore number
                        name = "$name $item"
                    }
                }
            }
        }

        val cleanedName= extractTextBeforeFirstNumber(cleanTextKeepName(name?.trim()))
        return InvoiceItem(
            name = cleanedName,
            quantity = quantity?.trim(),
            totalPrice = extractNumberOnly(price?.trim()))
    }

    data class InvoiceItem(
        val name: String?=null,
        val quantity:String?=null,
        val totalPrice: String?=null
    )


     fun convert2InvoiceItem(itemInvoices: List<MLActivity.LayoutLine>) : MutableList<InvoiceItem>{
        val listItems = mutableListOf<InvoiceItem>()
        itemInvoices.forEach {
            listItems.add(convertToInvoiceItem(it.text))
        }
        return listItems
    }

}