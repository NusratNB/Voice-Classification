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
import com.example.spectoclassifier118.spectoimage.SpectrogramGenerator
import android.os.SystemClock
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


class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var btnRecord: Button
    lateinit var btnClassification: Button
    lateinit var showBtn: Button
    var wavFile: WavFile? = null

    private lateinit var classifier: Classifier
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

        var audioRecoder = RecordWavMaster(this, pathToRecords.toString())
        var recording: Boolean = true
        val generator = LogMelSpecKt()




        txtSpeed = findViewById(R.id.txtSpeed)
        imageView = findViewById(R.id.imageView)
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

//                val data = generator.getAudio(fullAudioPath.path)

//                wavFile = WavFile.openWavFile(fullAudioPath);
                val datagenStartTime = SystemClock.uptimeMillis()
                val audioData = readMagnitudeValuesFromFile(fileName.path,-1, -1, 0 )
                val datagenEndTime = SystemClock.uptimeMillis()
                dataGenTime = (datagenEndTime - datagenStartTime).toFloat()


                resultCls = findViewById(R.id.result)
                var startTime = SystemClock.uptimeMillis()

                val result = audioData?.get(0)?.let { classifier.analyze(it) }
                val csvNamePath = fileName.toString().split(".wav")[0] + ".csv"
                val csvName = csvNamePath.substring(csvNamePath.lastIndexOf("/") +1 )
                val csvFullPath = pathToCSVFiles.absolutePath + "/" + csvName

                if (result != null) {
                    generateCSV(csvFullPath, result)
                }
                Toast.makeText(this, "CSV file $csvName", Toast.LENGTH_SHORT).show()

                val preResult = result?.get(0)

                var endTime = SystemClock.uptimeMillis()
                inferenceTime = (endTime - startTime).toFloat()
//                modelInfTime = classifier.getInfTime()
                var classes: Array<String> = arrayOf("down", "go", "left", "no", "off", "on", "right",
                                                    "stop", "up", "yes", "open_set")
                val maxIdx = preResult?.maxOrNull()?.let { it1 -> preResult.indexOfFirst { it == it1 } }
//                Toast.makeText(this, maxIdx.toString(), Toast.LENGTH_LONG).show()
//                var tt = spectogenerator.getInfTime()
//                val firstResult = result?.get(0)?.toString()
                resultCls.text ="Result: " + classes[maxIdx!!] + " | " + preResult[maxIdx]*100.0 +"%"
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateCSV(path: String, data: Array<FloatArray> ){

        Files.newBufferedWriter(Paths.get(path)).use { writer ->
            CSVPrinter(
                writer, CSVFormat.DEFAULT

            ).use { csvPrinter ->
                for (element in data){
                    csvPrinter.printRecord(element.asList())
                }
//            csvPrinter.printRecord(newAudioData.asList())
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
        spectogenerator = SpectrogramGenerator(this)
//        spectogenerator.initVars(pathToFolds)

    }

}