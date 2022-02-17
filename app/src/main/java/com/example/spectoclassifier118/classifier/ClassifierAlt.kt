package com.example.spectoclassifier118.classifier

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel





class ClassifierAlt(ctx: Context, activity: AssetManager){

    private val MODEL_PATH: String = "model_12.tflite"
    var inferenceTime: Float = 0.0f
    var numFrames: Int = 0
    val inputAudioLength: Int = 16240 // 1.015 seconds
    val nFFT: Int = 160// 400 // 24 milliseconds

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer? {
        val fileDescriptor: AssetFileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    private val tfLite: Interpreter? = loadModelFile(activity, MODEL_PATH)?.let { Interpreter(it) }

    fun makeInference(data: FloatArray): Array<FloatArray> {

        val slicedData = handleAudioLength(data)
        lateinit var probability: TensorBuffer
        var startTime = System.currentTimeMillis()
        val finalResult = Array(numFrames){FloatArray(11)}

        for (i in 0 until numFrames){
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 *  inputAudioLength )
            byteBuffer.order(ByteOrder.nativeOrder())
//            for (i in slicedData.indices) {
            for (j in slicedData[i].indices){
                byteBuffer.putFloat(slicedData[i][j])
            }
            val outputByteBuffer: ByteBuffer = ByteBuffer.allocate(4*11)
//            }

            val audioClip = TensorBuffer.createFixedSize(intArrayOf(1, inputAudioLength), DataType.FLOAT32)
            audioClip.loadBuffer(byteBuffer)


//            val outputs = model.process(audioClip)
            val outputs = tfLite?.run(byteBuffer, outputByteBuffer)
//            val buffer = ByteBuffer.wrap(outputs)
//            for (k in probability.floatArray.indices){
//                Log.d("Model's Output + $k", probability.floatArray[k].toString())
//            }
            Log.e("Outputs", outputs.toString())

//            finalResult[i] = outputs
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
        numFrames = 1
//        model.close()


//        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * inputAudioLength )

        return finalResult

    }

    private fun handleAudioLength(data: FloatArray): Array<FloatArray> {
        val resultData = null
        lateinit var resultArray: FloatArray
        lateinit var slicedData: Array<FloatArray>
        val currentAudioLength = data.size
        if (currentAudioLength> inputAudioLength){
            numFrames = (currentAudioLength - inputAudioLength) / nFFT
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
//            Log.d("Size of currentAudio", currentAudioLength.toString())
//            Log.d("This is size slicedData", slicedData.size.toString())
//            Log.d("This is size slicedData", slicedData[0].size.toString())
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
