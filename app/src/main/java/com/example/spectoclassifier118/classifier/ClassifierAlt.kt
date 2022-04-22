package com.example.spectoclassifier118.classifier

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.util.Log
import com.example.spectoclassifier118.ml.ModelNoPrePL
import com.example.spectoclassifier118.spectoimage.LogMelSpecKt
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel



class ClassifierAlt{


    //    lateinit var testSlicedData: Array<FloatArray>
    // this is class
    lateinit var tfLite: Interpreter
    private var inferenceTime: Float = 0.0f
    private val nFFT: Int = 320
    private var numFrames: Int = 0
    private var inputAudioLength: Int = 0
    private var nBatchSize: Int = 1
    private var nPredictions: Int = 1
    private var MODEL_NAME: String = ""
    private lateinit var mfccGenerator: LogMelSpecKt


    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer? {
        val fileDescriptor: AssetFileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getModel(activity: AssetManager, modelPath: String): Interpreter? {
        return loadModelFile(activity, modelPath)?.let { Interpreter(it) }
    }

    fun initModelName(modelName: String) {
        MODEL_NAME = modelName
    }

    fun initAudioLength(AudioLength: Int) {
        inputAudioLength = AudioLength
    }

    fun initBatchSize(nBatch: Int) {
        nBatchSize = nBatch
    }

    private fun handleAudioLength(
        data: FloatArray,
        inAudioLength: Int
    ): Pair<Array<FloatArray>, Int> {
        lateinit var slicedData: Array<FloatArray>


        val currentAudioLength = data.size
        var localNumPredictions = 1
        var localNumFrames = 1

        if (currentAudioLength > inAudioLength) {
            localNumFrames = (currentAudioLength - inAudioLength) / nFFT
            if (localNumFrames == 0) {
                localNumFrames = 1
            }
            slicedData = Array(localNumFrames) { FloatArray(inAudioLength) }
            for (i in 0 until (localNumFrames)) {
                slicedData[i] = data.slice(i * nFFT until inAudioLength + i * nFFT).toFloatArray()
            }
            localNumPredictions = localNumFrames / nBatchSize
        } else if (currentAudioLength == inAudioLength) {
            localNumFrames = 1
            slicedData = Array(localNumFrames) { FloatArray(inAudioLength) }

            for (i in 0 until (localNumFrames)) {
                slicedData[i] = data.slice(i * nFFT until inAudioLength + i * nFFT).toFloatArray()
            }
        } else {
            localNumFrames = 1
            slicedData = Array(localNumFrames) { FloatArray(inAudioLength) }
            val remainedLength = FloatArray(inAudioLength - currentAudioLength) { 0.0f }
            for (i in 0 until (localNumFrames)) {
                slicedData[i] = data + remainedLength
            }
        }

        return Pair(slicedData, localNumFrames)
    }

    fun transpose(data: Array<FloatArray>): Array<FloatArray> {
        val transposedData = Array(data[0].size){FloatArray(data.size)}
        for (i in data.indices){
            for (j in data[0].indices){
                transposedData[j][i] = data[i][j]
            }
        }
        return transposedData

    }

    @SuppressLint("LongLogTag")
    fun makeInference(
        activity: AssetManager,
        data: FloatArray,
        inpAudioLength: Int,
        modName: String
    ): Array<FloatArray> {
        val (slicedData, locNumPredictions) = handleAudioLength(data, inpAudioLength)

        mfccGenerator = LogMelSpecKt()

        val tfLite: Interpreter? = getModel(activity, modName)
        var outputs: Unit? = null
//        tfLite?.resizeInput(0, intArrayOf(nBatchSize, inpAudioLength))

        val output = Array(locNumPredictions) { FloatArray(7) }

        for (index in 0 until locNumPredictions){
            val currentData = slicedData[index]
            val mfccData = mfccGenerator.getMFCC(currentData)

            val outputByteBuffer: ByteBuffer = ByteBuffer.allocate(4 * 7)
            outputByteBuffer.order(ByteOrder.nativeOrder())
            Log.d("locNumPredictions", locNumPredictions.toString())
//            for (i in mfccData.indices){
//                Log.d("mfcc[$i].size", mfccData[i].joinToString(" "))
//            }

            Log.d("mfcc.size", mfccData.size.toString())
            Log.d("mfcc[0].size", mfccData[0].size.toString())
            val transposedMFCCs = transpose(mfccData)


            val inputByteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * transposedMFCCs.size * (transposedMFCCs[0].size))
            inputByteBuffer.order(ByteOrder.nativeOrder())
            for (i in transposedMFCCs.indices){
                for (j in (transposedMFCCs[0].indices)){
                    inputByteBuffer.putFloat(transposedMFCCs[i][j])
                }
            }

            val audioClip =
                TensorBuffer.createFixedSize(intArrayOf(7), DataType.FLOAT32)
            audioClip.loadBuffer(outputByteBuffer)
            val inputData = TensorBuffer.createFixedSize(
                intArrayOf(1, transposedMFCCs[0].size, transposedMFCCs.size),
                DataType.FLOAT32
            )
            inputData.loadBuffer(inputByteBuffer)
            outputs = tfLite?.run(inputData.buffer, audioClip.buffer)
            output[index] = audioClip.floatArray

        }

        tfLite?.close()
        return output

    }

//    fun makeInferenceAlt(
//        ctx: Context, data: FloatArray,
//        inpAudioLength: Int,): FloatArray {
//
//        val model = ModelNoPrePL.newInstance(ctx)
////        var rawData = data?.get(0)
//        val (slicedData, locNumPredictions) = handleAudioLength(data, inpAudioLength)
//
//        mfccGenerator = LogMelSpecKt()
//        val currentData = slicedData[0]
//        val mfccData = mfccGenerator.getMFCC(currentData)
//
////        val audioClip = TensorBuffer.createFixedSize(intArrayOf(1, 100, 32), DataType.FLOAT32)
//        val output = Array(locNumPredictions) { FloatArray(7) }
//        val outputByteBuffer: ByteBuffer = ByteBuffer.allocate(4 * 7)
//        outputByteBuffer.order(ByteOrder.nativeOrder())
//        Log.d("locNumPredictions", locNumPredictions.toString())
////        for (i in mfccData.indices){
////            Log.d("mfcc[$i].size", mfccData[i].joinToString(" "))
////        }
//
////            Log.d("mfcc.size", mfccData.size.toString())
////            Log.d("mfcc[0].size", mfccData[0].joinToString(" "))
//
//
//        val inputByteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * mfccData.size * (mfccData[0].size))
//        inputByteBuffer.order(ByteOrder.nativeOrder())
//        for (i in mfccData.indices){
//            for (j in (mfccData[0].indices)){
//                inputByteBuffer.putFloat(mfccData[i][j])
//            }
//        }
//
//        val audioClip =
//            TensorBuffer.createFixedSize(intArrayOf(7), DataType.FLOAT32)
//        audioClip.loadBuffer(outputByteBuffer)
//        val inputData = TensorBuffer.createFixedSize(
//            intArrayOf(1, mfccData.size, mfccData[0].size),
//            DataType.FLOAT32
//        )
//        inputData.loadBuffer(inputByteBuffer)
//        val outputs = model.process(inputData)
//        val probability = outputs.probabilityAsTensorBuffer
//        model.close()
//        return probability.floatArray
//    }


}