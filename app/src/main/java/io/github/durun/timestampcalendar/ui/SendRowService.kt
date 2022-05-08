package io.github.durun.timestampcalendar.ui

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.api.services.sheets.v4.model.ValueRange
import io.github.durun.timestampcalendar.R
import io.github.durun.timestampcalendar.libs.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class SendRowService : IntentService("SendRowService") {
    companion object {
        private const val TAG = "SendRowService"
        private val CHANNEL_ID = TAG

        private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd kk:mm:ss")
        private val formatter2 = dateFormatter

        private val notificationId = AtomicInteger(0)
    }

    override fun onHandleIntent(intent: Intent?) {
        // 準備
        val rowData = intent?.getParcelableExtra<RowData>(RowData.INTENT_KEY)
            ?: return
        val auth = MyAuth(this)
        if (!auth.isSignedIn()) throw Exception("Failed to sign in")
        val credential = auth.credential

        // 送る行データ
        val date =
            LocalDateTime.now().format(formatter2)
        val text = rowData.text
        val sendRow = listOf(date, text)

        // 途中通知
        createNotificationChannel()
        val notification = createNotification().also {
            it.second.apply {
                setContentText(text)
                setProgress(100, 0, true)
            }
        }
        notification.notify()

        // スプレッドシート
        val sheetsService = sheetsService(credential)

        val value = ValueRange().setValues(
            listOf(sendRow)
        )

        val sheetId = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("spread_sheet_id", null)
            ?: return

        val sheetName = DataSheet.logSheetName

        // logシートが無かったら作成する
        if (!DataSheet.logSheetExists(credential, sheetId)) {
            DataSheet.makeLogSheet(credential, sheetId)
            println("makeLogSheet")
        }

        val result = sheetsService.Spreadsheets().Values()
            .append(sheetId, "'$sheetName'!A:B", value)
            .setValueInputOption("USER_ENTERED")
            //.setValueInputOption("RAW")
            .execute()

        // 完了通知
        notification.second.setProgress(100, 100, false)
        notification.notify()
    }


    // notification

    /**
     * @return notificationId, notification
     */
    private fun createNotification(): Pair<Int, NotificationCompat.Builder> {
        return (notificationId.addAndGet(1)) to NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle("Timestamp Sent")
            priority = NotificationCompat.PRIORITY_DEFAULT
        }
    }

    private fun Pair<Int, NotificationCompat.Builder>.notify() {
        NotificationManagerCompat.from(this@SendRowService)
            .notify(this.first, this.second.build())
    }

    private fun Pair<Int, NotificationCompat.Builder>.cancel() {
        NotificationManagerCompat.from(this@SendRowService)
            .cancel(this.first)
    }

    private fun createNotificationChannel() {
        val name = "Timestamp"
        val descriptionText = "Timestamp Calendar notification"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
