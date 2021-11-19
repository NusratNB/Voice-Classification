package com.example.spectoclassifier118.classifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.example.spectoclassifier118.ml.EfficientB4
import com.example.spectoclassifier118.viewmodel.Recognition
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.nio.ByteBuffer



typealias RecognitionListener = (recognition: List<Recognition>) -> Unit
private const val MAX_RESULT_DISPLAY = 3
private const val TAG = "TFL Classify" // Name for logging
//private lateinit var rotationMatrix: Matrix
private var width:Int = 300
private var height:Int = 300

class Classifier(ctx: Context) {

    private val model = EfficientB4.newInstance(ctx)

    fun floatArrayToGrayscaleBitmap (
        floatArray: FloatArray,
        width: Int,
        height: Int,
        alpha :Byte = (255).toByte(),
        reverseScale :Boolean = false
    ) : Bitmap {

        // Create empty bitmap in RGBA format (even though it says ARGB but channels are RGBA)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val byteBuffer = ByteBuffer.allocate(width*height*4)

        // mapping smallest value to 0 and largest value to 255
        val maxValue = floatArray.maxOrNull() ?: 1.0f
        val minValue = floatArray.minOrNull() ?: 0.0f
        val delta = maxValue-minValue
        var tempValue :Byte

        // Define if float min..max will be mapped to 0..255 or 255..0
        val conversion = when(reverseScale) {
            false -> { v: Float -> ((v-minValue)/delta*255).toInt().toByte() }
            true -> { v: Float -> (255-(v-minValue)/delta*255).toInt().toByte() }
        }

        // copy each value from float array to RGB channels and set alpha channel
        floatArray.forEachIndexed { i, value ->
            tempValue = conversion(value)
            byteBuffer.put(4*i, tempValue)
            byteBuffer.put(4*i+1, tempValue)
            byteBuffer.put(4*i+2, tempValue)
            byteBuffer.put(4*i+3, alpha)
        }

        bmp.copyPixelsFromBuffer(byteBuffer)

        return bmp
    }

     fun analyze(bitmap: Bitmap): MutableList<Recognition> {
         var resizedBitmap: Bitmap = resizeBitmap(bitmap, width, height)

        val items = mutableListOf<Recognition>()

        // TODO 2: Convert Image to Bitmap then to TensorImage
        var tfImage = TensorImage.fromBitmap(resizedBitmap)

//         Log.(tfImage, "tf image");

        // TODO 3: Process the image using the trained model, sort and pick out the top results
        val outputs = model.process(tfImage)
            .probabilityAsCategoryList.apply {
                sortByDescending { it.score } // Sort with highest confidence first
            }.take(MAX_RESULT_DISPLAY) // take the top results

        // TODO 4: Converting the top probability items into a list of recognitions
        for (output in outputs) {
            items.add(Recognition(output.label, output.score))
        }

         return items
    }

     fun resizeBitmap(bmt:Bitmap, width: Int, height: Int):Bitmap{

        return Bitmap.createScaledBitmap(
            bmt,
            width,
            height,
            false
        )
    }




}