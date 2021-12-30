package com.example.spectoclassifier118.spectoimage

import java.io.IOException
import com.example.mylibrary.FileFormatNotSupportedException
import com.example.mylibrary.LogMelSpec
import com.example.mylibrary.WavFileException
import com.example.mylibrary.JLibrosa


class LogMelSpecKt() {

    lateinit var audioData: FloatArray
    private lateinit var innerPaths: String
    private lateinit var mfcc: Array<FloatArray>
    private val mfccBin = 40
    private val sampleRate = 16000
    private val hopLength = 160
    private val numMelBins = 64
    private val numFFT = 400

    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun main(Path: String): Array<FloatArray> {
        val defaultSampleRate = -1 // -1 value implies the method to use default sample rate
        val defaultAudioDuration = -1 // -1 value implies the method to process complete audio duration
        val jLibrosa = JLibrosa()

        /*
         * To read the magnitude values of audio files - equivalent to
         * librosa.load('../audioFiles/0a2b400e_nohash_2_down.wav', sr=None) function
         */
        val audioFeatureValues =
            jLibrosa.loadAndRead(Path, defaultSampleRate, defaultAudioDuration)
//        LogMelSpec.audioData = audioFeatureValues
        // System.out.println(audioFeatureValues.length);
        //for (int j = 0; j < 10; j++) {
        //    System.out.printf("%.10f%n", audioFeatureValues[j]);
        //}
        val audioFeatureValuesList = jLibrosa.loadAndReadAsList(
            Path, defaultSampleRate,
            defaultAudioDuration
        )
        // System.out.println(audioFeatureValuesList);

        /*
         * yangqin: To add pre-emphasis to audio librosa.effects.preemphasis(y,
         * coef=config_params.preemphasis_coef) function
         */
        val audioPreemphasisValue = FloatArray(audioFeatureValues.size)
        audioPreemphasisValue[0] = audioFeatureValues[0]
        //System.out.println(audioPreemphasisValue[0]);
        for (i in 1 until audioFeatureValues.size) {
            var pre: Double = audioFeatureValues[i] - audioFeatureValues[i - 1] * 0.96875
            audioPreemphasisValue[i] = pre.toFloat()
        }


        val fixedSizeAudio = handleAudioLength(audioPreemphasisValue)

        //for (int j = 0; j < 10; j++) {
        //    System.out.printf("%.10f%n", audioPreemphasisValue[j]);
        //}

        /* To read the no of frames present in audio file */
        //int nNoOfFrames = jLibrosa.getNoOfFrames();

        /* To read sample rate of audio file */
        //int sampleRate = jLibrosa.getSampleRate();

        //float[][] melSpectrogram = jLibrosa.generateMelSpectroGram(audioPreemphasisValue, sampleRate, 512, 40, 160);

        /* yangqin: To get log mel spectrogram */
        //System.out.println(logmelspec.length);
        //System.out.println(logmelspec[0].length);
        //for (int i = 0; i < 1; i++) {
        //    for (int j = 0; j < 10; j++) {
        //        System.out.printf("%.10f%n", logmelspec[i][j]);
        //    }
        //}
        mfcc = jLibrosa.generateMFCCFeatures(fixedSizeAudio, sampleRate, mfccBin, numFFT, numMelBins, hopLength)
        return minMaxScaling(mfcc)
    }


    private fun handleAudioLength(data: FloatArray): FloatArray {
        val dataLength = 16000
        lateinit var resultArray: FloatArray
        val currentAudioLength = data.size
        resultArray = if (currentAudioLength < dataLength){
            val remainedLength = FloatArray(dataLength-currentAudioLength){0.0f}
            data + remainedLength
        }else if (currentAudioLength > dataLength){
            data.copyOfRange(0, dataLength)
        }else{
            data
        }

        return resultArray
    }

    private fun getMax(data: Array<FloatArray>): Float? {

        val maxValue: Float?
        val mutableMax: MutableList<Float> = mutableListOf()

        for (current in data){
            current.maxOrNull()?.let { mutableMax.add(it) }

        }
        maxValue = mutableMax.maxOrNull()
        return maxValue
    }

    private fun getMin(data: Array<FloatArray>): Float? {

        val minValue: Float?
        val mutableMin: MutableList<Float> = mutableListOf()

        for (current in data){
            current.minOrNull()?.let { mutableMin.add(it) }
        }
        minValue = mutableMin.minOrNull()
        return minValue
    }


    private fun minMaxScaling(data: Array<FloatArray>): Array<FloatArray> {
        val minVal = getMin(data)
        val maxVal = getMax(data)
        val rows = data.size
        val columns = data[0].size
        val normalizedData: Array<FloatArray> = Array(rows){ FloatArray(columns) }

        for(i in data.indices){
            for (j in 0 until data[i].size){
                if (maxVal != null) {
                    normalizedData[i][j] = (data[i][j] - minVal!!)/(maxVal - minVal)
                }
            }
        }
        return normalizedData
    }



//    @JvmName("getAudioData1")
//    fun getAudioData(): FloatArray? {
//        return LogMelSpec.audioData
//    }

    fun getMFCC(path: String): Array<FloatArray> {
        innerPaths = path
        return main(innerPaths)
    }



}