package com.example.orcdemo2.ml

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.orcdemo2.R
import com.example.orcdemo2.ml.demo.LayoutExtractor
import com.example.orcdemo2.ml.demo.LayoutSectionType
import com.example.orcdemo2.ml.demo.SectionClassifier
import com.example.orcdemo2.ml.tflite.ModelWrapper
import com.example.orcdemo2.ml.tflite.MyTokenizer
import com.example.orcdemo2.ml.tflite2.ImageClassifier
import com.example.orcdemo2.ml.thuattoan.ExtractItem
import com.example.orcdemo2.ml.thuattoan.ExtractItem.extractInvoiceFromText

import com.example.orcdemo2.ml.thuattoan.LayoutLine
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException


class MLActivity : Activity() {
    private var classifier: ImageClassifier? = null

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ml)

        findViewById<Button>(R.id.selectImageBtn).setOnClickListener {
            pickImageFromGallery()
        }

      //  invoiceParser = InvoiceParser(this@MLActivity)

        try {
            classifier = ImageClassifier(this)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show()
        }
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


    fun parseInvoiceItems(text: String): List<InvoiceItem> {
        val items = mutableListOf<InvoiceItem>()
        val lines = text.split("\n")

        // Example: Find lines containing product name, quantity, and price
        val pattern = Regex("""([A-Za-z-]+)\s+([\d,]+)\s+([A-Za-z]+)\s+x\s+([\d,]+)\s+([\d,]+)""")

        lines.forEach { line ->
            val match = pattern.find(line)
            match?.let {
                val (name, qty, unit, price, total) = it.destructured
                items.add(
                    InvoiceItem(
                        name.trim(),
                        qty.replace(",", ".").toDouble(),
                        unit.trim(),
                        price.replace(",", ".").toDouble(),
                        total.replace(",", ".").toDouble()
                    )
                )
            }
        }
        return items
    }

    data class InvoiceItem(
        val name: String,
        val quantity: Double,
        val unit: String,
        val unitPrice: Double,
        val totalPrice: Double
    ) {
        override fun toString(): String {
            return "Item: $name | Qty: $quantity $unit | Price: $unitPrice€ | Total: $totalPrice€"
        }
    }

    ///

    private var invoiceParser: InvoiceParser? = null

    fun processInvoice(imageUri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        val items = invoiceParser?.detectItems(bitmap)
        Log.e("Suong","items "+  items.toString())
//        _invoiceItems.value = items.map { item ->
//            InvoiceItem(
//                name = extractTextFromBoundingBox(bitmap, item.boundingBox),
//                quantity = item.quantity,
//                unit = "Stück",  // Extract from OCR
//                unitPrice = item.unitPrice,
//                totalPrice = item.unitPrice * item.quantity
//            )
//        }
    }


    fun processInvoiceText(ocrText: String) {
        // Step 1: Tokenize the text (use WordPiece tokenizer)
        val tokenizer = MyTokenizer.loadFromAssets(assets, "vocab5.txt")
        val (inputIds, attentionMask, tokens) = tokenizer.tokenize(ocrText, maxLen = 128)

        // Step 2: Run TFLite model
        val model = ModelWrapper(assets, "bert_ner5.tflite", maxSeqLen = 128)
        val predictedLabels = model.predict(inputIds, attentionMask)

        // Step 3: Extract entities
        val entities = model.extractEntities(tokens, predictedLabels)

        // Step 4: Show items
        val items = entities["ITEM"] ?: emptyList()
        Log.e("ExtractedItems", items.joinToString("\n"))
    }



    private fun parseLineItems(text: String) {
        val lineItems = mutableListOf<Map<String, String>>()
        val lines = text.split("\n")

        // Simple regex to match line items (adjust based on invoice format)
        val lineItemPattern = Regex("^(.*?)\\s+(\\d+)\\s+([\\d.]+)\\s+([\\d.]+)$")

        for (line in lines) {
            val match = lineItemPattern.find(line.trim())
            if (match != null) {
                val (description, quantity, unitPrice, total) = match.destructured
                val item = mapOf(
                    "description" to description.trim(),
                    "quantity" to quantity,
                    "unitPrice" to unitPrice,
                    "total" to total
                )
                lineItems.add(item)
            }
        }

        // Log or process extracted line items
        lineItems.forEach { item ->
            Log.e("LineItem", "Description: ${item["description"]}, Qty: ${item["quantity"]}, " +
                    "Unit Price: ${item["unitPrice"]}, Total: ${item["total"]}")
        }
    }
}