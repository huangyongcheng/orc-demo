package com.example.orcdemo2.ml

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.orcdemo2.R
import com.example.orcdemo2.ml.InvoiceItemUtil.SEPARATE_ITEM_PART
import com.example.orcdemo2.ml.InvoiceItemUtil.convertToInvoiceItem
import com.example.orcdemo2.ml.InvoiceItemUtil.isInvoiceItem
import com.example.orcdemo2.ml.demo.LayoutExtractor
import com.example.orcdemo2.ml.demo.LayoutSectionType
import com.example.orcdemo2.ml.demo.SectionClassifier
import com.example.orcdemo2.ml.tflite2.ImageClassifier
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale


class MLActivity : Activity() {
    private var classifier: ImageClassifier? = null

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ml)

        /*
        findViewById<Button>(R.id.selectImageBtn).setOnClickListener {
           // pickImageFromGallery()
            val isItem = isInvoiceItem("1\\u003d19,00%   |  10,80   |  9,08   |  1,72")
            //"VU-Nr.   |  4556353644
            Log.e("Suong","istem: "+ isItem)

            val list = readProductsFromAssets(this@MLActivity,"ocr_test/test.json")

            val listinvoce = mutableListOf<LayoutLine>()
            for (layoutLine in list) {

                if(isInvoiceItem(layoutLine.text)){
                    listinvoce.add(layoutLine)
                }
            }
            Log.e("Suong","list invoice: "+ Gson().toJson(listinvoce))
            writeToFile(this@MLActivity,"result.json",Gson().toJson(listinvoce) )
        }

        findViewById<Button>(R.id.selectImageBtn2).setOnClickListener {
            val list = readProductsFromAssets(this@MLActivity,"ocr_test/test2.json")

            val listinvoce = mutableListOf<LayoutLine>()
            for (layoutLine in list) {

                if(isInvoiceItem(layoutLine.text)){
                    listinvoce.add(layoutLine)
                }
            }
            Log.e("Suong","list invoice: "+ Gson().toJson(listinvoce))
            writeToFile(this@MLActivity,"result2.json",Gson().toJson(listinvoce) )
        }
        findViewById<Button>(R.id.selectImageBtn3).setOnClickListener {
            val list = readProductsFromAssets(this@MLActivity,"ocr_test/test3.json")

            val listinvoce = mutableListOf<LayoutLine>()
            for (layoutLine in list) {

                if(isInvoiceItem(layoutLine.text)){
                    listinvoce.add(layoutLine)
                }
            }
            Log.e("Suong","list invoice: "+ Gson().toJson(listinvoce))
            writeToFile(this@MLActivity,"result3.json",Gson().toJson(listinvoce) )
        }

        findViewById<Button>(R.id.selectImageBtn4).setOnClickListener {
            val list = readProductsFromAssets(this@MLActivity,"ocr_test/test4.json")

            val listinvoce = mutableListOf<LayoutLine>()
            for (layoutLine in list) {

                if(isInvoiceItem(layoutLine.text)){
                    listinvoce.add(layoutLine)
                }
            }
            Log.e("Suong","list invoice: "+ Gson().toJson(listinvoce))
            writeToFile(this@MLActivity,"result4.json",Gson().toJson(listinvoce) )
        }

        findViewById<Button>(R.id.selectImageBtnAll).setOnClickListener {
            findViewById<Button>(R.id.selectImageBtn).performClick()
            findViewById<Button>(R.id.selectImageBtn2).performClick()
            findViewById<Button>(R.id.selectImageBtn3).performClick()
            findViewById<Button>(R.id.selectImageBtn4).performClick()
        }

         */

       // findViewById<Button>(R.id.selectImageBtnAll).performClick()
        testInvoiceOrc()

    }

    private fun convert2InvoiceItem(itemInvoices: List<LayoutLine>) : MutableList<InvoiceItemUtil.InvoiceItem>{
        val listItems = mutableListOf<InvoiceItemUtil.InvoiceItem>()
        itemInvoices.forEach {
            listItems.add(convertToInvoiceItem(it.text))
        }
        return listItems
    }

    private fun testInvoiceOrc() {
        filesDir.delete()
        val allFiles = assets.list("ocr_test")
        allFiles?.filter { it.startsWith("test") }?.forEach {


            val listORC = readProductsFromAssets(this@MLActivity, "ocr_test/$it")

            val invoiceItems = mutableListOf<LayoutLine>()

            listORC.forEachIndexed { index, layoutLine ->

                // Debug: check specific line
                if (layoutLine.text.contains("6,00 Stück x")) {
                    Log.e("Suong", "layoutLine: ${layoutLine}")
                }

                if (isInvoiceItem(layoutLine.text)) {
                    if (index > 0) {
                        val previousLayoutLine = listORC[index - 1]
                        val previousParts = previousLayoutLine.text.split(Regex("""\s+\|\s+""")).map { it.trim() }


                        if(previousLayoutLine.text.contains("Mehr unter")){
                            Log.e("Suong123", previousLayoutLine.text)
                        }
                        // If the previous line has only one part and the current line is visually more right-aligned
                        val shouldCombine = previousParts.size == 1 &&
                                layoutLine.minX > previousLayoutLine.minX &&
                                !InvoiceItemUtil.containsWebsite(previousLayoutLine.text)

                        if (shouldCombine) {
                            // Combine the previous and current layout lines into one invoice item
                            val combinedText = "${previousLayoutLine.text}$SEPARATE_ITEM_PART${layoutLine.text}"
                            val combinedLayoutLine = LayoutLine(
                                text = combinedText,
                                midY = previousLayoutLine.midY,
                                minX = previousLayoutLine.minX,
                                maxX = previousLayoutLine.maxX
                            )
                            invoiceItems.add(combinedLayoutLine)
                        } else {
                            // Add current layout line directly if no need to combine
                            invoiceItems.add(layoutLine)
                        }

                    } else {
                        // First item, no previous line to compare
                        invoiceItems.add(layoutLine)
                    }
                }
            }
            writeToFile(this@MLActivity, "result_${it}",
                Gson().toJson(invoiceItems))
            writeToFile(this@MLActivity, "result_${it}_item",
                Gson().toJson(InvoiceItemUtil.convert2InvoiceData(invoiceItems, listORC)))

        }
        mergeJsonFiles(this@MLActivity)
    }

    data class InvoiceItem2(
        val name: String,
        val price: String
    )


    data class LayoutLine(
        val text: String,
        val midY: Float,
        val minX: Float,
        val maxX: Float
    )

    fun writeToFile(context: Context, fileName: String?, jsonRaw: String) {
        try {
            // Parse lại chuỗi JSON để đảm bảo hợp lệ, rồi pretty-print
            val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
            val jsonElement = Gson().fromJson(jsonRaw, com.google.gson.JsonElement::class.java)
            val prettyJson = gsonPretty.toJson(jsonElement)

            val fos = context.openFileOutput(fileName, MODE_PRIVATE)
            fos.write(prettyJson.toByteArray())
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace() // JSON sai định dạng cũng bắt được
        }
    }

    private fun mergeJsonFiles(context: Context) {


        try {
            val dirFiles = context.filesDir.listFiles()

            if (dirFiles == null) return

            // Lọc và sắp xếp file theo số cuối cùng trong tên file
            val sortedFiles = dirFiles
                .filter { it.name.endsWith(".json_item") }
                .sortedWith(compareBy {
                    // Tìm số cuối cùng trong tên file, nếu không có thì dùng 0
                    Regex("(\\d+)").find(it.name)?.groupValues?.last()?.toIntOrNull() ?: 0
                })

            val stringBuilder = StringBuilder()

            for (file in sortedFiles) {
                try {
                    val content = file.readText()
                    stringBuilder.append("\nFile name: ${file.name}\n")
                    stringBuilder.append(content.trim())
                    stringBuilder.append("\n================================\n")
                } catch (e: Exception) {
                    e.printStackTrace() // Bỏ qua file lỗi
                }
            }

            val result = stringBuilder.toString().trimEnd()

            // Ghi ra file AtestAllFile.json
            val fos = context.openFileOutput("AtestAllFile.json", Context.MODE_PRIVATE)
            fos.write(result.toByteArray())
            fos.close()

        } catch (e: Exception) {
            Log.e("Suong","exception: "+ e.message)
            e.printStackTrace()
        }
    }

    fun readProductsFromAssets(context: Context, fileName: String): List<LayoutLine> {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val gson = Gson()
        val listType = object : TypeToken<List<LayoutLine>>() {}.type
        return gson.fromJson(jsonString, listType)
    }

    // test12 => true
    fun hasMoreLettersThanDigits(input: String): Boolean {
        val letters = input.count { it.isLetter() }
        val digits = input.count { it.isDigit() }
        return letters > digits
    }

    fun containsDate(text: String): Boolean {
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
                } catch (e: Exception) {
                    // Không phải ngày hợp lệ, tiếp tục thử pattern khác
                }
            }
        }
        return false
    }

    fun containsTime(text: String): Boolean {
        val timeRegex = Regex("""\b([01]?[0-9]|2[0-3]):[0-5][0-9](\s?(Uhr|AM|PM|am|pm))?\b""")
        return timeRegex.containsMatchIn(text)
    }

    fun containsInValidNumbers(text: String): Boolean {
        // Tìm tất cả cụm số cách nhau bằng dấu cách
        val tokens = text.trim().split(Regex("\\s+"))
        val numberTokens = tokens.filter { it.matches(Regex("\\d+")) }

        // 0,94 14,40 return true (invalid)
        val decimalRegex = Regex("""\d+[,.]\d+""")
        val matches = decimalRegex.findAll(text)
        return numberTokens.size >= 2 || matches.count() >= 2
    }

    // 20%   |  13.85 => true
    fun isNumericWithoutLetters(text: String): Boolean {
        // Nếu có bất kỳ chữ cái nào, trả về false
        if (text.contains(Regex("[a-zA-Z]"))) return false

        // Tìm tất cả chuỗi số kiểu thập phân (cho phép , hoặc .)
        val numberMatch = Regex("""\d+[.,]?\d*""").find(text)

        // Nếu có ít nhất 1 chuỗi số đúng định dạng, trả về true
        return numberMatch != null
    }

    // "*6,50 EUR   |  A,1" => true
    fun containsInvalidFraction(text: String): Boolean {
        // Kiểm tra nếu chuỗi không chứa dấu ',' hoặc '.' thì không phải phân số → false
        if (!text.contains(",") && !text.contains(".")) return false

        // Tách theo khoảng trắng thành các token
        val tokens = text.trim().split("\\s+".toRegex())

        // Duyệt từng token, kiểm tra nếu token chứa dấu ',' hoặc '.'
        for (token in tokens) {
            if (token.contains(",") || token.contains(".")) {
                val parts = token.split(",", ".")

                // Nếu không đúng 2 phần hoặc bất kỳ phần nào không phải số, thì là không hợp lệ
                if (parts.size != 2 || parts.any { !it.matches(Regex("\\d+")) }) {
                    return true
                }
            }
        }

        return false
    }

    // "*6,50 EUR   |  A,1" => true
    // have the first index start with number
    fun startsWithNumberAfterSpecial(text: String): Boolean {
        // Bỏ các ký tự đặc biệt ở đầu (không phải chữ số hoặc chữ cái)
        val cleaned = text.trim().dropWhile { !it.isLetterOrDigit() }

        // Nếu chuỗi rỗng hoặc bắt đầu là chữ cái thì không hợp lệ
        if (cleaned.isEmpty() || cleaned.first().isLetter()) return false

        // Tìm số đầu tiên sau khi làm sạch (số nguyên hoặc thập phân dùng , hoặc .)
        val match = Regex("^\\d+[,.]?\\d*").find(cleaned)
        return match != null
    }

    // không check dự vào dấu :
    // không check nếu name mà có số đầu tiên được

    fun isInvoiceItem3(text:String):Boolean {
        if(text.contains("*6,50 EUR   |  A,1")){
            Log.e("Suong","text: "+ text)
        }
        if (text.contains(" | ")) {
            // Rule 1: loại trừ các từ khóa liên quan đến thanh toán, tổng tiền
            val nonItemKeywords = listOf(
                "gesamtsumme", "zahlung", "gegeben", "betrag", "summe",
                "kartenzahlung", "total", "wechselgeld", "bezahlt", "change","gesamt","mwst","datum",
                "visa","beleg-nr.","beleg nummer","belegnummer","genehmigung",
                "terminalnummer","zurück","tax:","lieferung",
                "=",
            )
            if (nonItemKeywords.any { text.lowercase(Locale.ROOT).contains(it) }) return false

            // Rule 2: chứa từ khóa thường gặp trong dòng item
            val itemKeywords = listOf("stück", "einheit",  "einzel", "artikel", "qty")
            if (itemKeywords.any { text.contains(it) }) {
                Log.e("Suong","itemKeywords: "+ text)
                return true
            }

            // the part equal list key word
            // Tax: => false
            val equalKeyWord = listOf("tax:","tax","cash","cash tendered:")
            val parts = text.split(Regex("""\s+\|\s+""")).map { it.trim() }

            val hasEqualKeyWord = parts.any {part-> equalKeyWord.any { it.equals(part.trim(), true) } }
            if(hasEqualKeyWord){
                return false
            }

            // Rule 4: chứa định dạng số lượng + đơn giá + tổng giá (có dấu phẩy hoặc chấm)
            val hasCurrency = Regex("\\d{1,3}[.,]\\d{2}").containsMatchIn(text)

          //  if (hasCurrency && text.count { it.isDigit() } >= 4) return true
            if(hasCurrency){

                val parts = text.split(Regex("""\s+\|\s+""")).map { it.trim() }
                if(parts.size>1) {
                    val hasMoreLettersThanDigits = parts.all { hasMoreLettersThanDigits(it) }
                    if (hasMoreLettersThanDigits) {
                        return false
                    } else if (containsDate(text)) {
                        Log.e("Suong", "containsDate: " + text)
                        return false
                    } else if (containsTime(text)) {
                        Log.e("Suong", "containsDate: " + text)
                        return false

                    } else {
                        val hasContainsInValidNumbers = parts.any { containsInValidNumbers(it.trim()) }
                        if (hasContainsInValidNumbers) {
                            Log.e("Suong", "hasContainsValidNumbers: " + text)
                            return false
                        } else if (parts.all { isNumericWithoutLetters(it.trim()) }) {
                            Log.e("Suong", "hasContainsValidNumbers: " + text)
                            return false
                        }
                        else {
                            Log.e("Suong", "hasCurrency: " + text)
                            return true
                        }

                    }
                } else {
                    return false
                }
            }



            // Rule 7: dòng có chứa từ hoặc ký tự định lượng như “x”, “kg”, “Stück”, v.v.
            val unitIndicators = listOf("stück", "packung", "flasche")
            if (unitIndicators.any { text.contains(it) }) {
                Log.e("Suong","unitIndicators: "+ text)
                return true
            }


        }

        return false
    }

    fun isInvoiceItem2(text:String):Boolean{


        // the keyword
        val ignoreKeywords = listOf(
            "datum", "visa", "beleg", "netto", "brutto", "miete", "start", "ende",
            "steuer", "signatur", "tse", "payback", "genehm", "terminal",
            "gesamtbetrag", "mwst", "typ","betrag","trace-nr"
        )
        if (ignoreKeywords.any { it in text }) return false

        // contain "|"
        if (!text.contains("|")) return false

        // minimum length
        if (text.length < 15) return false

        //
        val mostlyDigits = text.count { it.isDigit() } > text.length * 0.6
        if (mostlyDigits) return false

        // Separate string by "   |  "
        val parts = text.split(Regex("""\s+\|\s+""")).map { it.trim() }

        // all part is letter
        val containsLetterRegex = Regex("""\p{L}""") // \p{L} = Unicode letter
        val allPartsContainLetter = parts.all { part ->
            containsLetterRegex.containsMatchIn(part)
        }
        if (allPartsContainLetter) return false

        //
        val hasOnlyDigitsWithSpaces = parts.any { part ->
            part.replace(" ", "").all { it.isDigit() } && part.contains(" ")
        }
        if (hasOnlyDigitsWithSpaces) return false
        //
        if (parts.isNotEmpty()) {

            var allAreLettersOrCurrency = true
            var allAreLongDigits = true


            for (part in parts) {
                val lower = part.lowercase()

                // Check if not all text/currency → set false
                if (!(part.all { it.isLetter() || it.isWhitespace() } || "eur" in lower)) {
                    allAreLettersOrCurrency = false
                }

                // Check if not long number → set false
                val isAllDigits = part.all { it.isDigit() }
                if(isAllDigits){
                    if ( part.length > 7) {
                        allAreLongDigits = false
                    }
                }

            }

            if (allAreLettersOrCurrency || !allAreLongDigits) return false
        }

        //
        val endsWithAmount = Regex("""[\d,.]+\s*(eur)?\s*#?\*?$""").containsMatchIn(text)
        if (!endsWithAmount) return false

        return true
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri = data.data!!
            findViewById<ImageView>(R.id.imageView).setImageURI(imageUri)
            runTextRecognition(imageUri)
//            processInvoice(imageUri)

        }
    }



    private fun runTextRecognition(imageUri: Uri) {
//        val bitmap = if (Build.VERSION.SDK_INT < 28) {
//            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
//        } else {
//            val source = ImageDecoder.createSource(contentResolver, imageUri)
//            ImageDecoder.decodeBitmap(source)
//
//        }

        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)


//        val result = classifier!!.classify(bitmap)
//        Log.e("Suong","result: "+ result)

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                Log.e("Suong","Text: "+ visionText.text)

//                val value = parseInvoiceItems(visionText.text)
//                Log.e("Suong","value list: "+ value.toList().toString())
               // processInvoiceText(visionText.text)
//                parseLineItems(visionText.text)
               findViewById<TextView>(R.id.textResult).text = visionText.text



//                val words = mutableListOf<ExtractItem.OcrWord>()
//                for (block in visionText.textBlocks) {
//                    for (line in block.lines) {
//                        for (element in line.elements) {
//                            val rect = element.boundingBox
//                            if (rect != null) {
//                                words.add(ExtractItem.OcrWord(element.text, rect.left, rect.top, rect.width(), rect.height()))
//                            }
//                        }
//                    }
//                }
//
//                val items = extractInvoiceItemsFromBoxes(words)
//                Log.e("Suong","items: "+Gson().toJson(items))



                //
//                val text = visionText.textBlocks.flatMap { it.lines }
//                val layoutExtractor = LayoutLine.LayoutExtractor()
//                val rawSections = layoutExtractor.extractSections(from = text)
//
//                val classifier = LayoutLine.SectionClassifier()
//                val classifiedSections = classifier.classify(rawSections).filter { it.type == LayoutLine.LayoutSectionType.ITEM_LIST }
//
//                Log.e("Suong","classifiedSections "+ Gson().toJson(classifiedSections))


                //

//                val invoice = extractInvoiceFromText(visionText.text.trimIndent())
//                Log.e("Suong","invoice "+ Gson().toJson(invoice))


                //

                val extractor = LayoutExtractor(bitmap.height)
                val classifier = SectionClassifier()

                val sectionsUnknown = extractor.extractSections(visionText)
                val sections = classifier.classify(sectionsUnknown)

                for (section in sections) {
                    Log.e("SuongVong","=== Section type: ${section.type} ===")
                    for (line in section.lines) {
                        Log.e("SuongVong",line.text)
                    }
                    if (section.type == LayoutSectionType.ITEM_LIST) {
                        val items = classifier.parseItemList(section.lines)
                        Log.e("SuongVong","--- Parsed Items ---")
                        for (item in items) {
                            Log.e("SuongVong","Product: ${item.productName}, Quantity: ${item.quantity ?: 0}, UnitPrice: ${item.unitPrice ?: 0}, LineTotal: ${item.lineTotal ?: 0}")
                        }
                    }
                }



            }
            .addOnFailureListener { e ->
                findViewById<TextView>(R.id.textResult).text = "Error: ${e.message}"
            }
    }




}