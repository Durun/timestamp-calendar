package io.github.durun.timestampcalendar.ui

import android.app.IntentService
import android.content.Intent
import androidx.preference.PreferenceManager
import com.google.api.services.sheets.v4.model.ValueRange
import io.github.durun.timestampcalendar.libs.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SendRowService : IntentService("SendRowService") {
    companion object {
        private const val TAG = "SendRowService"
        private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd kk:mm:ss")
        private val formatter2 = dateFormatter
    }

    override fun onHandleIntent(intent: Intent?) {
        // 準備
        val rowData = intent?.getParcelableExtra<RowData>(RowData.INTENT_KEY)
            ?: return
        val auth = MyAuth(this)
        if (!auth.isSignedIn()) throw Exception("Failed to sign in")
        val credential = auth.credential

        // 送る行データ
        val date = LocalDateTime.now().format(formatter2)
        val text = rowData.text
        val sendRow = listOf(date, text)

        // 途中通知
        val notification = ProgressNotification.of(this, "Timestamp Sent")
        notification.setContentText(text)
        notification.notifyInProgress(this)

        try {
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

            sheetsService.Spreadsheets().Values()
                .append(sheetId, "'$sheetName'!A:B", value)
                .setValueInputOption("USER_ENTERED")
                //.setValueInputOption("RAW")
                .execute()

            // 完了通知
            notification.notifyComplete(this)
        } catch (e: Exception) {
            notification.setContentText(e.message ?: e.stackTraceToString())
            notification.notifyComplete(this)
        }
    }
}
