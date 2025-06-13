package com.example.orcdemo2.ml.ocr.model


data class InvoiceData(
    val vat: String? = null,
    val total: String? = null,
    val items: List<InvoiceItem>? = null
)

data class InvoiceItem(
    val name: String? = null,
    val quantity: String? = null,
    val totalPrice: String? = null,
)