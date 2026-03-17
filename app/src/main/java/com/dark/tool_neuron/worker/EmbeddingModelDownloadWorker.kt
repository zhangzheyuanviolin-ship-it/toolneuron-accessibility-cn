package com.dark.tool_neuron.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dark.tool_neuron.R
import com.dark.tool_neuron.engine.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class EmbeddingModelDownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "embedding_download"
        private const val NOTIFICATION_ID = 1001
        const val TAG = "EmbeddingModelDownload"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            createNotificationChannel()
            setForeground(createForegroundInfo(0))

            val modelPath = EmbeddingEngine.getModelPath(context)

            // Check if already downloaded
            if (modelPath.exists()) {
                showCompletionNotification(true)
                return@withContext Result.success()
            }

            modelPath.parentFile?.mkdirs()

            val url = URL("https://huggingface.co/spaces/Void2377/neurov/resolve/main/all-MiniLM-L6-v2-Q5_K_M.gguf?download=true")
            val connection = url.openConnection()
            connection.connect()

            val fileSize = connection.contentLength
            val inputStream = connection.getInputStream()
            val outputStream = modelPath.outputStream()

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int
            var lastProgress = 0
            var lastUpdateTime = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    inputStream.close()
                    outputStream.close()
                    modelPath.delete()
                    return@withContext Result.failure()
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val progress = (totalBytesRead.toFloat() / fileSize.toFloat() * 100).toInt()
                val currentTime = System.currentTimeMillis()

                // Only update notification every 5% or every 3 seconds to avoid lag
                if (progress >= lastProgress + 5 || currentTime - lastUpdateTime >= 3000) {
                    setForeground(createForegroundInfo(progress))
                    lastProgress = progress
                    lastUpdateTime = currentTime
                }
            }

            outputStream.close()
            inputStream.close()

            showCompletionNotification(true)
            Result.success()
        } catch (e: Exception) {
            showCompletionNotification(false, e.message)
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Embedding Model Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Downloads the embedding model for RAG features"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading Embedding Model")
            .setContentText(if (progress > 0) "$progress% complete" else "Starting download...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(success: Boolean, error: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(if (success) "Embedding model ready for RAG features" else "Error: ${error ?: "Unknown"}")
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}