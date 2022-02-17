package com.example.spectoclassifier118.classifier

import android.content.Context
import android.content.res.AssetManager
import android.os.Bundle
import org.tensorflow.lite.Interpreter
import org.jetbrains.annotations.NotNull
import java.io.File


class ClassifierAlt(ctx: Context) {
    private var mContext: Context? = null
    var manager: AssetManager = getAssets()
    val nFrames =1
    var assetMgr: AssetManager = AssetManager()

}