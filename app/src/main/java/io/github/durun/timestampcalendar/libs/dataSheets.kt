package io.github.durun.timestampcalendar.libs

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.model.*
import com.google.api.services.sheets.v4.model.RowData

object DataSheet {
    const val title = "TimeStampCalendarData"

    fun newSheet(): Spreadsheet {
        return Spreadsheet().apply {
            properties = SpreadsheetProperties().setTitle("TimeStampCalendarData")
            sheets = listOf(
                Sheet().apply {
                    properties = SheetProperties().setTitle("control")
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
                    properties = SheetProperties().setTitle("log")
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

    fun isFormatOk(credential: GoogleAccountCredential, spreadSheetId: String): Boolean {
        val spreadsheet = sheetsService(credential).Spreadsheets()[spreadSheetId]
            .execute()
        val titles = spreadsheet.sheets.map { it.properties.title }
        return titles.containsAll(listOf("control", "log"))
    }
}