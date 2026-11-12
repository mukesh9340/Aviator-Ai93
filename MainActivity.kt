package com.example.aviatorai

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var output: TextView
    private val captureRequest = 501

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra(ScreenCaptureService.EXTRA_TEXT).orEmpty()
            if (text.isNotBlank()) runOnUiThread {
                output.text = text
                status.text = "स्क्रीन पढ़ी गई"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { text = "स्क्रीन कैप्चर बंद है"; textSize = 16f }
        output = TextView(this).apply {
            text = "गेम खोलने के बाद स्क्रीन शेयरिंग शुरू करें।"
            textSize = 18f
            setPadding(0, 24, 0, 24)
        }
        val start = Button(this).apply {
            text = "Start Live Screen Reading"
            setOnClickListener {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
            }
        }
        val stop = Button(this).apply {
            text = "Stop"
            setOnClickListener {
                stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                status.text = "स्क्रीन कैप्चर बंद है"
            }
        }
        val note = TextView(this).apply {
            text = "\nध्यान दें: Aviator का अगला multiplier या crash समय भरोसेमंद तरीके से पता नहीं लगाया जा सकता। यह ऐप जीत की भविष्यवाणी नहीं करता और कोई bet नहीं लगाता।"
            textSize = 14f
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            addView(TextView(this@MainActivity).apply {
                text = "✈ AVIATOR AI ASSISTANT"; textSize = 24f
            })
            addView(status); addView(start); addView(stop); addView(output); addView(note)
        }
        setContentView(ScrollView(this).apply { addView(layout) })
        val filter = IntentFilter(ScreenCaptureService.ACTION_TEXT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(receiver, filter)
    }

    @Deprecated("Deprecated in Android but used for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
            status.text = "स्क्रीन पढ़ना चालू है — अब गेम पर जाएँ"
        } else status.text = "स्क्रीन शेयरिंग की अनुमति नहीं मिली"
    }

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
