package com.example.orcdemo2.ml.thuattoan

object ExtractItem {

//    data class InvoiceItem(
//        val name: String,
//        val quantity: Double?,
//        val unitPrice: Double?,
//        val totalPrice: Double?
//    )
//
//    fun extractInvoiceItemsGeneral(ocrText: String): List<InvoiceItem> {
//        val items = mutableListOf<InvoiceItem>()
//        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
//
//        val numberRegex = Regex("""\d+[,.]?\d*""")
//
//        for (line in lines) {
//            // Tìm tất cả số trong dòng
//            val numbers = numberRegex.findAll(line).map { it.value.replace(",", ".") }.toList()
//
//            // Nếu có từ 2 đến 3 số thì có thể là dòng sản phẩm
//            if (numbers.size in 2..3) {
//                val totalPrice = numbers.lastOrNull()?.toDoubleOrNull()
//                val unitPrice = if (numbers.size >= 2) numbers[numbers.size - 2].toDoubleOrNull() else null
//                val quantity = if (numbers.size == 3) numbers.first().toDoubleOrNull() else null
//
//                // Tên = phần còn lại (sau khi loại bỏ số)
//                val name = line.replace(numberRegex, "").replace(Regex("[€xX]"), "").trim()
//
//                // Nếu tên không phải là số, thì coi là item
//                if (name.any { it.isLetter() }) {
//                    items.add(InvoiceItem(name, quantity, unitPrice, totalPrice))
//                }
//            }
//        }
//
//        return items
//    }




//    data class OcrWord(val text: String, val x: Int, val y: Int, val width: Int, val height: Int)
//
//    data class InvoiceItem(
//        val name: String,
//        val quantity: Double?,
//        val unitPrice: Double?,
//        val totalPrice: Double?
//    )
//
//    fun extractInvoiceItemsFromBoxes(words: List<OcrWord>): List<InvoiceItem> {
//        // 1. Nhóm từ theo dòng (y gần nhau)
//        val lineThreshold = 20
//        val lines = words.groupBy { word -> word.y / lineThreshold }
//
//        val numberRegex = Regex("""\d+[,.]?\d*""")
//        val items = mutableListOf<InvoiceItem>()
//
//        for ((_, wordLine) in lines) {
//            val sortedWords = wordLine.sortedBy { it.x }
//
//            val numbers = sortedWords.filter { numberRegex.matches(it.text) }
//            if (numbers.size < 2) continue // skip lines without price
//
//            val textLine = sortedWords.joinToString(" ") { it.text }
//
//            val quantity = numbers.getOrNull(0)?.text?.replace(",", ".")?.toDoubleOrNull()
//            val unitPrice = numbers.getOrNull(1)?.text?.replace(",", ".")?.toDoubleOrNull()
//            val totalPrice = numbers.last().text.replace(",", ".").toDoubleOrNull()
//
//            // Lấy phần chữ không phải số làm tên sản phẩm
//            val nameWords = sortedWords.filter { !numberRegex.matches(it.text) }
//            val name = nameWords.joinToString(" ") { it.text }
//
//            if (name.any { it.isLetter() }) {
//                items.add(InvoiceItem(name, quantity, unitPrice, totalPrice))
//            }
//        }
//
//        return items
//    }



//    data class InvoiceItem(
//        val name: String,
//        val quantity: String,
//        val unitPrice: String,
//        val totalPrice: String
//    )
//
//    fun extractInvoiceItems(lines: List<String>): List<InvoiceItem> {
//        val items = mutableListOf<InvoiceItem>()
//        val itemLinePattern = Regex("""(?i)(.+?)\s+([\d.,]+)\s*[xX×*]\s*([\d.,]+)\s+([\d.,]+)""")
//
//        for (line in lines) {
//            val match = itemLinePattern.find(line)
//            if (match != null) {
//                val (name, quantity, unitPrice, totalPrice) = match.destructured
//                items.add(
//                    InvoiceItem(
//                        name = name.trim(),
//                        quantity = quantity.trim(),
//                        unitPrice = unitPrice.trim(),
//                        totalPrice = totalPrice.trim()
//                    )
//                )
//            }
//        }
//
//        return items
//    }



//    data class InvoiceItem(
//        val name: String,
//        val quantity: Float,
//        val unit: String,
//        val unitPrice: Float,
//        val totalPrice: Float
//    )
//
//    fun extractInvoiceItems(lines: List<String>): List<InvoiceItem> {
//        val items = mutableListOf<InvoiceItem>()
//
//        // Regex: ví dụ "Rübli-Muffin", "6,00 Stück x 2,40 14,40"
//        val quantityLineRegex = Regex("""(\d+[.,]?\d*)\s*(Stück|kg|x)?\s*[xX×]\s*(\d+[.,]?\d*)\s+(\d+[.,]?\d*)""")
//
//        var i = 0
//        while (i < lines.size - 1) {
//            val name = lines[i].trim()
//            val nextLine = lines[i + 1].trim()
//
//            val match = quantityLineRegex.find(nextLine)
//            if (match != null) {
//                val (qtyStr, unit, priceStr, totalStr) = match.destructured
//
//                val quantity = qtyStr.replace(",", ".").toFloatOrNull() ?: continue
//                val unitPrice = priceStr.replace(",", ".").toFloatOrNull() ?: continue
//                val totalPrice = totalStr.replace(",", ".").toFloatOrNull() ?: continue
//
//                items.add(
//                    InvoiceItem(
//                        name = name,
//                        quantity = quantity,
//                        unit = unit.ifEmpty { "x" },
//                        unitPrice = unitPrice,
//                        totalPrice = totalPrice
//                    )
//                )
//                i += 2 // skip to next item
//            } else {
//                i++
//            }
//        }
//
//        return items
//    }


    data class InvoiceItem(
        val name: String,
        val quantity: Double,
        val unit: String,
        val unitPrice: Double,
        val totalPrice: Double
    )

    data class Invoice(
        val orderNumber: String,
        val invoiceNumber: String,
        val date: String,
        val branch: String,
        val cashier: String,
        val items: List<InvoiceItem>,
        val paymentMethod: String,
        val amountPaid: Double,
        val change: Double
    )

    fun extractInvoiceFromText(text: String): Invoice {
        val lines = text.lines()

        // Extract header information
        val orderNumber = lines.find { it.contains("Bestellung Nr.:") }?.substringAfter("Bestellung Nr.:")?.trim() ?: ""
        val invoiceNumber = lines.find { it.contains("RECHNUNG") }?.substringAfter("RECHNUNG")?.trim() ?: ""
        val date = lines.find { it.contains("Datum") }?.substringAfter("Datum")?.substringBefore("Filiale:")?.trim() ?: ""
        val branch = lines.find { it.contains("Filiale:") }?.substringAfter("Filiale:")?.substringBefore("Kasse:")?.trim() ?: ""
        val cashier = lines.find { it.contains("Es bediente Sie") }?.substringAfter("Es bediente Sie")?.trim() ?: ""
        val paymentMethod = lines.find { it.contains("Gegeben") }?.substringAfter("Gegeben")?.substringBefore(":")?.trim() ?: ""
        val amountPaid = lines.find { it.contains("Gegeben") }?.substringAfterLast(" ")?.trim()?.toDoubleOrNull() ?: 0.0
        val change = lines.find { it.contains("Zurück:") }?.substringAfterLast(" ")?.trim()?.toDoubleOrNull() ?: 0.0

        // Extract items (assuming items are between "---" lines)
        val items = mutableListOf<InvoiceItem>()
        var inItemsSection = false

        for (line in lines) {
            if (line.contains("---")) {
                inItemsSection = !inItemsSection
                continue
            }

            if (inItemsSection && line.isNotBlank() && !line.contains("Artikelname") && !line.contains("Gesamtsumme")) {
                val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (parts.size >= 5) {
                    val name = parts[0]
                    val quantity = parts[1].replace(",", ".").toDoubleOrNull() ?: 0.0
                    val unit = parts[2]
                    val unitPrice = parts[4].replace(",", ".").toDoubleOrNull() ?: 0.0
                    val totalPrice = parts.last().replace(",", ".").toDoubleOrNull() ?: 0.0

                    items.add(InvoiceItem(name, quantity, unit, unitPrice, totalPrice))
                }
            }
        }

        return Invoice(
            orderNumber = orderNumber,
            invoiceNumber = invoiceNumber,
            date = date,
            branch = branch,
            cashier = cashier,
            items = items,
            paymentMethod = paymentMethod,
            amountPaid = amountPaid,
            change = change
        )
    }
}