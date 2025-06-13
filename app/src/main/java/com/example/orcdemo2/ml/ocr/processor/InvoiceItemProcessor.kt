package com.example.orcdemo2.ml.ocr.processor

import android.util.Log
import com.example.orcdemo2.ml.ocr.Constants.SEPARATE_ITEM_PART
import java.text.SimpleDateFormat
import java.util.Locale

object InvoiceItemProcessor {


    /**
     * Extracts the first numeric value (integer or decimal) found in the given text.
     *
     * This function is useful for parsing strings where numbers may appear in mixed
     * content such as item lines, quantities, or prices in OCR results.
     *
     * The regex captures numbers with optional decimal separators (dot or comma).
     * Only the **first match** is returned.
     *
     * @param text The input string possibly containing numbers (e.g., "x3 5,99 EUR").
     * @return The first numeric value found, as a string (e.g., "5,99"), or null if none found.
     *
     * Examples:
     * - "x3 5,99 EUR" => "3"
     * - "Preis: 12.45 €" => "12.45"
     * - "No numbers here" => null
     * - "10Stück" => "10"
     */
    fun extractNumberOnly(text: String?): String? {
        val regex = Regex("""\d+[.,]?\d*""")
        return text?.let { regex.find(it)?.value }
    }

    /**
     * Checks whether the given text is a valid numeric string (integer or decimal).
     *
     * A valid number must:
     * - Contain only digits, optionally with a decimal separator (dot or comma)
     * - Not include any non-numeric characters (e.g., letters, currency symbols)
     * - Be trimmed of whitespace before validation
     *
     * This function is often used to confirm that a string represents a clean number
     * suitable for parsing or arithmetic operations.
     *
     * @param text The input string to validate.
     * @return `true` if the string is a valid number format, otherwise `false`.
     *
     * Examples:
     * - "123" => true
     * - "45,67" => true
     * - "12.99" => true
     * - " 88 " => true
     * - "5,5%" => false
     * - "EUR 12.00" => false
     * - null => false
     */
    fun isValidNumber(text: String?): Boolean {
        val regex = Regex("""^\d+(?:[.,]\d+)?$""")
        if (text != null) {
            return regex.matches(text.trim())
        }
        return false
    }

    /**
     * Validates whether a given string is a likely product name.
     *
     * This function is used to filter out lines or tokens from OCR results that are
     * not valid product names (e.g., prices, currency-only strings, or short codes).
     *
     * Criteria for a valid product name:
     * - Must not be null or blank.
     * - Must contain **at least 4 letter characters** (ignores digits and symbols).
     * - Must **not contain standalone currency keywords** like "EUR".
     *
     * @param name The text to evaluate (typically a product name from OCR).
     * @return `true` if the name is likely a valid product name, otherwise `false`.
     *
     * Examples:
     * - "Milch 1L" => true
     * - "12,45 EUR" => false
     * - "ABC" => false (not enough letters)
     * - "Apfelmus Extra Fein" => true
     */
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

    /**
     * Checks whether a string contains more letters than digits.
     *
     * This function is useful for distinguishing text-like content (e.g., product names)
     * from number-heavy content (e.g., prices, codes) in OCR-parsed invoice lines.
     *
     * It counts all Unicode letters and digits, ignoring symbols and spaces.
     *
     * @param input The input string to evaluate.
     * @return `true` if the string contains more letters than digits, otherwise `false`.
     *
     * Examples:
     * - "Apfel 123" => true  (5 letters, 3 digits)
     * - "12345" => false
     * - "Milk2Go" => true
     * - "500ml" => false
     */
    private fun hasMoreLettersThanDigits(input: String): Boolean {
        val letters = input.count { it.isLetter() }
        val digits = input.count { it.isDigit() }
        return letters > digits
    }

    /**
     * Checks if the given text contains a website URL in common formats.
     *
     * This function uses a regex to detect typical website addresses starting with "www."
     * and ending with common top-level domains (TLDs) such as .com, .de, .net, etc.
     * It is useful for filtering out or identifying footer/header lines from OCR results.
     *
     * Currently supported TLDs: `.com`, `.de`, `.net`, `.org`, `.info`, `.biz`, `.eu`
     *
     * @param text The input string to check.
     * @return `true` if the text contains a website URL, otherwise `false`.
     *
     * Examples:
     * - "Visit us at www.example.com" => true
     * - "Kontakt: www.firma.de oder telefonisch" => true
     * - "Just some random text" => false
     */
    fun containsWebsite(text: String): Boolean {
        val regex = Regex("""www\.[a-zA-Z0-9\-]+\.(com|de|net|org|info|biz|eu)""", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text)
    }

    /**
     * Detects whether the input text contains a valid date in common formats.
     *
     * This function first normalizes the text to correct common OCR misread characters
     * (e.g., "O" → "0", "l1" → "11", "S" → "5", etc.). Then, it tries to match the cleaned
     * text against multiple common date formats using regular expressions.
     * If a match is found and successfully parsed into a valid date, it returns `true`.
     *
     * Supported date formats (with optional time):
     * - dd.MM.yyyy
     * - dd/MM/yyyy
     * - MM.dd.yyyy
     * - MM/dd/yyyy
     * - yyyy.MM.dd
     * - yyyy/MM/dd
     * - Each of the above with optional `HH:mm` time part
     *
     * @param text The raw input string (possibly OCR output).
     * @return `true` if a valid date is detected after normalization; `false` otherwise.
     *
     * Examples:
     * - "Rechnungsdatum: 12.06.2024" => true
     * - "Datum 0b.06.2023" => true (normalized to 06.06.2023)
     * - "Lieferung: 2023/11/05 13:45" => true
     * - "Artikelnummer 456Z21" => false
     */
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

    /**
     * Checks whether the given text contains a valid time expression.
     *
     * This function detects time patterns in the format of `HH:mm`, including:
     * - 24-hour format: e.g. "14:30", "09:15"
     * - 12-hour format with optional AM/PM: e.g. "2:45 PM", "11:00am"
     * - German format with "Uhr": e.g. "8:00 Uhr"
     *
     * Valid hours range from 00 to 23, and minutes from 00 to 59.
     * It is useful for identifying timestamps in OCR-parsed invoice data.
     *
     * @param text The input string to scan for time expressions.
     * @return `true` if a valid time is found; otherwise `false`.
     *
     * Examples:
     * - "Lieferzeit: 14:30 Uhr" => true
     * - "Termin um 9:15 AM" => true
     * - "Rechnung 17.04.2023" => false
     */
    private fun containsTime(text: String): Boolean {
        val timeRegex = Regex("""\b([01]?[0-9]|2[0-3]):[0-5][0-9](\s?(Uhr|AM|PM|am|pm))?\b""")
        return timeRegex.containsMatchIn(text)
    }

    /**
     * Checks whether the given text contains an invalid number pattern.
     *
     * This function is used to detect lines with multiple numeric values that might indicate
     * parsing errors or ambiguity in invoice item lines. It ignores a leading standalone number
     * (e.g., index or quantity), then:
     * - Counts how many remaining tokens are pure integers.
     * - Also counts how many decimal-like numbers exist (e.g., "12,50", "8.99").
     *
     * If there are at least 2 pure numbers or 2 decimal matches, it is considered invalid.
     *
     * @param text The input string to analyze.
     * @return `true` if the line contains multiple suspicious number values; otherwise `false`.
     *
     * Examples:
     * - "1 12,99 3,00" => true (two decimals)
     * - "3 1 5" => true (two integers after dropping the leading index)
     * - "Kalb.Schnitzel 12,50" => false
     */
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

    /**
     * Checks if the given text represents a numeric value **without any letters**.
     *
     * This function returns `true` if:
     * - The string contains no alphabetic characters (a–z, A–Z).
     * - It contains at least one numeric sequence, possibly with a decimal separator (dot or comma).
     *
     * Useful in invoice parsing to distinguish clean numeric fields (like prices or quantities)
     * from alphanumeric codes or descriptions.
     *
     * @param text The input string to validate.
     * @return `true` if the text is a number without letters; otherwise `false`.
     *
     * Examples:
     * - "12.50" => true
     * - "8,99" => true
     * - "EUR 12.50" => false
     * - "1a23" => false
     */
    private fun isNumericWithoutLetters(text: String): Boolean {
        if (text.contains(Regex("[a-zA-Z]"))) return false
        val numberMatch = Regex("""\d+[.,]?\d*""").find(text)
        return numberMatch != null
    }

    /**
     * Checks whether the given input string is a **pure integer**.
     *
     * A pure integer means:
     * - Only digits (0–9)
     * - No decimal separators (like '.' or ',')
     * - No spaces or other characters
     *
     * This is useful for identifying quantities, indexes, or counts in invoice data.
     *
     * @param input The input string to check.
     * @return `true` if the input is a valid whole number; otherwise `false`.
     *
     * Examples:
     * - "15" => true
     * - "003" => true
     * - "12.5" => false
     * - "10 EUR" => false
     * - "abc123" => false
     */
    fun isPureInteger(input: String): Boolean {
        val regex = Regex("^\\d+$")
        return regex.matches(input)
    }

    /**
     * Determines whether the given line of text is likely to be a **valid invoice item line**.
     *
     * This function is designed for extracting product or service items from invoices. It applies multiple filters
     * to exclude lines that likely represent totals, tax info, payment summaries, or metadata.
     *
     * ### Criteria for a valid item line:
     * - Must contain the item separator (`SEPARATE_ITEM_PART`, e.g. `"|"`).
     * - Must NOT contain any known non-item keywords (e.g., "gesamt", "betrag", "mwst", "beleg-nr.").
     * - None of the parts should match known labels like `"net"`, `"tax"`, or `"cash"` exactly.
     * - Should include at least one valid currency-like number (e.g., `12,99` or `12.99`).
     * - Must contain more than one separated part.
     * - Should not consist entirely of text (i.e., must contain some numbers).
     * - Should not contain a date, time, or website.
     * - Should not include two or more standalone numbers/decimals (e.g., `5 3.00 4.00`) to avoid misinterpreting totals.
     * - Should not be fully numeric-only across all parts (e.g., `"12 | 30"`).
     *
     * @param text The input line to evaluate.
     * @return `true` if the line is likely a product or item entry; otherwise `false`.
     *
     * ### Examples:
     * - `"1 Stück | Apfelsaft 1L | 1,50"` => `true`
     * - `"Gesamtsumme | 14,40"` => `false`
     * - `"12.99 | MwSt 7%"` => `false`
     * - `"Wasserflasche | 0,50"` => `true`
     */
    fun isInvoiceItem(text: String): Boolean {
        if (!text.contains(SEPARATE_ITEM_PART)) return false



        // Check for non-item keywords
        val nonItemKeywords = listOf(
            "gesamtsumme", "zahlung", "gegeben", "betrag", "summe","smme",
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