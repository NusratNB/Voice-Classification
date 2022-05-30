package com.example.spectoclassifier118.classifier


import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.support.common.FileUtil

class SoundClassification(ctx: Context) {

    private val modelName = "float_model_08.tflite"
    private val inputAudioLength = 15600 // 0.975 sec
    private val clsNum = 4
    var labelOutput: String = ""
    var tfLite: Interpreter? = null
    private lateinit var labels: List<String>

    init {
        tfLite = getModel(ctx.assets, modelName)
        labels = FileUtil.loadLabels(ctx, "noise_classes_keepin.txt")
    }

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

    private fun handleAudioLength(data: ShortArray): ShortArray{
        lateinit var slicedData: ShortArray
        val currentAudioLength = data.size
        val inputAudioLength = 15600
        val begin = currentAudioLength -inputAudioLength
        slicedData = data.slice(begin until inputAudioLength).toShortArray()
        return slicedData

    }



    fun makeInference(data: ShortArray): FloatArray {

        var outputs: Unit? = null
        val lengthHandledData = handleAudioLength(data)

        val inputByteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * inputAudioLength)
        inputByteBuffer.order(ByteOrder.nativeOrder())

        for (i in lengthHandledData.indices) {
            inputByteBuffer.putFloat((lengthHandledData[i].toFloat()) / 32768f)
        }

        val outputByteBuffer: ByteBuffer = ByteBuffer.allocate(4 * clsNum)
        outputByteBuffer.order(ByteOrder.nativeOrder())

        val audioClip = TensorBuffer.createFixedSize(intArrayOf(clsNum), DataType.FLOAT32)
        audioClip.loadBuffer(outputByteBuffer)
        val inputData =
            TensorBuffer.createFixedSize(intArrayOf(1, inputAudioLength), DataType.FLOAT32)
        inputData.loadBuffer(inputByteBuffer)

        outputs = tfLite?.run(inputData.buffer, audioClip.buffer)
        val outData = audioClip.floatArray
        val maxId = outData.maxOrNull()?.let { it1 -> outData.indexOfFirst { it == it1 } }
        val confidence = outData[maxId!!]
        labelOutput = labels[maxId!!]
//        Log.d("SoundClassifier result", labels[maxId!!])
        return outData
    }

}