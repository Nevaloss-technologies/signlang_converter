package com.nevaloss.sign_lang_app
import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.channels.FileChannel

object TFLiteHelper {

    private lateinit var tflite: Interpreter

    fun loadModel(context: Context) {
        val fileDescriptor = context.assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        tflite = Interpreter(modelBuffer)
    }

    fun runModel(bitmap: Bitmap): FloatArray {
        // Resize to 96x96 and convert to TensorImage
        val resized = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        val tensorImage = TensorImage.fromBitmap(resized)

        // Optional: normalize image (you can skip NormalizeOp if not required)
        val normalizedImage = TensorImage(tensorImage.dataType)
        normalizedImage.load(resized)
        // normalizedImage = NormalizeOp(0f, 255f).apply(normalizedImage) // if needed

        val inputBuffer = normalizedImage.buffer

        // Output buffer for 26 classes
        val output = Array(1) { FloatArray(26) }

        tflite.run(inputBuffer, output)

        return output[0] // return the 26 float values
    }

    fun close() {
        tflite.close()
    }
}
