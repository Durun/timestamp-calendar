package io.github.durun.timestampcalendar.ui

import android.app.IntentService
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import io.github.durun.timestampcalendar.libs.Calendar
import io.github.durun.timestampcalendar.libs.DataSheet
import io.github.durun.timestampcalendar.libs.MyAuth
import io.github.durun.timestampcalendar.libs.ProgressNotification

class UpdateCalendarService : IntentService("UpdateCalendarService") {
    companion object {
        private const val TAG = "UpdateCalendarService"
    }

    override fun onHandleIntent(intent: Intent?) {
        val notification = ProgressNotification.of(this, "Updating calendar")
        notification.notifyInProgress(this)

        try {
            val auth = MyAuth(this)
            Log.d(TAG, "Logged in")

            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val sheetId = preferences.getString("spread_sheet_id", null)
                ?: throw Exception("Preference spread_sheet_id is not set")
            // カレンダーへの反映が終わっている行のIndex
            val doneIndex = DataSheet.readDoneIndex(auth.credential, sheetId)
                ?: throw Exception("Cannot read doneIndex")
            val entries = DataSheet.readHistory(auth.credential, sheetId, doneIndex)
            Log.d(TAG, "doneIndex = $doneIndex")
            val calendarId = preferences.getString("calendar_id", null)
                ?: throw Exception("Preference calendar_id is not set")
            entries.forEachIndexed { i, entry ->
                notification.notifyProgress(this, entries.size, i)
                Calendar.insertEvent(auth.credential, calendarId, entry)
            }
            val newDoneIndex = doneIndex + entries.size
            if (newDoneIndex != doneIndex) {
                DataSheet.writeDoneIndex(auth.credential, sheetId, newDoneIndex)
            }
            notification.notifyComplete(this)
        } catch (e: Exception) {
            notification.setContentText(e.message ?: e.stackTraceToString())
            notification.notifyComplete(this)
            throw e
        }
    }
}