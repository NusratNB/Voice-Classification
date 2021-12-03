package com.example.spectoclassifier118

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.example.spectoclassifier118.classifier.Classifier
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.spectoclassifier118.spectoimage.SpectrogramGenerator
import java.io.FileOutputStream
import java.io.IOException
import android.os.Environment
import java.io.File


class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var btnLoadImage: Button
    lateinit var btnClassification: Button
    lateinit var showBtn: Button
    private val pickImage=100
    private var imageUri: Uri? = null
    private var bitmap: Bitmap? = null
    private var resBitmap: Bitmap? = null
    private var trimmedBtm: Bitmap? = null
    private lateinit var classifier: Classifier
    private lateinit var spectogenerator: SpectrogramGenerator

    private val storageAndLocationResultLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    if ( permissionName == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
                        Toast.makeText(this, "permission granted for reading storage", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }




    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initClassifierSpectrogramGenerator()

        imageView = findViewById(R.id.imageView)
        btnLoadImage = findViewById(R.id.buttonLoadPicture)
        btnLoadImage.setOnClickListener{
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }

        btnClassification = findViewById(R.id.classificationButton)
        btnClassification.setOnClickListener{
            val result = trimmedBtm?.let { classifier.analyze(it) }
            val firstResult = result?.get(0)?.toString()


            var toast = Toast.makeText(this, result.toString(), Toast.LENGTH_LONG)
            val v = toast.view!!.findViewById<View>(android.R.id.message) as TextView
            v.setTextColor(Color.RED)
            toast.show()
        }

        showBtn = findViewById(R.id.showButton)
        showBtn.setOnClickListener{
//            storageAndLocationResultLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.MANAGE_EXTERNAL_STORAGE))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                showRationalDialog("Permission Demo requires storage access",
                    "Camera cannot be used because storage access is denied")
            } else{
                storageAndLocationResultLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    )
                )
            }
            trimmedBtm = spectogenerator.generateImage(this)

            var xx = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
          //  var file_path: String = Environment.getExternalStorageDirectory() //+ "/Android/media"

            val file = File(
                Environment.getExternalStoragePublicDirectory("Download"),
                System.currentTimeMillis().toString()
            )

//            File(file_path, "map.png").writeBitmap(trimmedBtm!!, Bitmap.CompressFormat.PNG, 100)

            imageView.setImageBitmap(trimmedBtm)
            var filename = spectogenerator.getFilename()
            var toast = Toast.makeText(this, filename, Toast.LENGTH_LONG)
            val v = toast.view!!.findViewById<View>(android.R.id.message) as TextView
            v.setTextColor(Color.RED)
            toast.show()
        }
    }

    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
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


    fun resizeBitmap(bmt:Bitmap, width: Int, height: Int):Bitmap{

        return Bitmap.createScaledBitmap(
            bmt,
            width,
            height,
            true
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            imageView.setImageURI(imageUri)
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            resBitmap= bitmap?.let { resizeBitmap(it, 300, 300) }
        }
    }

    private fun initClassifierSpectrogramGenerator() {
        classifier = Classifier(this)
        spectogenerator = SpectrogramGenerator(this)
    }
}