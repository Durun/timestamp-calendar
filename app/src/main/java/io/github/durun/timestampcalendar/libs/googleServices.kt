package io.github.durun.timestampcalendar.libs

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets

private const val appName = "Timestamp Calendar"

fun sheetsService(credential: GoogleAccountCredential): Sheets = Sheets.Builder(
    AndroidHttp.newCompatibleTransport(),
    JacksonFactory.getDefaultInstance(),
    credential
)
    .setApplicationName(appName)
    .build()

fun driveService(credential: GoogleAccountCredential): Drive = Drive.Builder(
    AndroidHttp.newCompatibleTransport(),
    JacksonFactory.getDefaultInstance(),
    credential
)
    .setApplicationName(appName)
    .build()

fun calendarService(credential: GoogleAccountCredential) = Calendar.Builder(
    AndroidHttp.newCompatibleTransport(),
    JacksonFactory.getDefaultInstance(),
    credential
)
    .setApplicationName(appName)
    .build()