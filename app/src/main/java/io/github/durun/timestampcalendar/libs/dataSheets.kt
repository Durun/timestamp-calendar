package io.github.durun.timestampcalendar.libs

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.model.*
import com.google.api.services.sheets.v4.model.RowData

object DataSheet {
    const val title = "TimeStampCalendarData"
    const val controlSheetName = "control"
    const val logSheetName = "log"

    fun newSheet(): Spreadsheet {
        return Spreadsheet().apply {
            properties = SpreadsheetProperties().setTitle("TimeStampCalendarData")
            sheets = listOf(
                Sheet().apply {
                    properties = SheetProperties().setTitle(controlSheetName)
                    data = listOf(GridData().apply {
                        startRow = 0
                        startColumn = 0
                        rowData = listOf(
                            RowData().setValues(
                                listOf(CellData().setFormattedValue("uploaded to calendar"))
                            )
                        )
                    })
                },
                Sheet().apply {
                    properties = SheetProperties().setTitle(logSheetName)
                }
            )
        }
    }

    fun getIdOrNull(credential: GoogleAccountCredential): String? {
        val files = driveService(credential).Files()
            .list()
            .apply {
                corpora = "user"
                q = "name='$title' and mimeType='application/vnd.google-apps.spreadsheet'"
                orderBy = "viewedByMeTime desc"
            }
            .execute()
            .files
        return files.firstOrNull()?.id
    }

    fun logSheetExists(credential: GoogleAccountCredential, spreadSheetId: String): Boolean {
        val spreadsheet = sheetsService(credential).Spreadsheets()[spreadSheetId]
            .execute()
        val titles = spreadsheet.sheets.map { it.properties.title }
        return logSheetName in titles
    }

    fun makeLogSheet(credential: GoogleAccountCredential, spreadSheetId: String) {
        val body = BatchUpdateSpreadsheetRequest()
            .setRequests(
                listOf(
                    Request()
                        .setAddSheet(
                            AddSheetRequest()
                                .setProperties(SheetProperties().setTitle(logSheetName))
                        )
                )
            )
        sheetsService(credential).Spreadsheets()
            .batchUpdate(spreadSheetId, body)
            .execute()
    }

    fun isFormatOk(credential: GoogleAccountCredential, spreadSheetId: String): Boolean {
        val spreadsheet = sheetsService(credential).Spreadsheets()[spreadSheetId]
            .execute()
        val titles = spreadsheet.sheets.map { it.properties.title }
        return titles.containsAll(listOf(controlSheetName, logSheetName))
    }
}