package io.github.durun.timestampcalendar.libs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.durun.timestampcalendar.R
import java.util.concurrent.atomic.AtomicInteger

/**
 * 途中経過を可視化するための通知
 */
class ProgressNotification private constructor(
    private val id: Int,
    private val builder: NotificationCompat.Builder
) {
    companion object {
        private val nextNotificationId = AtomicInteger(0)

        private fun channelIdOf(context: Context): String {
            return context::class.qualifiedName
                ?: throw Exception("Cannot get name of class $context")
        }

        private fun createChannel(
            context: Context,
            channelId: String,
            name: String = "Timestamp Calendar",
            descriptionText: String = "Timestamp Calendar progress notification",
        ) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        fun of(
            context: Context,
            title: String,
            channelId: String = channelIdOf(context)
        ): ProgressNotification {
            createChannel(context, channelId)
            return ProgressNotification(
                id = nextNotificationId.addAndGet(1),
                builder = NotificationCompat.Builder(context, channelId).apply {
                    setSmallIcon(R.mipmap.ic_launcher)
                    setContentTitle(title)
                    priority = NotificationCompat.PRIORITY_DEFAULT
                }
            )
        }
    }

    private fun notify(context: Context) {
        NotificationManagerCompat
            .from(context)
            .notify(this.id, this.builder.build())
    }

    /**
     * 通知を消す
     */
    fun cancel(context: Context) {
        NotificationManagerCompat
            .from(context)
            .cancel(this.id)
    }

    /**
     * 表示テキストを設定する
     */
    fun setContentText(text: String) {
        this.builder.setContentText(text)
    }

    /**
     * In progress 通知を表示する
     */
    fun notifyInProgress(context: Context) {
        this.builder.setProgress(100, 0, true)
        this.notify(context)
    }

    /**
     * 進捗率を表示する
     */
    fun notifyProgress(context: Context, max: Int, progress: Int) {
        this.builder.setProgress(max, progress, false)
        this.notify(context)
    }

    /**
     * 完了通知を表示する
     */
    fun notifyComplete(context: Context) {
        this.builder.setProgress(100, 100, false)
        this.notify(context)
    }
}
