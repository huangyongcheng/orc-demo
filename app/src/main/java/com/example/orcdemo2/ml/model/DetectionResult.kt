package com.example.orcdemo2.ml.model

import android.graphics.RectF

data class DetectionResult(   val itemName: String,
                              val quantity: Float,
                              val unitPrice: Float,
                              val boundingBox: RectF
)
