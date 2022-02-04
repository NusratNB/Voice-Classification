package com.example.spectoclassifier118.classifier

import android.content.Context
import android.util.Log
import com.example.spectoclassifier118.ml.TCResNet14SE
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder



var inferenceTime: Float = 0.0f
var numFrames: Int = 0
const val inputAudioLength: Int = 16240 // 1.015 seconds
const val nFFT: Int = 160// 400 // 24 milliseconds

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
    fun analyze(data: FloatArray): FloatArray {

        val slicedData = handleAudioLength(data)
        lateinit var probability: TensorBuffer
        var startTime = System.currentTimeMillis()
        for (i in 0 until numFrames){
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 *  inputAudioLength )
            byteBuffer.order(ByteOrder.nativeOrder())
//            for (i in slicedData.indices) {
            for (j in slicedData[i].indices){
                byteBuffer.putFloat(slicedData[i][j])
            }
//            }

            val audioClip = TensorBuffer.createFixedSize(intArrayOf(1, inputAudioLength), DataType.FLOAT32)
            audioClip.loadBuffer(byteBuffer)

            var outputs = model.process(audioClip)

            probability = outputs.probabilityAsTensorBuffer
            for (k in probability.floatArray.indices){
                Log.d("Model's Output + $k", probability.floatArray[k].toString())
            }

        }

//        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 *  inputAudioLength *numFrames )
//        byteBuffer.order(ByteOrder.nativeOrder())
//        for (i in slicedData.indices) {
//            for (j in slicedData[i].indices){
//                byteBuffer.putFloat(slicedData[i][j])
//            }
//        }
//        val audioClip = TensorBuffer.createFixedSize(intArrayOf(numFrames, inputAudioLength), DataType.FLOAT32)
//        audioClip.loadBuffer(byteBuffer)
//
//        val outputs = model.process(audioClip)
//        probability = outputs.probabilityAsTensorBuffer


        val endTime = System.currentTimeMillis()
        inferenceTime = (endTime - startTime).toFloat()
        numFrames = 0
//        model.close()


//        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * inputAudioLength )

        return probability.floatArray

    }


    private fun handleAudioLength(data: FloatArray): Array<FloatArray> {
        val resultData = null
        lateinit var resultArray: FloatArray
        lateinit var slicedData: Array<FloatArray>
        val currentAudioLength = data.size
        if (currentAudioLength> inputAudioLength){
            numFrames = (currentAudioLength - inputAudioLength) / nFFT
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
            Log.d("Size of currentAudio", currentAudioLength.toString())
            Log.d("This is size slicedData", slicedData.size.toString())
            Log.d("This is size slicedData", slicedData[0].size.toString())
            for (i in 0 until (numFrames)){
                slicedData[i] = data.slice(i*nFFT until inputAudioLength + i*nFFT).toFloatArray()
            }
        }else if (currentAudioLength == inputAudioLength){
            numFrames = 1
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
            Log.d("Size of currentAudio", currentAudioLength.toString())
            Log.d("This is size slicedData", slicedData.size.toString())
            Log.d("This is size slicedData", slicedData[0].size.toString())
            for (i in 0 until (numFrames)){
                slicedData[i] = data.slice(i*nFFT until inputAudioLength + i*nFFT).toFloatArray()
            }
        } else{
            numFrames = 1
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
            Log.d("Size of currentAudio", currentAudioLength.toString())
            Log.d("This is size slicedData", slicedData.size.toString())
            Log.d("This is size slicedData", slicedData[0].size.toString())
            val remainedLength = FloatArray(inputAudioLength-currentAudioLength){0.0f}
            for (i in 0 until (numFrames)){
                slicedData[i] = data + remainedLength
            }
        }



//        resultArray = when {
//            currentAudioLength < inputAudioLength -> {
//                val remainedLength = FloatArray(inputAudioLength-currentAudioLength){0.0f}
//                data + remainedLength
//            }
//            currentAudioLength > inputAudioLength -> {
//                data.copyOfRange(0, inputAudioLength)
//            }
//            else -> {
//                data
//            }
//        }
        return slicedData
    }
}


