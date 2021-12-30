package com.example.spectoclassifier118

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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


class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var btnRecord: Button
    lateinit var btnClassification: Button
    lateinit var showBtn: Button
    private val pickImage=100
    private var imageUri: Uri? = null
    private var bitmap: Bitmap? = null
    private var resBitmap: Bitmap? = null
    private var trimmedBtm: Bitmap? = null
    private lateinit var classifier: Classifier
    private lateinit var spectogenerator: SpectrogramGenerator
//    private lateinit var audioRecoder: RecordWavMaster
    lateinit var audioName: TextView
    lateinit var resultCls: TextView
    lateinit var prevBtn: Button
    lateinit var nextBtn: Button
    lateinit var wavList: ArrayList<Any?>
    lateinit var setSpectoName: String
    var auidoIndex: Int = 0
    lateinit var pathToFolds: File
    var imgGenTime: Float = 0.0f
    var inferenceTime: Float = 0.0f
    var modelInfTime: Float = 0.0f
    lateinit var txtSpeed: TextView
    lateinit var btnPlay: Button
    lateinit var fileName: File
    var prevFileName: File =File("")

    lateinit var fullAudioPath: File
    lateinit var pathToRecords: File


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
                if (prevFileName.exists()){
                    prevFileName.delete()
                }
            }

        }

        btnClassification = findViewById(R.id.classificationButton)
        btnClassification.setOnClickListener{
            if(fileName.exists()){
                val data = generator.getMFCC(fullAudioPath.path)
                val row = data.size
                val column = data[0].size
                val transpose = Array(column) { FloatArray(row) }
                for (i in 0..row - 1) {
                    for (j in 0..column - 1) {
                        transpose[j][i] = data[i][j]
                    }
                }
                resultCls = findViewById(R.id.result)
                var startTime = SystemClock.uptimeMillis()
                val result = transpose?.let { classifier.analyze(it) }
                var endTime = SystemClock.uptimeMillis()
                inferenceTime = (endTime - startTime).toFloat()
//                modelInfTime = classifier.getInfTime()
                val maxIdx = result.maxOrNull()?.let { it1 -> result.indexOfFirst { it == it1 } }
                Toast.makeText(this, maxIdx.toString(), Toast.LENGTH_LONG).show()
//                var tt = spectogenerator.getInfTime()
//                val firstResult = result?.get(0)?.toString()
//                resultCls.text ="Result: " + firstResult
//                txtSpeed.text = "Model time: " + modelInfTime.toString() + " Inference: " + inferenceTime.toString() + " ms;   Data gen: " + tt.toString() + " ms"
//                prevFileName = fileName
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


    fun resizeBitmap(bmt:Bitmap, width: Int, height: Int):Bitmap{

        return Bitmap.createScaledBitmap(
            bmt,
            width,
            height,
            true
        )
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