package com.example.spectoclassifier118.classifier

import android.annotation.SuppressLint
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


class CoroutinesHandler(ctx: Context, activity: AssetManager){


//    lateinit var testSlicedData: Array<FloatArray>
    lateinit var tfLite: Interpreter
    private var inferenceTime: Float = 0.0f
    private val nFFT: Int = 320
    private var numFrames: Int = 0
    private var inputAudioLength: Int = 0
    private var nBatchSize: Int = 1
    private var nPredictions: Int = 1
    private var MODEL_NAME: String = ""


    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer? {
        val fileDescriptor: AssetFileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getModel(activity: AssetManager,modelPath: String): Interpreter? {
        return loadModelFile(activity, modelPath)?.let { Interpreter(it) }
    }

    fun initModelName(modelName: String){
        MODEL_NAME = modelName
    }

    fun initAudioLength(AudioLength: Int){
        inputAudioLength = AudioLength
    }

    fun initBatchSize(nBatch: Int){
        nBatchSize = nBatch
    }

    private fun handleAudioLength(data: FloatArray, inAudioLength: Int): Pair<Array<FloatArray>, Int> {
        lateinit var slicedData: Array<FloatArray>

        val currentAudioLength = data.size
        var localNumPredictions = 1

        if(currentAudioLength > inAudioLength){
            var localNumFrames = (currentAudioLength - inAudioLength) / nFFT
            if (localNumFrames == 0){
                localNumFrames =1
            }
            slicedData = Array(localNumFrames){FloatArray(inAudioLength)}
            for (i in 0 until (localNumFrames)){
                slicedData[i] = data.slice(i*nFFT until inAudioLength + i*nFFT).toFloatArray()
            }
            localNumPredictions = localNumFrames/nBatchSize
        }else if (currentAudioLength == inAudioLength){
            val localNumFrames = 1
            slicedData = Array(localNumFrames){FloatArray(inAudioLength)}

            for (i in 0 until (localNumFrames)){
                slicedData[i] = data.slice(i*nFFT until inAudioLength + i*nFFT).toFloatArray()
            }
        } else{
            val localNumFrames = 1
            slicedData = Array(localNumFrames){FloatArray(inAudioLength)}
            val remainedLength = FloatArray(inAudioLength-currentAudioLength){0.0f}
            for (i in 0 until (localNumFrames)){
                slicedData[i] = data + remainedLength
            }
        }

        return Pair(slicedData, localNumPredictions)
    }

    @SuppressLint("LongLogTag")
    fun makeInference(activity: AssetManager,data: FloatArray, inpAudioLength: Int, modName: String): Array<FloatArray> {
        val (slicedData, locNumPredictions) = handleAudioLength(data, inpAudioLength)

        val tfLite: Interpreter? = getModel(activity, modName)
        tfLite?.resizeInput(0, intArrayOf(nBatchSize, inpAudioLength))



        val startTime = System.currentTimeMillis()
        var outputs: Unit? = null
        val batchedData = Array(locNumPredictions){Array(7){FloatArray(inpAudioLength)} }

        for (i in 0 until locNumPredictions){
            batchedData[i] = slicedData.slice(i*nBatchSize until (i+1)*nBatchSize).toTypedArray()
        }
        val batchedOutput = Array(locNumPredictions){Array(nBatchSize){FloatArray(7)} }

        for (s in 0 until locNumPredictions){
            val testSlicedData = batchedData[s] //Array(nBatchSize){ FloatArray(inputAudioLength) }
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(nBatchSize*4*inpAudioLength )
            byteBuffer.order(ByteOrder.nativeOrder())
            //            for (i in slicedData.indices) {
            for (i in 0 until nBatchSize) {
                for (j in testSlicedData[i].indices) {
                    byteBuffer.putFloat(testSlicedData[i][j])
                }
            }
            val outputByteBuffer: ByteBuffer = ByteBuffer.allocate(nBatchSize*4*7)
            outputByteBuffer.order(ByteOrder.nativeOrder())
            //            }
            val audioClip = TensorBuffer.createFixedSize(intArrayOf(nBatchSize, 7), DataType.FLOAT32)
            audioClip.loadBuffer(outputByteBuffer)
            val inputData = TensorBuffer.createFixedSize(intArrayOf(nBatchSize, inpAudioLength), DataType.FLOAT32)
            inputData.loadBuffer(byteBuffer)


            outputs = tfLite?.run(inputData.buffer, audioClip.buffer)

            val sliceOutput = Array(nBatchSize){FloatArray(7)}
            for(i in 0 until nBatchSize) {
                sliceOutput[i] =
                    audioClip.floatArray.slice(i * 7 until (i + 1) * 7).toFloatArray()
            }

            batchedOutput[s] = sliceOutput
        }
        var indOut = 0
        val fullOut = Array(locNumPredictions*nBatchSize){FloatArray(7)}
        for (i in 0 until locNumPredictions){
            for (j in 0 until nBatchSize){
                fullOut[indOut]=batchedOutput[i][j]
                Log.d("$modName value $indOut", fullOut[indOut].joinToString(" "))
                indOut += 1
            }
        }

        val endTime = System.currentTimeMillis()
        inferenceTime = (endTime - startTime).toFloat()
//        numFrames = 1
        tfLite?.close()
        return fullOut

    }


}