package io.github.durun.timestampcalendar.libs

import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.time.ZoneOffset

object Calendar {
    fun insertEvent(credential: GoogleAccountCredential, calendarId: String, entry: LogEntry) {
        Log.d("Calendar", rfc3339DateFormatter.format(entry.date.toInstant(ZoneOffset.UTC)))
        val dateTime = DateTime(rfc3339DateFormatter.format(entry.date.toInstant(ZoneOffset.UTC)))
        val event = Event().apply {
            summary = entry.text
            description = entry.text
            start = EventDateTime().also {
                it.dateTime = dateTime
            }
            end = EventDateTime().also {
                it.dateTime = dateTime
            }
        }
        val result = calendarService(credential).Events().insert(calendarId, event).execute()
        Log.d("Calendar", result.toPrettyString())
    }
}