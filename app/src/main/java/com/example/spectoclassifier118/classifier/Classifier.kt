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
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


typealias RecognitionListener = (recognition: List<Recognition>) -> Unit
private const val MAX_RESULT_DISPLAY = 3
private const val TAG = "TFL Classify" // Name for logging
//private lateinit var rotationMatrix: Matrix
private var width:Int = 300
private var height:Int = 300
lateinit var tfImagefromBitmap: TensorImage
lateinit var tfImage: TensorImage

class Classifier(ctx: Context) {

    private val model = EfficientB4.newInstance(ctx)
    val flowerModel: EfficientB4 by lazy{

        // TODO 6. Optional GPU acceleration
        val options = Model.Options.Builder().setDevice(Model.Device.GPU).build()

        // Initialize the Flower Model
        EfficientB4.newInstance(ctx, options)
    }

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

     fun analyze(bitmap: Bitmap): List<Category> {
         var resizedBitmap: Bitmap = resizeBitmap(bitmap, width, height)
         val imageProcessor = ImageProcessor.Builder()
             .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
             .build()

        val items = mutableListOf<Recognition>()

//         tfImage.load(bitmap);


         // TODO 2: Convert Image to Bitmap then to TensorImage
         tfImagefromBitmap = TensorImage.fromBitmap(resizedBitmap)

         var tImage = TensorImage(DataType.FLOAT32)
         var tensorImage = tImage.load(resizedBitmap)
//         var byteBuffer = tensorImage.buffer

         var byteBuffer = tfImagefromBitmap.getTensorBuffer().getBuffer()

         val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(4, 1, 300, 300, 3), DataType.FLOAT32)

//         inputFeature0.loadBuffer(byteBuffer)
//         tfImage = imageProcessor.process(tfImagefromBitmap);
         tfImage = TensorImage.createFrom(tfImagefromBitmap, DataType.FLOAT32)
//         Log.(tfImage, "tf image");
         var outputs = model.process(tfImage)
         val probability = outputs.probabilityAsCategoryList
//         val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        // TODO 3: Process the image using the trained model, sort and pick out the top results


//            .probabilityAsCategoryList.apply {
//                sortByDescending { it.score } // Sort with highest confidence first
//            }.take(MAX_RESULT_DISPLAY) // take the top results
//
//        // TODO 4: Converting the top probability items into a list of recognitions
//        for (output in outputs) {
//            items.add(Recognition(output.label, output.score))
//        }
                 model.close()
         return probability

    }

     fun resizeBitmap(bmt:Bitmap, width: Int, height: Int):Bitmap{

        return Bitmap.createScaledBitmap(
            bmt,
            width,
            height,
            false
        )
    }

    fun  getTfImage(): String {
        return tfImage.getDataType().toString()
    }




}