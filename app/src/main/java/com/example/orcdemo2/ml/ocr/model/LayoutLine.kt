package com.example.orcdemo2.ml.ocr.model

data class LayoutLine(
    val text: String,
    val midY: Float,
    val minX: Float,
    val maxX: Float
)
