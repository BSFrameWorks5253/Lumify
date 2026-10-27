package com.lumify.editor.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.lumify.editor.MainActivity
import com.lumify.editor.R
import com.lumify.editor.core.ImageProcessor
import com.lumify.editor.ui.screens.ExportFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class BatchExportService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isCanceled = false

    companion object {
        const val CHANNEL_ID = "lumify_batch_export_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START_EXPORT = "com.lumify.editor.export.START"
        const val ACTION_CANCEL_EXPORT = "com.lumify.editor.export.CANCEL"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_EXPORT -> {
                isCanceled = true
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_EXPORT -> {
                isCanceled = false
                val notification = buildNotification("Preparing batch export...", 0, 100)
                startForeground(NOTIFICATION_ID, notification)
                startExportPipeline()
            }
        }
        return START_NOT_STICKY
    }

    private fun startExportPipeline() {
        serviceScope.launch {
            val items = BatchQueueManager.queue.value
            val total = items.size
            var successCount = 0

            for (i in items.indices) {
                if (isCanceled) break
                val item = items[i]
                BatchQueueManager.updateProgress(i + 1, total, item.title)

                val progress = ((i.toFloat() / total) * 100).toInt()
                updateNotification("Exporting ${i + 1} of $total: ${item.title}", progress, 100)

                try {
                    val finalBitmap = ImageProcessor.processImage(item.bitmap, item.editState, isExport = true)
                    val fileName = "LUMIFY_BATCH_${System.currentTimeMillis()}_${i + 1}.${item.format.extension.lowercase().substringBefore(' ')}"
                    val saved = saveToGallery(finalBitmap, fileName, item.format.compressFormat, item.quality)
                    if (saved) successCount++
                } catch (_: Exception) {
                }
            }

            BatchQueueManager.finishExport(successCount)
            showCompletionNotification(successCount, total)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Batch Export Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress during batch master export"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, current: Int, max: Int): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lumify Batch Exporter")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_lumify_logo)
            .setProgress(max, current, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String, current: Int, max: Int) {
        val notification = buildNotification(text, current, max)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(success: Int, total: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Batch Export Finished")
            .setContentText("Successfully exported $success of $total master photos to Gallery")
            .setSmallIcon(R.drawable.ic_lumify_logo)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun saveToGallery(
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): Boolean {
        var outputStream: OutputStream? = null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumify")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false
                outputStream = contentResolver.openOutputStream(uri)
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/Lumify"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
            }

            outputStream?.let {
                bitmap.compress(format, quality, it)
                it.flush()
                true
            } ?: false
        } catch (_: Exception) {
            false
        } finally {
            outputStream?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
