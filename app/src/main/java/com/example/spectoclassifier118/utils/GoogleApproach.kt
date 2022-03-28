package com.example.spectoclassifier118.utils

import android.annotation.SuppressLint
import android.util.Log
import kotlin.math.floor
import kotlin.properties.Delegates

class RecognitionFilter {

//    private val resultData = data
//    private val nFrames: Int = resultData.size
//    private val so = SmoothOutput(nFrames)
//    private val smoothedData = so.smoothData(resultData)
    private val listOfClasses = listOf("고마워", "보고싶어", "빨리", "사랑해", "싫어", "아파", "짜증나")
    private val numClasses: Int = listOfClasses.size

    lateinit var googleClsName: String
    lateinit var syntiantClsName: String
    lateinit var customClsName: String
    private var customMaxId by Delegates.notNull<Int>()
    var averageThresholdV1 = 0.0
    var averageThresholdV2 = 0.0

//    fun getSmoothedData(data: Array<FloatArray>): Array<FloatArray> {
//        val nFrames = data.size
//        val smOut = SmoothOutput(nFrames)
//        return smOut.smoothData(data)
//    }



    fun googleApproach(res: Array<FloatArray>): Double {
        //TODO 1.Apply Googles Approach here

        val googlePh = FloatArray(numClasses)
        Log.d("nFrames", res.size.toString())
        var nFrames = res.size
        if (nFrames == 0){
            nFrames = 1
        }
        val smOut = SmoothOutput(nFrames)
        val smoothedData = smOut.smoothData(res)
        for (i in smoothedData.indices){
            googlePh[i] = (smoothedData[i].average().toFloat())
            Log.d("smoothedData array $i", smoothedData[i].joinToString(" "))
        }
        Log.d("googlePh size", googlePh.size.toString())
        val googleMaxId =  googlePh.maxOrNull()?.let { it1 -> googlePh.indexOfFirst { it == it1 }}
        Log.d("googlePh array", googlePh.joinToString(" "))
        Log.d("googleMaxId size", googleMaxId.toString())
        var googleClProb = googlePh[googleMaxId!!]*100.0
        googleClProb = String.format("%.2f", googleClProb).toDouble()
        googleClsName = listOfClasses[googleMaxId]

        return googleClProb
    }

    fun takeAverage(res: Array<FloatArray>): Double{
        //TODO 3.Take average

        val nFrames = res.size
        val smOut = SmoothOutput(nFrames)
        val customPh = FloatArray(numClasses)
        val transposedData = smOut.transposeOutput(res.size, res)
        for (i in transposedData.indices){
            customPh[i] = (transposedData[i].average()).toFloat()
        }
        customMaxId =
            customPh.maxOrNull()?.let { it1 -> customPh.indexOfFirst { it == it1 } }!!
        var customClProb = customPh[customMaxId]*100.0
        customClProb = String.format("%.2f", customClProb).toDouble()
        customClsName = listOfClasses[customMaxId]
        return customClProb
    }

    @SuppressLint("LongLogTag")
    fun syntiantApproach(res: Array<FloatArray>): Pair<String, Float>{
        //TODO 2.Apply Syntiant Approach here

        val syntiantPhThresholdV1 = 85.0
        val syntiantPhThresholdV2 = 95.0f
        val consFramesThreshold = 5
        val synNFrames = res.size
        val so = SmoothOutput(synNFrames)
        var countThV1 = 0
        var countThV2 = 0
        var resClass = ""
        var resClassProbability = 0.0f
        val consecutivePh = floor(synNFrames*0.3).toInt()
        val dominantClass = so.transposeOutput(res.size, res)[customMaxId]
        val scores = arrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        val counts = arrayOf(0, 0, 0, 0, 0, 0, 0)
//        val classAboveThreshold: HashMap<Int, Int> = hashMapOf(0 to 0, 1 to 0,
//            2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)

        for (index in res.indices){
//            val isLastFrameAboveThreshold: HashMap<Int, Int> = hashMapOf(0 to 0, 1 to 0,
//                2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)
            val currentFrame = res[index]
            for (i in listOfClasses.indices){
                val currentProb = currentFrame[i]*100.0f
                if (currentProb>=syntiantPhThresholdV2){
                    scores[i] = scores[i] + currentProb
                    counts[i] = counts[i] + 1
//                    isLastFrameAboveThreshold[i] = 1
                }
            }
        }
        val maxVal = counts.maxOrNull() ?: 0
        val maxIdx = counts.indexOf(counts.maxOrNull())
        if (maxVal > 0){
            resClass = listOfClasses[maxIdx]
            resClassProbability = scores[maxIdx]/maxVal.toFloat()
            resClassProbability = String.format("%.2f", resClassProbability).toFloat()
        }

        return Pair(resClass, resClassProbability)
    }

}