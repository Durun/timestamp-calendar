package io.github.durun.timestampcalendar.libs

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets

fun sheetsService(credential: GoogleAccountCredential): Sheets = Sheets.Builder(
    AndroidHttp.newCompatibleTransport(),
    JacksonFactory.getDefaultInstance(),
    credential
).build()

fun driveService(credential: GoogleAccountCredential): Drive = Drive.Builder(
    AndroidHttp.newCompatibleTransport(),
    JacksonFactory.getDefaultInstance(),
    credential
).build()