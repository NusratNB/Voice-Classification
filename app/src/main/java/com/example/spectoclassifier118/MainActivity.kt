package com.example.spectoclassifier118

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.spectoclassifier118.classifier.Classifier
import com.example.spectoclassifier118.classifier.ClassifierAlt
import com.example.spectoclassifier118.spectoimage.SpectrogramGenerator
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import com.example.spectoclassifier118.spectoimage.RecordWavMaster
import com.example.spectoclassifier118.spectoimage.LogMelSpecKt
import com.example.spectoclassifier118.wavreader.WavFile
import com.example.spectoclassifier118.wavreader.FileFormatNotSupportedException
import com.example.spectoclassifier118.wavreader.WavFileException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import kotlin.math.floor
import kotlin.math.max



class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var btnRecord: Button
    lateinit var btnClassification: Button
    lateinit var showBtn: Button
    var wavFile: WavFile? = null

    private lateinit var classifier: Classifier
    private lateinit var classifierAlt: ClassifierAlt
    private lateinit var spectogenerator: SpectrogramGenerator
    lateinit var resultCls: TextView
    lateinit var prevBtn: Button
    lateinit var nextBtn: Button
    var dataGenTime: Float = 0.0f
    var inferenceTime: Float = 0.0f
    lateinit var txtSpeed: TextView
    lateinit var btnPlay: Button
    lateinit var fileName: File
    var prevFileName: File =File("")

    lateinit var fullAudioPath: File
    lateinit var pathToRecords: File
    lateinit var pathToCSVFiles: File
    lateinit var transpose: Array<FloatArray>
    lateinit var googlePh: FloatArray
    lateinit var customPh: FloatArray

    lateinit var customPhTV: TextView
    lateinit var googlePhTV: TextView
    lateinit var syntiantPhTV: TextView
    var nFrames: Int = 1



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

//                    val pickIntent = Intent(
//                        Intent.ACTION_PICK,
//                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

//                    openGalleryLauncher.launch(pickIntent)
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { // get permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),200);
            requestStoragePermission()
        }
//        pathToFolds = File(externalCacheDir?.absoluteFile, "test_500" )
        initClassifierSpectrogramGenerator()
        pathToRecords = File(externalCacheDir?.absoluteFile, "AudioRecord" )
        pathToCSVFiles = File(externalCacheDir?.absoluteFile, "CSVFiles")
        if (!pathToCSVFiles.exists()){
            pathToCSVFiles.mkdir()
        }
        if (!pathToRecords.exists()){
            pathToRecords.mkdir()
        }
        val classes: Array<String> = arrayOf("down", "go", "left", "no", "off", "on", "right",
            "stop", "up", "yes", "open_set")

        var audioRecoder = RecordWavMaster(this, pathToRecords.toString())
        var recording: Boolean = true
        val generator = LogMelSpecKt()




        txtSpeed = findViewById(R.id.txtSpeed)
//        imageView = findViewById(R.id.imageView)
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
            }

        }

        btnClassification = findViewById(R.id.classificationButton)
        btnClassification.setOnClickListener{
            if(fileName.exists()){
                val datagenStartTime = SystemClock.uptimeMillis()
                val audioData = readMagnitudeValuesFromFile(fileName.path,-1, -1, 0 )
                val datagenEndTime = SystemClock.uptimeMillis()
                dataGenTime = (datagenEndTime - datagenStartTime).toFloat()


                resultCls = findViewById(R.id.result)
                val startTime = SystemClock.uptimeMillis()

                val result = audioData?.get(0)?.let { classifier.analyze(it) }
                val ressAlt = audioData?.get(0)?.let { classifierAlt.makeInference(it) }
                val csvNamePath = fileName.toString().split(".wav")[0] + ".csv"
                val csvName = csvNamePath.substring(csvNamePath.lastIndexOf("/") +1 )
                val csvFullPath = pathToCSVFiles.absolutePath + "/" + csvName
                Log.d("This is ressAlt ", ressAlt.toString())


                if (result != null) {
                    generateCSV(csvFullPath, result)
                }

                Toast.makeText(this, "CSV file $csvName", Toast.LENGTH_SHORT).show()
                val smoothedData = result?.let { it1 -> smoothData(it1) }
                val smcsvNamePath = fileName.toString().split(".wav")[0] + "_Smoothed.csv"
                val smcsvName = smcsvNamePath.substring(csvNamePath.lastIndexOf("/") +1 )
                val smcsvFullPath = pathToCSVFiles.absolutePath + "/" + smcsvName
                if (smoothedData != null){
                    generateCSV(smcsvFullPath, smoothedData)
                }
                customPh = FloatArray(11)
                for (i in transpose.indices){
                    customPh[i] = (transpose[i].average()).toFloat()
                }
                val customMaxId =
                    customPh.maxOrNull()?.let { it1 -> customPh.indexOfFirst { it == it1 } }
                var customClProb = customPh[customMaxId!!]*100.0
                customClProb = String.format("%.2f", customClProb).toDouble()
                customPhTV = findViewById(R.id.customPh)
                customPhTV.text = "CustomPH Result: " + classes[customMaxId!!] + " | " +customClProb + "%"



                googlePh = FloatArray(11)
                if (smoothedData != null) {
                    for (i in smoothedData.indices){
                        googlePh[i] = (smoothedData[i].average().toFloat())
                    }
                }
                val googleMaxId =  googlePh.maxOrNull()?.let { it1 -> googlePh.indexOfFirst { it == it1 }}
                var googleClProb = googlePh[googleMaxId!!]*100.0
                googleClProb = String.format("%.2f", googleClProb).toDouble()
                googlePhTV = findViewById(R.id.googlePh)
                googlePhTV.text = "GooglePH(Smoothing) Result: " + classes[googleMaxId!!] + " | " +googleClProb + "%"

                val syntiantPhThresholdV1 = 90.0
                val syntiantPhThresholdV2 = 80.0
                var averageThresholdV1 = 0.0
                var averageThresholdV2 = 0.0
                var countThV1 = 0
                var countThV2 = 0
                val consecutivePh = floor(nFrames*0.5).toInt()
                val dominantClass = transpose[customMaxId]
                for(i in dominantClass.indices){
                    val currentProb = dominantClass[i]*100.0
                    Log.d("Dominant class probs: ", currentProb.toString())
                    if (currentProb>syntiantPhThresholdV1){
                        countThV1 +=1
                        averageThresholdV1 += currentProb
                    }
                    if (currentProb>syntiantPhThresholdV2){
                        countThV2 +=1
                        averageThresholdV2 += currentProb
                    }
                }
                syntiantPhTV = findViewById(R.id.syntiantPh)
                if (countThV1>=consecutivePh){
                    averageThresholdV1 /= countThV1
                    averageThresholdV1 = String.format("%.2f", averageThresholdV1).toDouble()
                    syntiantPhTV.text = "SyntiantPh above 90.0%: " + countThV1 + " Class: "+ classes[customMaxId!!] + " | " + averageThresholdV1 + "%"
                }
                if (countThV2>=consecutivePh){
                    averageThresholdV2 /= countThV2
                    averageThresholdV2 = String.format("%.2f", averageThresholdV2).toDouble()
                    syntiantPhTV.text = "SyntiantPh above 80.0%: " + countThV2 + " Class: "+ classes[customMaxId!!] + " | " + averageThresholdV2 + "%"
                } else{
                    syntiantPhTV.text = "There is no probs above 80% and 90%"
                }
                val preResult = result?.get(0)

                val endTime = SystemClock.uptimeMillis()
                inferenceTime = (endTime - startTime).toFloat()

                val maxIdx = preResult?.maxOrNull()?.let { it1 -> preResult.indexOfFirst { it == it1 } }
//                resultCls.text ="Result: " + classes[maxIdx!!] + " | " + preResult[maxIdx]*100.0 +"%"
                resultCls.text = "Overall number of frames $nFrames"
                txtSpeed.text =
                    "Inference time: $inferenceTime ms | Datagen time: $dataGenTime ms"
                prevFileName = fileName
            } else{
                Toast.makeText(this, "Record doesn't exists, please record your voice", Toast.LENGTH_SHORT).show()
            }

        }
        btnPlay = findViewById(R.id.btnPlay)
        btnPlay.setOnClickListener{

            audioRecoder.startPlaying(this, 1, fileName)
        }

        showBtn = findViewById(R.id.showButton)
        showBtn.setOnClickListener{
            Toast.makeText(this, "Not working", Toast.LENGTH_SHORT).show()

//            var startImageGenTime = SystemClock.uptimeMillis()
//            trimmedBtm = spectogenerator.generateImage(this, fileName)
//            var endImageGenTime = SystemClock.uptimeMillis()
//            imgGenTime = ((endImageGenTime - startImageGenTime).toFloat())
//            wavList = spectogenerator.getWavList()
//            audioName = findViewById(R.id.audioName)
//            var strAudioName: String = spectogenerator.getFilename()
//
//            imageView.setImageBitmap(trimmedBtm)
////            audioName.text ="Audio name: " + strAudioName
//            txtSpeed.text ="Data generating time: " + imgGenTime.toString() + " ms"
        }


        nextBtn = findViewById(R.id.next)
        nextBtn.setOnClickListener{
            Toast.makeText(this, "Not working", Toast.LENGTH_SHORT).show()

//            if (wavList.size>=0 && auidoIndex<=wavList.size){
//                auidoIndex += 1
//
//                if (auidoIndex >= wavList.size){
//                    Toast.makeText(this, "This is end of the list", Toast.LENGTH_SHORT).show()
//                    setSpectoName = spectogenerator.setFilename(wavList[wavList.size - 1] as String).toString()
//                    audioName.text ="Audio name: " + wavList[wavList.size - 1] as String
//                    auidoIndex = wavList.size-1
//                } else{
//
//                    setSpectoName = spectogenerator.setFilename(wavList[auidoIndex] as String).toString()
//                    audioName.text ="Audio name: " + wavList[auidoIndex] as String
//
//                }
//
//            }
        }
        prevBtn = findViewById(R.id.previous)
        prevBtn.setOnClickListener{
            Toast.makeText(this, "Not working", Toast.LENGTH_SHORT).show()
//            if (auidoIndex>=0 && wavList.size>0){
//                auidoIndex -=1
//
//                if (auidoIndex <= 0){
//                    Toast.makeText(this, "This is beginning of the list", Toast.LENGTH_SHORT).show()
//                    setSpectoName = spectogenerator.setFilename(wavList[0] as String).toString()
//                    audioName.text ="Audio name: " + wavList[0] as String
//                    auidoIndex = 0
//                }else{
//
//                    setSpectoName = spectogenerator.setFilename(wavList[auidoIndex] as String).toString()
//                    audioName.text ="Audio name: " + wavList[auidoIndex] as String
//
//                }
//
//            }
        }
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
//        this.setNoOfChannels(mChannels)
//        this.setNoOfFrames(mNumFrames)
//        this.setSampleRate(mSampleRate)
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

    private fun transposeOutput(data: Array<FloatArray>): Array<FloatArray> {
        nFrames = data.size
        val nClasses = data[0].size
        transpose = Array(nClasses){FloatArray(nFrames)}
        for (i in 0 until nFrames){
            for (j in 0 until nClasses){
                transpose[j][i] = data[i][j]
            }
        }

        return transpose
    }
    private fun smoothData(data: Array<FloatArray>): Array<FloatArray> {
        nFrames = data.size
        val nClasses = data[0].size
        val transposedData = transposeOutput(data)
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
//        if (fullSmoothed != null) {
//            for (k in fullSmoothed.indices){
//                Log.d("Smoothed's Output + $k", fullSmoothed[k].contentToString())
//            }
//        }
        return fullSmoothed
    }


    private fun smoothOutput(j: Int, probs: FloatArray, wMax: Int, wSmooth: Int): Float {
        val hSmooth = max(1, (j-wSmooth+1))
        val sum = summation(hSmooth, probs, j)
        val smoothFactor = (1.0/(j.toFloat()-hSmooth.toFloat()+1.0)).toFloat()
        val pHat = smoothFactor*sum
        Log.d("This is pHat", pHat.toString())
        Log.d("This is hSmooth", hSmooth.toString())
        Log.d("This is summation", sum.toString())
        Log.d("This is j ", j.toString())
        Log.d("This is smoothFactor ", smoothFactor.toString())
        return pHat
    }

    private fun summation(hSmooth: Int, probs: FloatArray, j: Int): Float {
        return probs.copyOfRange(hSmooth, j).sum()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateCSV(path: String, data: Array<FloatArray> ){

        Files.newBufferedWriter(Paths.get(path)).use { writer ->
            CSVPrinter(
                writer, CSVFormat.DEFAULT

            ).use { csvPrinter ->
                for (element in data){
                    csvPrinter.printRecord(element.asList())
                }
                csvPrinter.flush()
            }
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



//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == RESULT_OK && requestCode == pickImage) {
//            imageUri = data?.data
//            imageView.setImageURI(imageUri)
//            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
//            resBitmap= bitmap?.let { resizeBitmap(it, 224, 224) }
//        }
//    }

    private fun initClassifierSpectrogramGenerator() {

        classifier = Classifier(this)
        classifierAlt = ClassifierAlt(this, this.assets)
        spectogenerator = SpectrogramGenerator(this)
//        spectogenerator.initVars(pathToFolds)

    }

}