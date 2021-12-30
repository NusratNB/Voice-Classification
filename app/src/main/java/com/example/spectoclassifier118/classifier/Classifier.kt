package com.example.spectoclassifier118.classifier

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.example.spectoclassifier118.ml.TCResNet14SE
import com.example.spectoclassifier118.viewmodel.Recognition
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
//import org.tensorflow.lite.gpu.CompatibilityList
import java.nio.ByteBuffer
import java.nio.ByteOrder


typealias RecognitionListener = (recognition: List<Recognition>) -> Unit
private const val MAX_RESULT_DISPLAY = 3
private const val TAG = "TFL Classify" // Name for logging
//private lateinit var rotationMatrix: Matrix
private var width:Int = 224
private var height:Int = 224
lateinit var tfImagefromBitmap: TensorImage
var inferenceTime: Float = 0.0f

class Classifier(ctx: Context) {

    private val model = TCResNet14SE.newInstance(ctx)

    //    private val model: EfficientB0 by lazy{
//
//        // TODO 6. Optional GPU acceleration
//        val compatList = CompatibilityList()
//
//        val options = if(compatList.isDelegateSupportedOnThisDevice) {
//            Log.d(TAG, "This device is GPU Compatible ")
//            Model.Options.Builder().setDevice(Model.Device.GPU).build()
//        } else {
//            Log.d(TAG, "This device is GPU Incompatible ")
//            Model.Options.Builder().setNumThreads(4).build()
//        }
//
//        // Initialize the Flower Model
//        EfficientB0.newInstance(ctx, options)
//    }
    fun analyze(data: Array<FloatArray>): FloatArray {

        val items = mutableListOf<Recognition>()
//
//         var imageProcessor = ImageProcessor.Builder()
//             .add(NormalizeOp(127.5f, 127.5f))
//             .build()


//         val resBtm: Bitmap = Bitmap.createScaledBitmap(
//             bitmap,
//             224,
//             224,
//             true
//         )
//        var startTime = System.currentTimeMillis()

//         val newbtm = AlphaToBlack(resBtm)
//    var endTime = System.currentTimeMillis()
//    inferenceTime = (endTime - startTime).toFloat()

        // TODO 2: Convert Image to Bitmap then to TensorImage
//         tfImagefromBitmap = TensorImage.fromBitmap(resBtm)
//         val tfImage: TensorImage = TensorImage.createFrom(tfImagefromBitmap, DataType.FLOAT32)
//
//         val normalized = imageProcessor.process(tfImage);

//         val byteBuffer = normalized.tensorBuffer.buffer


        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * data.size * data[0].size*1)
        byteBuffer.order(ByteOrder.nativeOrder())
        val index = 0
        for (i in 0..data.size-1) {
            for (j in 0..data[0].size-1) {
                byteBuffer.putFloat(data[i][j])
            }
        }

        val audioClip = TensorBuffer.createFixedSize(intArrayOf(1, 101, 40), DataType.FLOAT32)
//         val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        audioClip.loadBuffer(byteBuffer)
//         inputFeature0.loadBuffer(byteBuffer)


//         tfImagefromBitmap = imageProcessor.process(tfImagefromBitmap)

//        var tfImage: TensorImage = TensorImage.createFrom(tfImagefromBitmap, DataType.FLOAT32)
//         Log.(tfImage, "tf image");
        var startTime = System.currentTimeMillis()
        var outputs = model.process(audioClip)
//         var outputs = model.process(inputFeature0)
        var endTime = System.currentTimeMillis()
        inferenceTime = (endTime - startTime).toFloat()


//         val outputFeature0 = outputs.outputFeature0AsTensorBuffer
//         val probability = outputs.probabilityAsCategoryList.apply {
//                sortByDescending { it.score } // Sort with highest confidence first
//            }.take(MAX_RESULT_DISPLAY) // take the top results
        val probability = outputs.probabilityAsTensorBuffer
        //
//
//        for (output in probability) {
//            items.add(Recognition(output.label, output.score))
//        }
//         model.close()
//        val res = probability.dataType.toString()
        return probability.floatArray

    }
    fun getInfTime(): Float {
        return inferenceTime
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


