package com.example.aviatorai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var thread: HandlerThread? = null
    private var lastText = ""
    private var lastScanAt = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(7, notification())
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_RESULT_DATA)

        if (data == null || resultCode == 0) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            startCapture()
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    private fun startCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        thread = HandlerThread("AviatorScreenOCR").also { it.start() }
        val handler = Handler(thread!!.looper)
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader?.setOnImageAvailableListener({ imageReader ->
            val now = System.currentTimeMillis()
            if (now - lastScanAt < 1800) {
                imageReader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastScanAt = now
            val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val bitmap = imageToBitmap(image)
            image.close()
            if (bitmap != null) recognize(bitmap)
        }, handler)
        display = projection?.createVirtualDisplay(
            "AviatorAI-Screen", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, handler
        )
    }

    private fun imageToBitmap(image: Image): Bitmap? = try {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowPadding = plane.rowStride - pixelStride * image.width
        val paddedWidth = image.width + rowPadding / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
    } catch (_: Exception) { null }

    private fun recognize(bitmap: Bitmap) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                val text = result.text
                if (text.isNotBlank() && text != lastText) {
                    lastText = text
                    sendBroadcast(Intent(ACTION_TEXT).setPackage(packageName)
                        .putExtra(EXTRA_TEXT, summarize(text)))
                }
                recognizer.close(); bitmap.recycle()
            }
            .addOnFailureListener { recognizer.close(); bitmap.recycle() }
    }

    private fun summarize(text: String): String {
        val pattern = Regex("(?i)(\\\\d+(?:\\\\.\\\\d+)?\\\\s*x)")
        val found = pattern.findAll(text).map { it.value }.distinct().toList()
        return if (found.isNotEmpty())
            "स्क्रीन पर पहचाने गए multiplier:\\n${found.takeLast(12).joinToString("  •  ")}\\n\\nये केवल स्क्रीन पर दिखे टेक्स्ट हैं, अगले राउंड की भविष्यवाणी नहीं।"
        else "कोई स्पष्ट multiplier नहीं मिला।\\n\\nपहचाना गया टेक्स्ट:\\n${text.take(1000)}"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Aviator AI screen reading", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Aviator AI screen reading चालू")
        .setContentText("स्क्रीन का टेक्स्ट पढ़ा जा रहा है। रोकने के लिए ऐप खोलें।")
        .setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build()

    override fun onDestroy() {
        display?.release(); reader?.close(); projection?.stop(); thread?.quitSafely()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_TEXT = "com.example.aviatorai.SCREEN_TEXT"
        const val EXTRA_TEXT = "screen_text"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val CHANNEL_ID = "aviator_ai_capture"
    }
}
