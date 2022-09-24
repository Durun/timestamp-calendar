package io.github.durun.timestampcalendar.libs

import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.model.*
import com.google.api.services.sheets.v4.model.RowData
import java.time.OffsetDateTime

object DataSheet {
    private const val TAG = "DataSheet"
    const val title = "TimeStampCalendarData"
    private const val controlSheetName = "control"
    private const val doneValueRange = "${controlSheetName}!B1"
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

    fun readDoneIndex(credential: GoogleAccountCredential, spreadSheetId: String): Int? {
        val result = sheetsService(credential).Spreadsheets().get(spreadSheetId)
            .apply {
                includeGridData = true
                ranges = listOf(doneValueRange)
            }
            .execute()
        val cell = result.sheets.first()
            .data.first()
            .rowData.first()
            .getValues().first()
        return cell.effectiveValue.numberValue?.toInt()
    }

    fun writeDoneIndex(credential: GoogleAccountCredential, spreadSheetId: String, index: Int) {
        val content = ValueRange()
            .setRange(doneValueRange)
            .setValues(listOf(listOf(index)))
        val result = sheetsService(credential).Spreadsheets().values()
            .update(spreadSheetId, doneValueRange, content)
            .setValueInputOption("RAW")
            .execute()
        Log.d(TAG, result.toPrettyString())
    }

    fun readHistory(credential: GoogleAccountCredential, spreadSheetId: String, doneIndex: Int): List<LogEntry> {
        val result = sheetsService(credential).Spreadsheets().get(spreadSheetId)
            .apply {
                includeGridData = true
                ranges = listOf("${logSheetName}!A${doneIndex+1}:B")
            }
            .execute()
        return ((result.sheets.firstOrNull { it.properties.title == logSheetName }
            ?: throw Exception("Sheet $logSheetName not found in: ${result.sheets}"))
            .data.firstOrNull()
            ?: throw Exception("Data is empty: ${result.sheets.first { it.properties.title == logSheetName }.data}"))
            .rowData?.map { row ->
                val (date, text) = row.getValues().map { it.formattedValue }
                LogEntry(
                    date = OffsetDateTime.parse(date, dateFormatter),
                    text = text
                )
            } ?: emptyList()
    }
}