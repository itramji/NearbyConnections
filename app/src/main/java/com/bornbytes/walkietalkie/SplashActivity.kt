package com.bornbytes.walkietalkie

import android.content.Context
import android.content.Intent
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_splash.*
import java.util.jar.Manifest

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lav_walking.postDelayed({ startActivity(Intent(this@SplashActivity, MainActivity::class.java)) }, 2000L)
    }
}


fun Context.isTv():Boolean{
    return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}