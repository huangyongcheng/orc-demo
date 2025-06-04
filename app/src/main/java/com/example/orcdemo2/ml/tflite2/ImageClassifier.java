package com.example.orcdemo2.ml.tflite2;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ImageClassifier {
    private static final String TAG = "ImageClassifier";
    private static final String MODEL_NAME = "bert_ner5.tflite";
    private static int IMAGE_WIDTH = 224; // Change to your model's input size
    private static int IMAGE_HEIGHT = 224; // Change to your model's input size
    private static int CHANNELS = 3;       // Typically 3 for RGB, 1 for grayscale

    // Normalization parameters (change according to your model)
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 255.0f;

    private final Context context;
    private final Interpreter interpreter;
    private final List<String> labels;

    public ImageClassifier(Context context) throws IOException {
        this.context = context;

        // Initialize interpreter with GPU delegate if available
        Interpreter.Options options = new Interpreter.Options();
        try {
            // Add GPU delegate if supported (optional)
            // GpuDelegate delegate = new GpuDelegate();
            // options.addDelegate(delegate);

            interpreter = new Interpreter(FileUtil.loadMappedFile(context, MODEL_NAME), options);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TFLite Interpreter", e);
            throw new IOException("Failed to initialize TFLite Interpreter", e);
        }

        // Load labels
        try {
            labels = FileUtil.loadLabels(context, "vocab5.txt");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load labels", e);
            throw new IOException("Failed to load labels", e);
        }
    }

    public String classify(Bitmap bitmap) {
        // Convert bitmap to ARGB_8888 if needed
        Bitmap argbBitmap = bitmap.getConfig() == Bitmap.Config.ARGB_8888
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, false);

        try {
            // Get model input shape
//            int[] inputShape = interpreter.getInputTensor(0).shape();
//            IMAGE_WIDTH = inputShape[1];
//            IMAGE_HEIGHT = inputShape[2];
//            CHANNELS = inputShape[3];

            // Preprocess the image
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(IMAGE_WIDTH, IMAGE_HEIGHT, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                    .build();

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(argbBitmap);
            tensorImage = imageProcessor.process(tensorImage);

            // Verify input tensor size matches model expectations
            ByteBuffer inputBuffer = tensorImage.getBuffer();
            int expectedSize = 1 * IMAGE_WIDTH * IMAGE_HEIGHT * CHANNELS * Float.BYTES;
            if (inputBuffer.remaining() != expectedSize) {
                throw new IllegalArgumentException(
                        String.format("Input buffer size mismatch. Expected %d bytes, got %d bytes",
                                expectedSize, inputBuffer.remaining()));
            }

            // Prepare output buffer
            TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(
                    new int[]{1, labels.size()}, DataType.FLOAT32);

            // Run inference
            interpreter.run(inputBuffer, probabilityBuffer.getBuffer());

            // Process results
            Map<String, Float> labeledProbability = new TensorLabel(labels,
                    probabilityBuffer).getMapWithFloatValue();

            return Collections.max(labeledProbability.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
        } finally {
            if (argbBitmap != bitmap) {
                argbBitmap.recycle();
            }
        }
    }

    public String classify2(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Null bitmap provided");
            return "Error: Null image";
        }

        // Convert bitmap to ARGB_8888 if needed
        Bitmap argbBitmap = bitmap.getConfig() == Bitmap.Config.ARGB_8888
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, false);

        if (argbBitmap == null) {
            Log.e(TAG, "Failed to convert bitmap to ARGB_8888");
            return "Error: Image conversion failed";
        }

        try {
            // Preprocess the image
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(IMAGE_WIDTH, IMAGE_HEIGHT, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                    .build();

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(argbBitmap);
            tensorImage = imageProcessor.process(tensorImage);

            // Run inference
            TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(
                    new int[]{1, labels.size()}, DataType.FLOAT32);

            try {
                interpreter.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());
            } catch (Exception e) {
                Log.e(TAG, "Inference error", e);
                return "Error: Inference failed";
            }

            // Get the results
            Map<String, Float> labeledProbability = new TensorLabel(labels,
                    probabilityBuffer).getMapWithFloatValue();

            if (labeledProbability.isEmpty()) {
                return "No predictions";
            }

            return Collections.max(labeledProbability.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
        } finally {
            // Clean up if we created a new bitmap
            if (argbBitmap != bitmap) {
                argbBitmap.recycle();
            }
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}