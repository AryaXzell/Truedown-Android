package com.aryaxzell.truedown.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aryaxzell.truedown.MainActivity
import com.aryaxzell.truedown.R

object NotificationHelper {
    const val CHANNEL_PROGRESS = "channel_download_progress"
    const val CHANNEL_RESULT = "channel_download_result"
    const val CHANNEL_AUDIO = "channel_audio_player"

    const val ACTION_CANCEL_DOWNLOAD = "com.aryaxzell.truedown.ACTION_CANCEL"
    const val ACTION_RETRY_DOWNLOAD = "com.aryaxzell.truedown.ACTION_RETRY"
    const val ACTION_DOWNLOAD_MP3 = "com.aryaxzell.truedown.ACTION_DOWNLOAD_MP3"
    const val ACTION_OPEN_POST = "com.aryaxzell.truedown.ACTION_OPEN_POST"
    const val ACTION_CANCEL_APP_UPDATE = "com.aryaxzell.truedown.ACTION_CANCEL_APP_UPDATE"
    const val NOTIFICATION_ID_APP_UPDATE = 9999

    const val EXTRA_POST_ID = "extra_post_id"
    const val EXTRA_MEDIA_KIND = "extra_media_kind"
    const val EXTRA_MEDIA_INDEX = "extra_media_index"
    const val EXTRA_SOURCE_URL = "extra_source_url"
    const val EXTRA_URI_STRING = "extra_uri_string"

    fun createNotificationChannels(context: Context) {
        createChannels(context)
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                context.getString(R.string.notif_channel_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_progress_desc)
                setShowBadge(false)
            }

            val resultChannel = NotificationChannel(
                CHANNEL_RESULT,
                context.getString(R.string.notif_channel_result),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_result_desc)
            }

            val audioChannel = NotificationChannel(
                CHANNEL_AUDIO,
                context.getString(R.string.notif_channel_audio),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_audio_desc)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(resultChannel)
            notificationManager.createNotificationChannel(audioChannel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progress: Int,
        max: Int,
        cancelIntent: PendingIntent? = null
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setContentTitle(context.getString(R.string.notif_downloading))
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_truedown_logo)
            .setProgress(max, progress, max == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply {
                if (cancelIntent != null) {
                    addAction(0, context.getString(R.string.action_cancel), cancelIntent)
                }
            }
    }

    fun showSuccessNotification(
        context: Context,
        notificationId: Int,
        postId: String,
        title: String,
        mediaUri: String,
        mimeType: String,
        hasAudioOption: Boolean = false,
        showActions: Boolean = true
    ) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(mediaUri), mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaUri))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 2,
            Intent.createChooser(shareIntent, context.getString(R.string.action_share)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setContentTitle(context.getString(R.string.notif_download_success))
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_truedown_logo)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        if (showActions) {
            builder.addAction(0, context.getString(R.string.action_open), openPendingIntent)
            builder.addAction(0, context.getString(R.string.action_share), sharePendingIntent)

            if (hasAudioOption) {
                val mp3Intent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_DOWNLOAD_MP3
                    putExtra(EXTRA_POST_ID, postId)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val mp3PendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId * 10 + 3,
                    mp3Intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, context.getString(R.string.action_download_mp3), mp3PendingIntent)
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showErrorNotification(
        context: Context,
        notificationId: Int,
        postId: String,
        title: String,
        errorMessage: String,
        sourceUrl: String
    ) {
        val retryIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_RETRY_DOWNLOAD
            putExtra(EXTRA_POST_ID, postId)
            putExtra(EXTRA_SOURCE_URL, sourceUrl)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val retryPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 4,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setContentTitle(context.getString(R.string.notif_download_failed))
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_truedown_logo)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_retry), retryPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showUpdateProgressNotification(
        context: Context,
        title: String,
        progress: Float,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long = 0L
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_APP_UPDATE * 10 + 1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, AppUpdateCancelReceiver::class.java).apply {
            action = ACTION_CANCEL_APP_UPDATE
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_APP_UPDATE * 10 + 2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = (progress * 100).toInt().coerceIn(0, 100)
        val downloadedText = android.text.format.Formatter.formatFileSize(context, downloadedBytes)
        val totalText = if (totalBytes > 0) android.text.format.Formatter.formatFileSize(context, totalBytes) else "..."
        val speedText = if (speedBytesPerSec > 0) "${android.text.format.Formatter.formatFileSize(context, speedBytesPerSec)}/s" else ""

        val subtitle = if (speedText.isNotBlank()) {
            "$percent% • $downloadedText / $totalText • $speedText"
        } else {
            "$percent% • $downloadedText / $totalText"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_truedown_logo)
            .setProgress(100, percent, totalBytes <= 0L && progress <= 0f)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, context.getString(R.string.action_cancel), cancelPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_APP_UPDATE, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showUpdateReadyNotification(
        context: Context,
        apkFile: java.io.File,
        versionTag: String
    ) {
        val apkUri: Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val installPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_APP_UPDATE * 10 + 3,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setContentTitle(context.getString(R.string.notif_update_ready))
            .setContentText(context.getString(R.string.notif_update_ready_desc, versionTag))
            .setSmallIcon(R.drawable.ic_truedown_logo)
            .setAutoCancel(true)
            .setContentIntent(installPendingIntent)
            .addAction(0, context.getString(R.string.notif_update_install_action), installPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_APP_UPDATE, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showUpdateErrorNotification(
        context: Context,
        errorMessage: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_APP_UPDATE * 10 + 4,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setContentTitle(context.getString(R.string.notif_update_failed))
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_truedown_logo)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_APP_UPDATE, builder.build())
        } catch (_: SecurityException) {}
    }

    fun cancelUpdateNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_APP_UPDATE)
        } catch (_: SecurityException) {}
    }
}

class AppUpdateCancelReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_CANCEL_APP_UPDATE) {
            AppLogger.i("AppUpdateCancel", "Menerima aksi pembatalan unduhan update dari notifikasi")
            NightlyUpdateManager.cancelDownload(context)
            ReleaseUpdateManager.cancelDownload(context)
            NotificationHelper.cancelUpdateNotification(context)
        }
    }
}
