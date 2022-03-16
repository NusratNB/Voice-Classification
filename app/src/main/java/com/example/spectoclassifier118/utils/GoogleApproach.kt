package com.example.spectoclassifier118.utils

import kotlin.math.floor

class RecognitionFilter(data: Array<FloatArray>) {

    private val resultData = data
    private val nFrames: Int = resultData.size
    private val so = SmoothOutput(nFrames)
    private val smoothedData = so.smoothData(resultData)
    private val listOfClasses = listOf("고마워", "보고싶어", "빨리", "사랑해", "싫어", "아파", "짜증나")
    private val numClasses: Int = listOfClasses.size

    private lateinit var



    fun googleApproach(): Double {
        //TODO 1.Apply Googles Approach here

        val googlePh = FloatArray(numClasses)
        for (i in smoothedData.indices){
            googlePh[i] = (smoothedData[i].average().toFloat())
        }
        val googleMaxId =  googlePh.maxOrNull()?.let { it1 -> googlePh.indexOfFirst { it == it1 }}
        var googleClProb = googlePh[googleMaxId!!]*100.0
        googleClProb = String.format("%.2f", googleClProb).toDouble()
        val className: String = listOfClasses[googleMaxId]

        return googleClProb
    }

    fun syntiantApproach(){
        //TODO 2.Apply Syntiant Approach here

        val syntiantPhThresholdV1 = 85.0
        val syntiantPhThresholdV2 = 70.0
        var averageThresholdV1 = 0.0
        var averageThresholdV2 = 0.0
        var countThV1 = 0
        var countThV2 = 0
        val consecutivePh = floor(nFrames*0.3).toInt()




    }

    fun takeAverage(){
        //TODO 3.Take average

        val customPh = FloatArray(numClasses)
        val transposedData = so.transposeOutput(resultData.size, resultData)
    }


}