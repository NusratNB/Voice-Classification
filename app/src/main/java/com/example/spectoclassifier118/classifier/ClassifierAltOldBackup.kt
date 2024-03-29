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


class ClassifierAltOldBackup(ctx: Context, activity: AssetManager){

    private val MODEL_PATH: String = "modeltfNightly.49.tflite"
    lateinit var testSlicedData: Array<FloatArray>
    private var inferenceTime: Float = 0.0f
    private var numFrames: Int = 0
    private val inputAudioLength: Int = 16240 // 1.015 seconds
    private val nFFT: Int = 160// 400 // 24 milliseconds
    private val nBatchSize: Int =16
    private var nPredictions: Int = 1

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

    @SuppressLint("LongLogTag")
    fun makeInference(data: FloatArray): Array<FloatArray> {
        val slicedData = handleAudioLength(data)
        tfLite?.resizeInput(0, intArrayOf(nBatchSize, inputAudioLength))
        nPredictions = numFrames/nBatchSize
        Log.d("batchedData nPredictions", nPredictions.toString())
        Log.d("batchedData numFrames", numFrames.toString())

        lateinit var probability: TensorBuffer
        var startTime = System.currentTimeMillis()
        val finalResult = Array(numFrames){FloatArray(11)}
        var outputs: Unit? = null
        val batchedData = Array(nPredictions){Array(11){FloatArray(inputAudioLength)} }

        for (i in 0 until nPredictions){
            batchedData[i] = slicedData.slice(i*nBatchSize until (i+1)*nBatchSize).toTypedArray()
        }
        val batchedOutput = Array(nPredictions){Array(nBatchSize){FloatArray(11)} }

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
            val outputByteBuffer: ByteBuffer = ByteBuffer.allocate(nBatchSize*4*11)
            outputByteBuffer.order(ByteOrder.nativeOrder())
    //            }
            val audioClip = TensorBuffer.createFixedSize(intArrayOf(nBatchSize, 11), DataType.FLOAT32)
            audioClip.loadBuffer(outputByteBuffer)
            val inputData = TensorBuffer.createFixedSize(intArrayOf(nBatchSize, inputAudioLength), DataType.FLOAT32)
            inputData.loadBuffer(byteBuffer)


            outputs = tfLite?.run(inputData.buffer, audioClip.buffer)
            Log.d("Outputs ", audioClip.floatArray.size.toString())
            Log.d("sliced data size", slicedData.size.toString())
            val sliceOutput = Array(nBatchSize){FloatArray(11)}
            for (bb in audioClip.floatArray.indices){
                Log.d("FloatOut", audioClip.floatArray[bb].toString())
            }
            for(i in 0 until nBatchSize) {
                sliceOutput[i] =
                    audioClip.floatArray.slice(i * 11 until (i + 1) * 11).toFloatArray()
            }
            batchedOutput[s] = sliceOutput

        for (k in audioClip.floatArray.indices){
            val kk = audioClip.floatArray[k]
            Log.d("audioClip elements $k", kk.toString())
        }
        }
        var indOut = 0
        val fullOut = Array(nPredictions*nBatchSize){FloatArray(11)}
        var ddd = 0
        for (i in 0 until nPredictions){
            for (j in 0 until nBatchSize){
                fullOut[indOut]=batchedOutput[i][j]
                for (k in 0 until 11){
                    Log.d("batchedDataa $ddd", batchedOutput[i][j][k].toString())
                    ddd += 1
                }

            }
        }
        for (ss in fullOut.indices){
            var indd = fullOut[ss]
            for (kk in indd.indices){
                Log.d("fullOut $ss", indd[kk].toString())
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
        numFrames = 1
//        model.close()

        return fullOut

    }

    private fun handleAudioLength(data: FloatArray): Array<FloatArray> {
        lateinit var slicedData: Array<FloatArray>

        val currentAudioLength = data.size

        if (currentAudioLength> inputAudioLength){
            numFrames = (currentAudioLength - inputAudioLength) / nFFT
            slicedData = Array(numFrames){FloatArray(inputAudioLength)}
            Log.d("handleAudio: numFrames", numFrames.toString())
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

}
