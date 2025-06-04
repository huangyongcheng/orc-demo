package com.example.orcdemo2.ml

data class InvoiceItem (
    val name: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val totalPrice: Double
)