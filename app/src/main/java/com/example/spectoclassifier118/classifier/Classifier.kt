package com.example.spectoclassifier118.classifier

import android.content.Context
import android.graphics.Bitmap
import com.example.spectoclassifier118.ml.EfficientB4
import com.example.spectoclassifier118.viewmodel.Recognition
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import android.os.SystemClock




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
     fun analyze(bitmap: Bitmap): MutableList<Recognition> {

         val items = mutableListOf<Recognition>()

         var imageProcessor = ImageProcessor.Builder()
             .add(NormalizeOp(127.0f, 128.0f))
             .build()



         val resBtm: Bitmap = Bitmap.createScaledBitmap(
             bitmap,
             300,
             300,
             true
         )

         val newbtm = AlphaToBlack(resBtm)

         // TODO 2: Convert Image to Bitmap then to TensorImage
         tfImagefromBitmap = TensorImage.fromBitmap(newbtm)
         val tfImage: TensorImage = TensorImage.createFrom(tfImagefromBitmap, DataType.FLOAT32)
         val normalized = imageProcessor.process(tfImage);
         val byteBuffer = normalized.tensorBuffer.buffer
         val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 300, 300, 3), DataType.FLOAT32)
         inputFeature0.loadBuffer(byteBuffer)


//         tfImagefromBitmap = imageProcessor.process(tfImagefromBitmap)

//        var tfImage: TensorImage = TensorImage.createFrom(tfImagefromBitmap, DataType.FLOAT32)
//         Log.(tfImage, "tf image");
         var outputs = model.process(inputFeature0)
//         val outputFeature0 = outputs.outputFeature0AsTensorBuffer
         val probability = outputs.probabilityAsCategoryList.apply {
                sortByDescending { it.score } // Sort with highest confidence first
            }.take(MAX_RESULT_DISPLAY) // take the top results
//
//
        for (output in probability) {
            items.add(Recognition(output.label, output.score))
        }
//         model.close()
         return items

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


