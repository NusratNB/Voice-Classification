package com.example.spectoclassifier118

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.spectoclassifier118.classifier.*
import java.io.File
import com.example.spectoclassifier118.spectoimage.RecordWavMaster
import com.example.spectoclassifier118.wavreader.WavFile
import com.example.spectoclassifier118.wavreader.FileFormatNotSupportedException
import com.example.spectoclassifier118.wavreader.WavFileException
import com.example.spectoclassifier118.utils.RecognitionFilter
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var btnRecord: Button
    lateinit var btnClassification: Button
    lateinit var btnAltClassification: Button

    private lateinit var classifier: CoroutinesHandler
    var dataGenTime: Float = 0.0f
    var inferenceTime: Float = 0.0f
    lateinit var btnPlay: Button
    lateinit var fileName: File
    var prevFileName: File =File("")

    lateinit var fullAudioPath: File
    lateinit var pathToRecords: File
    lateinit var pathToCSVFiles: File

    lateinit var firstModTxt: TextView
    lateinit var secondModTxt: TextView
    lateinit var thirdModTxt: TextView
    lateinit var fourthModTxt: TextView
    lateinit var fifthModTxt: TextView
    lateinit var infTimeTxt: TextView
    lateinit var altModTxt: TextView

    private lateinit var resultFirst: Array<FloatArray>
    private lateinit var resultSecond: Array<FloatArray>
    lateinit var resultThird: Array<FloatArray>
    private lateinit var resultFourth: Array<FloatArray>
    private lateinit var resultFifth: Array<FloatArray>
    var nFrames: Int = 1

    private val firstModelName: String = "ModFirst.tflite"
    private val secondModelName: String = "ModSecond.tflite"
    private val thirdModelName: String = "ModThird.tflite"
    private val fourthModelName: String = "ModFourth.tflite"
    private val fifthModelName: String = "ModFifth.tflite"
    private val modelAlt = "model_07.tflite"

    private val firstModAudLength: Int = 3195
    private val secModAudLength: Int = 5010
    private val thirdModAudLength: Int = 8640
    private val fourthModAudLength: Int = 12240
    private val fifthModAudLength: Int = 15900

    private var firstModGoogProb: Float = 0.0f
    private var secModGoogProb: Float = 0.0f
    private var thirdModGoogProb: Float = 0.0f
    private var fourthModGoogProb: Float = 0.0f
    private var fifthModGoogProb: Float = 0.0f

    private var firstModAveProb: Float = 0.0f
    private var secModAveProb: Float = 0.0f
    private var thirdModAveProb: Float = 0.0f
    private var fourthModAveProb: Float = 0.0f
    private var fifthModAveProb: Float = 0.0f

    private var firstModSynProb: Float = 0.0f
    private var secModSynProb: Float = 0.0f
    private var thirdModSynProb: Float = 0.0f
    private var fourthModSynProb: Float = 0.0f
    private var fifthModSynProb: Float = 0.0f

    private var firstModGoogClsName: String = ""
    private var secModGoogClsName: String = ""
    private var thirdModGoogClsName: String = ""
    private var fourthModGoogClsName: String = ""
    private var fifthModGoogClsName: String = ""

    private var firstModAveClsName: String = ""
    private var secModAveClsName: String = ""
    private var thirdModAveClsName: String = ""
    private var fourthModAveClsName: String = ""
    private var fifthModAveClsName: String = ""

    private var firstModSynClsName: String = ""
    private var secModSynClsName: String = ""
    private var thirdModSynClsName: String = ""
    private var fourthModSynClsName: String = ""
    private var fifthModSynClsName: String = ""

    private val batchSize: Int = 32

    private val firstClassifier: FirstModelClassifier = FirstModelClassifier()
    private val secondClassifier: SecondModelClassifier = SecondModelClassifier()
    private val thirdClassifier: ThirdModelClassifier = ThirdModelClassifier()
    private val fourthClassifier: FourthModelClassifier = FourthModelClassifier()
    private val fifthClassifier: FifthModelClassifier = FifthModelClassifier()
    private val clsAlt: ClassifierAlt = ClassifierAlt()



//    # Audio length
//    # mean - 1.5* std = 2955 + 240 = 3195
//    # mean - std = 8400 - 3630 = 4770 + 240 = 5010
//    # mean = 8400 samples ==>8640
//    # mean + std  = 12000 + 240 = 12240
//    # mean + 2*std = 15660 + 240 = 15900



    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted){
                    Toast.makeText(this@MainActivity, "Permission is granted",
                        Toast.LENGTH_SHORT).show()
                }else{
                    if (permissionName== Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this@MainActivity, "Storage reading denied",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }



    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firstModTxt = findViewById(R.id.firstModel)
        secondModTxt = findViewById(R.id.secondModel)
        thirdModTxt = findViewById(R.id.thirdModel)
        fourthModTxt = findViewById(R.id.fourthModel)
        fifthModTxt = findViewById(R.id.fifthModel)
        infTimeTxt = findViewById(R.id.infTime)
        altModTxt = findViewById(R.id.resultAltMod)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { // get permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),200);
            requestStoragePermission()
        }
        initClassifierSpectrogramGenerator()
        pathToRecords = File(externalCacheDir?.absoluteFile, "AudioRecord" )
        pathToCSVFiles = File(externalCacheDir?.absoluteFile, "CSVFiles")
        if (!pathToCSVFiles.exists()){
            pathToCSVFiles.mkdir()
        }
        if (!pathToRecords.exists()){
            pathToRecords.mkdir()
        }
        val classes: Array<String> = arrayOf("고마워", "보고싶어", "빨리", "사랑해", "싫어", "아파", "짜증나")

        val audioRecoder = RecordWavMaster(this, pathToRecords.toString())
        var recording: Boolean = true
        btnRecord = findViewById(R.id.btnRecord)
        btnRecord.text = "Start"
        btnRecord.setOnClickListener{

            if (recording){
                audioRecoder.recordWavStart()
                btnRecord.text = "Recording"
                recording = false


            }else{
                audioRecoder.recordWavStop()
                btnRecord.text = "Start"
                recording = true
                fileName = audioRecoder.audioName
                fullAudioPath = File(fileName.toString()) //File(pathToRecords.toString(), fileName.toString())
//                if (prevFileName.exists()){
//                    prevFileName.delete()
//                }
                val noiseOut = audioRecoder.noiseClassifierResult
                for (element in noiseOut){
                    Log.d("Inference noise out", Arrays.toString(element))
                }
            }


        }

        btnAltClassification = findViewById(R.id.btnAltClass)
        btnAltClassification.setOnClickListener {
            //Todo make inference for Alt approach
            val altModStartTime = SystemClock.uptimeMillis()
            if (fileName.exists()){
                val audioData = readMagnitudeValuesFromFile(fileName.path,-1, -1, 0 )
//                val resultData = clsAlt.makeInferenceAlt(this, audioData, 15900 )


                val resultData = audioData?.get(0)?.let { it1 ->
                    makePrediction( assets,
                        modelName = modelAlt, data = it1,
                        audioLength = 15600, nBatch = 1
                    )
                }!!
//                val resultData = audioData?.get(0)?.let { _ ->
//                    clsAlt.makeInferenceAlt(this, it, 15900)
//
//                }
//                val resultData = clsAlt.makeInferenceAlt(this, audioData?.get(0)!!, 15900)
//                val resultData = data.let { clsAlt.makeInferenceAlt(this, it, audioLength) }



//                Log.d("resultData: ", resultData.joinToString(" "))
                val customMaxId =
                    resultData[0].maxOrNull()?.let { it1 -> resultData[0].indexOfFirst { it == it1 } }!!
                var customClProb = resultData[0][customMaxId] * 100.0
                customClProb = String.format("%.2f", customClProb).toDouble()
                val customClsName = classes[customMaxId]
                val altModEndTime = SystemClock.uptimeMillis()
                val altModProcessTime = ((altModEndTime - altModStartTime).toFloat())/1000f
                altModTxt.text = "Result: $customClsName, Prob: $customClProb, Time: $altModProcessTime"

            }


        }


        btnClassification = findViewById(R.id.classificationButton)
        btnClassification.setOnClickListener{
            if(fileName.exists()){
                val datagenStartTime = SystemClock.uptimeMillis()
                val audioData = readMagnitudeValuesFromFile(fileName.path,-1, -1, 0 )
                val datagenEndTime = SystemClock.uptimeMillis()
                dataGenTime = (datagenEndTime - datagenStartTime).toFloat()
                val startTime = SystemClock.uptimeMillis()

                val a1 = GlobalScope.async(Dispatchers.IO){
                    resultFirst =
                        audioData?.get(0)?.let { it1 ->
                            makePrediction( assets,
                                modelName = firstModelName, data = it1,
                                audioLength = firstModAudLength, nBatch = batchSize
                            )
                        }!!
                }

//                val a2 = GlobalScope.async(Dispatchers.IO){
//                    resultSecond =
//                        audioData?.get(0)?.let { it1 ->
//                            makePrediction(assets,
//                                modelName = secondModelName, data = it1,
//                                audioLength = secModAudLength, nBatch = batchSize
//                            )
//                        }!!
//                }
//
                val a3 = GlobalScope.async(Dispatchers.Default) {
                    resultThird =
                        audioData?.get(0)?.let { it1 ->
                            makePrediction(assets,
                                modelName = thirdModelName, data = it1,
                                audioLength = thirdModAudLength, nBatch = batchSize
                            )
                        }!!
                }

                val a5 = GlobalScope.async(Dispatchers.Default) {
                    resultFifth =
                        audioData?.get(0)?.let { it1 ->
                            makePrediction(assets,
                                modelName = fifthModelName, data = it1,
                                audioLength = fifthModAudLength, nBatch = batchSize
                            )
                        }!!
                }
                val a4 = GlobalScope.async(Dispatchers.Default) {
                    resultFourth =
                        audioData?.get(0)?.let { it1 ->
                            makePrediction(assets,
                                modelName = fourthModelName, data = it1,
                                audioLength = fourthModAudLength, nBatch = batchSize
                            )
                        }!!
                }

                Log.d("ssss", "start ui logic")
                GlobalScope.launch(Dispatchers.Main) {
                    val differs = listOf(a1,  a3, a4, a5)
                    runBlocking {
                        Log.d("ssss", "start thread logic")
                        differs.awaitAll()

                        Log.d("ssss", "end thread logic")
                    }
                    val recFilter = RecognitionFilter()

                    firstModGoogProb = recFilter.googleApproach(resultFirst).toFloat()
                    Log.d("firstModGoogProb", firstModGoogProb.toString())
                    firstModGoogClsName = recFilter.googleClsName

//                    secModGoogProb = recFilter.googleApproach(resultSecond).toFloat()
//                    secModGoogClsName = recFilter.googleClsName

                    thirdModGoogProb = recFilter.googleApproach(resultThird).toFloat()
                    thirdModGoogClsName = recFilter.googleClsName

                    fourthModGoogProb = recFilter.googleApproach(resultFourth).toFloat()
                    fourthModGoogClsName = recFilter.googleClsName
//
                    fifthModGoogProb = recFilter.googleApproach(resultFifth).toFloat()
                    fifthModGoogClsName = recFilter.googleClsName

                    firstModAveProb = recFilter.takeAverage(resultFirst).toFloat()
                    firstModAveClsName = recFilter.customClsName

//                    secModAveProb = recFilter.takeAverage(resultSecond).toFloat()
//                    secModAveClsName = recFilter.customClsName
//
                    thirdModAveProb = recFilter.takeAverage(resultThird).toFloat()
                    thirdModAveClsName = recFilter.customClsName

                    fourthModAveProb = recFilter.takeAverage(resultFourth).toFloat()
                    fourthModAveClsName = recFilter.customClsName
//
                    fifthModAveProb = recFilter.takeAverage(resultFifth).toFloat()
                    fifthModAveClsName = recFilter.customClsName

                    val (firstModSynClsName, firstModSynProb) = recFilter.syntiantApproach(resultFirst)
//                    val (secondModSynClsName, secondModSynProb) = recFilter.syntiantApproach(resultSecond)
                    val (thirdModSynClsName, thirdModSynProb) = recFilter.syntiantApproach(resultThird)
                    val (fourthModSynClsName, fourthModSynProb) = recFilter.syntiantApproach(resultFourth)
                    val (fifthModSynClsName, fifthModSynProb) = recFilter.syntiantApproach(resultFifth)

                    firstModTxt.text = "1-M GProb: $firstModSynProb GCl: $firstModSynClsName AProb: $firstModAveProb ACl: $firstModAveClsName"
//                    secondModTxt.text = "2-M GProb: $secondModSynProb GCl: $secondModSynClsName AProb: $secModAveProb ACl: $secModAveClsName"
                    thirdModTxt.text = "3-M GProb: $thirdModSynProb GCl: $thirdModSynClsName AProb: $thirdModAveProb ACl: $thirdModAveClsName"
                    fourthModTxt.text = "4-M GProb: $fourthModSynProb GCl: $fourthModSynClsName AProb: $fourthModAveProb ACl: $fourthModAveClsName"
                    fifthModTxt.text = "5-M GProb: $fifthModSynProb GCl: $fifthModSynClsName AProb: $fifthModAveProb ACl: $fifthModAveClsName"
                    val endTime = SystemClock.uptimeMillis()
                    inferenceTime = ((endTime - startTime).toFloat())/1000.0f
                    infTimeTxt.text = "Prediction time: $inferenceTime s."


                    prevFileName = fileName
                }

            } else{
                Toast.makeText(this, "Record doesn't exists, please record your voice", Toast.LENGTH_SHORT).show()
            }

        }

        btnPlay = findViewById(R.id.btnPlay)
        btnPlay.setOnClickListener{

            audioRecoder.startPlaying(this, 1, fileName)
        }


    }
    private fun makePrediction(
        activity: AssetManager,
        modelName: String,
        data: FloatArray,
        audioLength: Int,
        nBatch: Int
    ): Array<FloatArray> {
        var resultData: Array<FloatArray> = emptyArray<FloatArray>()

        if (modelName == firstModelName){
            var resultDataFirst: Array<FloatArray> = emptyArray<FloatArray>()
            firstClassifier.initBatchSize(nBatch)
            resultDataFirst = data.let { firstClassifier.makeInference(activity,it, audioLength, modelName) }
            resultData = resultDataFirst
        } else if (modelName == secondModelName){
            var resultDataSecond: Array<FloatArray> = emptyArray<FloatArray>()
            secondClassifier.initBatchSize(nBatch)
            resultDataSecond = data.let { secondClassifier.makeInference(activity,it, audioLength, modelName) }
            resultData = resultDataSecond
        } else if (modelName == thirdModelName){
            var resultDataThird: Array<FloatArray> = emptyArray<FloatArray>()
            thirdClassifier.initBatchSize(nBatch)
            resultDataThird = data.let { thirdClassifier.makeInference(activity,it, audioLength, modelName) }
            resultData = resultDataThird
        } else if (modelName == fourthModelName){
            var resultDataFourth: Array<FloatArray> = emptyArray<FloatArray>()
            fourthClassifier.initBatchSize(nBatch)
            resultDataFourth = data.let { fourthClassifier.makeInference(activity,it, audioLength, modelName) }
            resultData = resultDataFourth
        } else if (modelName == fifthModelName){
            var resultDataFifth: Array<FloatArray> = emptyArray<FloatArray>()
            fifthClassifier.initBatchSize(nBatch)
            resultDataFifth = data.let { fifthClassifier.makeInference(activity,it, audioLength, modelName) }
            resultData = resultDataFifth
        }else if (modelName == modelAlt) {
            clsAlt.initBatchSize(nBatch)
            resultData = data.let { clsAlt.makeInferenceWithPreProcessingLayer(activity,it, audioLength, modelName) }
//            return data.let { clsAlt.makeInferenceAlt(this, it, audioLength) }
        }
        return resultData

    }


    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    private fun readMagnitudeValuesFromFile(
        path: String,
        sampleRate: Int,
        readDurationInSeconds: Int,
        offsetDuration: Int
    ): Array<FloatArray>? {
        if (!path.endsWith(".wav")) {
            throw FileFormatNotSupportedException(
                "File format not supported. jLibrosa currently supports audio processing of only .wav files"
            )
        }
        val sourceFile = File(path)
        var wavFile: WavFile? = null
        wavFile = WavFile.openWavFile(sourceFile)
        var mNumFrames = wavFile.numFrames.toInt()
        var mSampleRate = wavFile.sampleRate.toInt()
        val mChannels = wavFile.numChannels
        val totalNoOfFrames = mNumFrames
        val frameOffset = offsetDuration * mSampleRate
        var tobeReadFrames = readDurationInSeconds * mSampleRate
        if (tobeReadFrames > totalNoOfFrames - frameOffset) {
            tobeReadFrames = totalNoOfFrames - frameOffset
        }
        if (readDurationInSeconds != -1) {
            mNumFrames = tobeReadFrames
            wavFile.numFrames = mNumFrames.toLong()
        }
        if (sampleRate != -1) {
            mSampleRate = sampleRate
        }

        // Read the magnitude values across both the channels and save them as part of
        // multi-dimensional array
        val buffer = Array(mChannels) {
            FloatArray(
                mNumFrames
            )
        }
        var readFrameCount: Long = 0
        // for (int i = 0; i < loopCounter; i++) {
        readFrameCount = wavFile.readFrames(buffer, mNumFrames, frameOffset)
        // }
        wavFile?.close()
        return buffer
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) { // handle user response to permission request
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission to record audio granted", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_LONG).show()

        }
    }

    private fun showRationalDialog( title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){ dialog, _->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationalDialog("Kids drawing app needs storage access",
                "For loading image, Kids drawing app needs external storage")
        }else{
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }


    private fun initClassifierSpectrogramGenerator() {
        classifier = CoroutinesHandler(this, this.assets)

    }

}