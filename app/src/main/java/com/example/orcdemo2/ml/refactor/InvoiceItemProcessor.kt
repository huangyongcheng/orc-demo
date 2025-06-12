package com.example.orcdemo2.ml.refactor

import android.util.Log
import com.example.orcdemo2.ml.refactor.Constants.SEPARATE_ITEM_PART
import java.text.SimpleDateFormat
import java.util.Locale

object InvoiceItemProcessor {


    fun extractNumberOnly(text: String?): String? {
        val regex = Regex("""\d+[.,]?\d*""")
        return text?.let { regex.find(it)?.value }
    }

    fun isValidNumber(text: String?): Boolean {
        val regex = Regex("""^\d+(?:[.,]\d+)?$""")
        if (text != null) {
            return regex.matches(text.trim())
        }
        return false
    }

    fun isProductNameValid(name: String?): Boolean {
        if (name?.contains("tzGH") == true) {
            Log.e("Suong", name.toString())
        }
        if (name.isNullOrBlank() || name.count { it.isLetter() } < 4) return false
        val currencyKeywords = listOf("EUR")
        return currencyKeywords.none { keyword ->
            Regex("""\b${Regex.escape(keyword)}\b""").containsMatchIn(name)
        }
    }

    private fun hasMoreLettersThanDigits(input: String): Boolean {
        val letters = input.count { it.isLetter() }
        val digits = input.count { it.isDigit() }
        return letters > digits
    }

    fun containsWebsite(text: String): Boolean {
        val regex = Regex("""www\.[a-zA-Z0-9\-]+\.(com|de|net|org|info|biz|eu)""", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text)
    }

    private fun containsDate(text: String): Boolean {
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

    private fun containsTime(text: String): Boolean {
        val timeRegex = Regex("""\b([01]?[0-9]|2[0-3]):[0-5][0-9](\s?(Uhr|AM|PM|am|pm))?\b""")
        return timeRegex.containsMatchIn(text)
    }

    private fun containsInValidNumbers(text: String): Boolean {
        val tokens = text.trim().split(Regex("\\s+"))
        val firstToken = tokens.first()
        val tailTokens = if (firstToken.matches(Regex("^\\d+$"))) {
            tokens.drop(1)
        } else {
            tokens
        }
        val numberTokens = tailTokens.filter { it.matches(Regex("\\d+")) }
        val decimalRegex = Regex("""\d+[,.]\d+""")
        val matches = decimalRegex.findAll(text)
        return numberTokens.size >= 2 || matches.count() >= 2
    }

    private fun isNumericWithoutLetters(text: String): Boolean {
        if (text.contains(Regex("[a-zA-Z]"))) return false
        val numberMatch = Regex("""\d+[.,]?\d*""").find(text)
        return numberMatch != null
    }

    fun isPureInteger(input: String): Boolean {
        val regex = Regex("^\\d+$")
        return regex.matches(input)
    }

    fun isInvoiceItem(text: String): Boolean {
        if (!text.contains(SEPARATE_ITEM_PART)) return false

        if (text.contains("Trinkgeld gegeben:")) {
            Log.e("Suong123", text)
        }

        // Check for non-item keywords
        val nonItemKeywords = listOf(
            "gesamtsumme", "zahlung", "gegeben", "betrag", "summe",
            "kartenzahlung", "total", "t0tal", "wechselgeld", "bezahlt",
            "change", "gesamt", "mwst", "datum", "visa", "beleg-nr.",
            "beleg nummer", "belegnummer", "genehmigung", "terminalnummer",
            "zurück", "tax:", "lieferung", "ust.", "umsatzsteuer",
            "urnsatzstever", "urmsatzstever", "credit", "incl,", "incl.",
            "brutto", "netto", "card", "="
        )
        if (nonItemKeywords.any { text.lowercase(Locale.ROOT).contains(it) }) return false

        // Check for equal keywords in parts
        val parts = text.split(Regex(SEPARATE_ITEM_PART)).map { it.trim() }
        val equalKeyWord = listOf("tax:", "tax", "cash", "cash tendered:", "net", "netto", "ec-cash", "übertrag")
        if (parts.any { part -> equalKeyWord.any { it.equals(part.trim(), true) } }) return false

        // Check for currency format
        val hasCurrency = Regex("\\d{1,3}[.,]\\d{2}").containsMatchIn(text)
        if (!hasCurrency) return false

        // Ensure multiple parts exist
        if (parts.size <= 1) return false

        // Validate parts
        if (parts.all { hasMoreLettersThanDigits(it) }) return false
        if (containsDate(text)) return false
        if (containsTime(text)) return false
        if (parts.any { containsInValidNumbers(it.trim()) }) return false
        if (parts.all { isNumericWithoutLetters(it.trim()) }) return false
        if (parts.any { containsWebsite(it.trim()) }) return false

        return true
    }
}