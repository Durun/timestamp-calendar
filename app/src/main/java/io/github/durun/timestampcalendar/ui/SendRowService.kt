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
        val notification = ProgressNotification.of(this, "Timestamp Sent")
        notification.notifyInProgress(this, text)

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
        notification.notifyComplete(this)
    }
}
