package com.example.spectoclassifier118.classifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.NonNull
import com.example.spectoclassifier118.ml.EfficientB4
import com.example.spectoclassifier118.viewmodel.Recognition
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.nio.ByteBuffer
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.image.ops.ResizeOp





typealias RecognitionListener = (recognition: List<Recognition>) -> Unit
private const val MAX_RESULT_DISPLAY = 3
private const val TAG = "TFL Classify" // Name for logging
//private lateinit var rotationMatrix: Matrix
private var width:Int = 300
private var height:Int = 300
lateinit var tfImagefromBitmap: TensorImage
//lateinit var tfImage: TensorImage

class Classifier(ctx: Context) {

    private val model = EfficientB4.newInstance(ctx)
     fun analyze(bitmap: Bitmap): List<Category> {

         var imageProcessor = ImageProcessor.Builder()
             .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
             .add(NormalizeOp(127.5f, 128.5f))
             .build()





         val newbtm = AlphaToBlack(bitmap)
         // TODO 2: Convert Image to Bitmap then to TensorImage
         tfImagefromBitmap = TensorImage.fromBitmap(newbtm)
         var tfImage: TensorImage = TensorImage.createFrom(tfImagefromBitmap, DataType.FLOAT32)
         var normalized = imageProcessor.process(tfImage);
         var byteBuffer = normalized.getTensorBuffer().getBuffer()
         val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 300, 300, 3), DataType.FLOAT32)
         inputFeature0.loadBuffer(byteBuffer)


         tfImagefromBitmap = imageProcessor.process(tfImagefromBitmap)

//        var tfImage: TensorImage = TensorImage.createFrom(tfImagefromBitmap, DataType.FLOAT32)
//         Log.(tfImage, "tf image");
         var outputs = model.process(inputFeature0)
//         val outputFeature0 = outputs.outputFeature0AsTensorBuffer
         val probability = outputs.probabilityAsCategoryList.apply {
                sortByDescending { it.score } // Sort with highest confidence first
            }.take(MAX_RESULT_DISPLAY) // take the top results
//
//        // TODO 4: Converting the top probability items into a list of recognitions
//        for (output in probability) {
//            items.add(Recognition(output.label, output.score))
//        }
         model.close()
         return probability

    }


    fun AlphaToBlack(image: Bitmap): Bitmap? {
        val rgbImage = image.copy(Bitmap.Config.ARGB_8888, true)
        for (y in 0 until rgbImage.height) {
            for (x in 0 until rgbImage.width) {
                val aPixel = rgbImage.getPixel(x, y)
                if (rgbImage.getPixel(x, y) < -0x1000000) rgbImage.setPixel(x, y, -0x1000000)
            }
        }
        return rgbImage
    }
}


