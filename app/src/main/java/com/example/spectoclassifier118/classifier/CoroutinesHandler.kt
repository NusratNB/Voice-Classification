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


    lateinit var testSlicedData: Array<FloatArray>
    lateinit var tfLite: Interpreter
    private var inferenceTime: Float = 0.0f
    private val nFFT: Int = 160
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

    private fun handleAudioLength(data: FloatArray): Array<FloatArray> {
        lateinit var slicedData: Array<FloatArray>

        val currentAudioLength = data.size

        if(currentAudioLength > inputAudioLength){
            numFrames = (currentAudioLength - inputAudioLength) / nFFT
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
            Log.d("handleAudio: numFrames", numFrames.toString())
            Log.d("handleAudio: inputAudioLength", inputAudioLength.toString())
            Log.d("handleAudio: currentAudioLength", currentAudioLength.toString())
            for (i in 0 until (numFrames)){
                slicedData[i] = data.slice(i*nFFT until inputAudioLength + i*nFFT).toFloatArray()
            }

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

    @SuppressLint("LongLogTag")
    fun makeInference(activity: AssetManager,data: FloatArray): Array<FloatArray> {
        val slicedData = handleAudioLength(data)
        val tfLite: Interpreter? = getModel(activity, MODEL_NAME)
        tfLite?.resizeInput(0, intArrayOf(nBatchSize, inputAudioLength))
        nPredictions = numFrames/nBatchSize


        val startTime = System.currentTimeMillis()
        var outputs: Unit? = null
        val batchedData = Array(nPredictions){Array(7){FloatArray(inputAudioLength)} }

        for (i in 0 until nPredictions){
            batchedData[i] = slicedData.slice(i*nBatchSize until (i+1)*nBatchSize).toTypedArray()
        }
        val batchedOutput = Array(nPredictions){Array(nBatchSize){FloatArray(7)} }

        for (s in 0 until nPredictions){
            testSlicedData = batchedData[s] //Array(nBatchSize){ FloatArray(inputAudioLength) }
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(nBatchSize*4*inputAudioLength )
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
            val inputData = TensorBuffer.createFixedSize(intArrayOf(nBatchSize, inputAudioLength), DataType.FLOAT32)
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
        val fullOut = Array(nPredictions*nBatchSize){FloatArray(7)}
        for (i in 0 until nPredictions){
            for (j in 0 until nBatchSize){
                fullOut[indOut]=batchedOutput[i][j]
                indOut += 1
            }
        }

        val endTime = System.currentTimeMillis()
        inferenceTime = (endTime - startTime).toFloat()
        numFrames = 1
        tfLite?.close()
        return fullOut

    }


}