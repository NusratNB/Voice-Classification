package com.example.spectoclassifier118.utils


import kotlin.math.max

class SmoothOutput {
    lateinit var transpose: Array<FloatArray>

    fun transposeOutput(numFrames: Int, data: Array<FloatArray>): Array<FloatArray> {
        val nFrames = data.size
        val nClasses = data[0].size
        transpose = Array(nClasses){FloatArray(nFrames)}
        for (i in 0 until nFrames){
            for (j in 0 until nClasses){
                transpose[j][i] = data[i][j]
            }
        }

        return transpose
    }

    fun smoothData(data: Array<FloatArray>): Array<FloatArray> {
        val nFrames = data.size
        val nClasses = data[0].size
        val transposedData = transposeOutput(nFrames, data)
        val wSmooth = 10
        val wMax = 100
        val fullSmoothed = Array(nClasses){FloatArray(nFrames)}
        for(i in transposedData.indices){
            val classSmoothed = FloatArray(nFrames)
            val classes: FloatArray = transposedData[i]
            for (k in classes.indices){
                if (k>=0){
                    val j = k+1
                    val probs = classes.copyOfRange(0, j)
                    val pHat = smoothOutput(j, probs, wMax, wSmooth)
                    classSmoothed[k] = pHat
                }
            }
            fullSmoothed[i] = classSmoothed
        }
        return fullSmoothed
    }

    private fun smoothOutput(j: Int, probs: FloatArray, wMax: Int, wSmooth: Int): Float {
        val hSmooth = max(1, (j-wSmooth+1))
        val sum = summation(hSmooth, probs, j)
        val smoothFactor = (1.0/(j.toFloat()-hSmooth.toFloat()+1.0)).toFloat()
        val pHat = smoothFactor*sum
//        Log.d("This is pHat", pHat.toString())
//        Log.d("This is hSmooth", hSmooth.toString())
//        Log.d("This is summation", sum.toString())
//        Log.d("This is j ", j.toString())
//        Log.d("This is smoothFactor ", smoothFactor.toString())
        return pHat
    }

    private fun summation(hSmooth: Int, probs: FloatArray, j: Int): Float {
        return probs.copyOfRange(hSmooth, j).sum()
    }
}