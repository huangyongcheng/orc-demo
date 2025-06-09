package com.example.orcdemo2.ml

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

object InvoiceItemUtil {

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
            val parts = text.split(Regex("""\s+\|\s+""")).map { it.trim() }
            val hasEqualKeyWord = parts.any {part-> equalKeyWord.any { it.equals(part.trim(), true) } }
            if(hasEqualKeyWord){
                return false
            }

            // Rule 4: contains format of quantity + unit price + total price (with comma or dot)
            val hasCurrency = Regex("\\d{1,3}[.,]\\d{2}").containsMatchIn(text)
            if(hasCurrency){

                val parts = text.split(Regex("""\s+\|\s+""")).map { it.trim() }
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
            val unitIndicators = listOf("stück", "packung", "flasche")
            if (unitIndicators.any { text.contains(it) }) {
                return true
            }


        }

        return false
    }


    fun convertToInvoiceItem(line: String): InvoiceItem {
        val parts = line.split(Regex("""\s+\|\s+""")).map { it.trim() }

        if (parts.size < 2) return InvoiceItem("","")

        val price = parts.last()
        val name = parts.dropLast(1).joinToString(" | ")

        return InvoiceItem(name = name, price = price)
    }

    data class InvoiceItem(
        val name: String,
        val price: String
    )

}