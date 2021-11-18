package com.example.spectoclassifier118

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.example.spectoclassifier118.classifier.Classifier
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var btnLoadImage: Button
    lateinit var btnClassification: Button
    lateinit var showBtn: Button
    private val pickImage=100
    private var imageUri: Uri? = null
    private var bitmap: Bitmap? = null
    private var resBitmap: Bitmap? = null
    private lateinit var classifier: Classifier
    private var size:Int = 300



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initClassifier()

        imageView = findViewById(R.id.imageView)
        btnLoadImage = findViewById(R.id.buttonLoadPicture)
        btnLoadImage.setOnClickListener{
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
        btnClassification = findViewById(R.id.classificationButton)
        btnClassification.setOnClickListener{
            val result = bitmap?.let { classifier.analyze(it) }
            val firstResult = result?.get(0)?.toString()

            var toast = Toast.makeText(this, firstResult, Toast.LENGTH_LONG)
            val v = toast.view!!.findViewById<View>(android.R.id.message) as TextView
            v.setTextColor(Color.RED)
            toast.show()

        }
        showBtn = findViewById(R.id.showButton)
        showBtn.setOnClickListener{
            resBitmap = bitmap?.let { it1 -> classifier.resizeBitmap(it1, size, size) }
            imageView.setImageBitmap(resBitmap)
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            imageView.setImageURI(imageUri)
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        }
    }


    private fun initClassifier() {
        classifier = Classifier(this)
    }
}