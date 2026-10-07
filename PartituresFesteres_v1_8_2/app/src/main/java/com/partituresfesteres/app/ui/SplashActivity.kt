package com.partituresfesteres.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import com.partituresfesteres.app.data.LanguageManager
import com.partituresfesteres.app.data.SettingsStore

class SplashActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = if (resources.configuration.smallestScreenWidthDp >= 600) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        if (SettingsStore(this).load().keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        val splashImage = ImageView(this).apply {
            setImageResource(com.partituresfesteres.app.R.drawable.splash_screen_art)
            scaleType = if (isTablet) ImageView.ScaleType.FIT_XY else ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(0xFFF4E8D2.toInt())
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(splashImage)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1600)
    }
}
