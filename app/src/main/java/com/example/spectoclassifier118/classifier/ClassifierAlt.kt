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
import java.util.*


class ClassifierAlt(ctx: Context, activity: AssetManager){

    private val MODEL_PATH: String = "modeltfNightly.49.tflite"
    lateinit var testSlicedData: Array<FloatArray>
    var inferenceTime: Float = 0.0f
    var numFrames: Int = 0
    val inputAudioLength: Int = 16240 // 1.015 seconds
    val nFFT: Int = 160// 400 // 24 milliseconds
    val nBatchSize: Int =1

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

    fun makeInference(data: FloatArray): Unit? {

        val slicedData = handleAudioLength(data)
        lateinit var probability: TensorBuffer
        var startTime = System.currentTimeMillis()
        val finalResult = Array(numFrames){FloatArray(11)}
        var outputs: Unit? = null

//        for (i in 0 until 4){
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(2*4*inputAudioLength )
            byteBuffer.order(ByteOrder.nativeOrder())
    //            for (i in slicedData.indices) {
            for (i in 0 until 2) {
                for (j in testSlicedData[i].indices) {
                    byteBuffer.putFloat(testSlicedData[i][j])
                }
            }
            val outputByteBuffer: ByteBuffer = ByteBuffer.allocate(2*4*11)
            outputByteBuffer.order(ByteOrder.nativeOrder())
    //            }
            val audioClip = TensorBuffer.createFixedSize(intArrayOf(2, 11), DataType.FLOAT32)
            audioClip.loadBuffer(outputByteBuffer)
            val inputData = TensorBuffer.createFixedSize(intArrayOf(2, inputAudioLength), DataType.FLOAT32)
            inputData.loadBuffer(byteBuffer)


    //            val outputs = model.process(audioClip)
//            val newShape: IntArray = IntArray([4,16240])
            tfLite?.resizeInput(0, intArrayOf(2, inputAudioLength))
            outputs = tfLite?.run(inputData.buffer, audioClip.buffer)
    //            val buffer = ByteBuffer.wrap(outputs)
    //            for (k in probability.floatArray.indices){
    //                Log.d("Model's Output + $k", probability.floatArray[k].toString())
    //            }
            Log.d("Outputs ", audioClip.floatArray.size.toString())
            Log.d("sliced data size", slicedData.size.toString())
        for (k in audioClip.floatArray.indices){
            val kk = audioClip.floatArray[k]
            Log.d("audioClip elements $k", kk.toString())
        }

    //            finalResult[i] = outputs
//        }

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

        return outputs

    }

    private fun handleAudioLength(data: FloatArray): Array<FloatArray> {
        val resultData = null
        lateinit var resultArray: FloatArray
        lateinit var slicedData: Array<FloatArray>

        val currentAudioLength = data.size
        if (currentAudioLength> inputAudioLength){
            numFrames = (currentAudioLength - inputAudioLength) / nFFT
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
            testSlicedData = Array(2){ FloatArray(inputAudioLength) }
            for (i in 0 until (numFrames)){
                slicedData[i] = data.slice(i*nFFT until inputAudioLength + i*nFFT).toFloatArray()
            }
            testSlicedData[0] = slicedData[0]
            testSlicedData[1] = slicedData[1]

        }else if (currentAudioLength == inputAudioLength){
            numFrames = 1
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}

            for (i in 0 until (numFrames)){
                slicedData[i] = data.slice(i*nFFT until inputAudioLength + i*nFFT).toFloatArray()
            }
        } else{
            numFrames = 1
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
            val remainedLength = FloatArray(inputAudioLength-currentAudioLength){0.0f}
            for (i in 0 until (numFrames)){
                slicedData[i] = data + remainedLength
            }
        }

        return slicedData
    }

}
