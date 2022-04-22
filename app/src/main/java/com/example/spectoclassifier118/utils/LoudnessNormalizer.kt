package com.example.spectoclassifier118.utils


import kotlin.math.log
import kotlin.math.pow
import kotlin.math.sqrt

class LoudnessNormalizer {

    private fun dBFS(audioData: FloatArray): Float {

        val summation = audioData.map { it * it }.sum()

        val rms = sqrt(summation / audioData.size)
        return ratioToDB(rms)
    }

    private fun ratioToDB(rms: Float): Float {

        return 20.0f * log(rms, 10f)
    }

    private fun dbToFloat(db: Float): Float {

        return ((10.0).pow(db.toDouble() / 20.0)).toFloat()
    }

    private fun applyGain(audioData: FloatArray, ratio: Float): List<Float> {
        val ratioFloat = dbToFloat(ratio)

        return audioData.map { it * ratioFloat }
    }

    fun audioNormalization(threshold: Float, audioData: FloatArray): FloatArray {

        val audioInDB = dBFS(audioData)
        val differenceInDb = threshold - audioInDB
        val normAudio = applyGain(audioData,differenceInDb)
        val finalAudio = FloatArray(normAudio.size)
        for(i in normAudio.indices){
            var currentElement = normAudio[i]
            if (currentElement<-1f){
                currentElement = -1f
            } else if (currentElement>(32767f/32768f)){
                currentElement = (32767f/32768f)
            }
            finalAudio[i] = currentElement
        }
        return finalAudio
    }

}