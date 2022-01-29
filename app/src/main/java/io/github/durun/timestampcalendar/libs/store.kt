package io.github.durun.timestampcalendar.libs

import android.content.SharedPreferences

import androidx.core.content.edit

private const val ROWS_KEY = "rows"

internal fun SharedPreferences.loadRows(): RowDataList {
    return RowDataList.decodeFromString(this.getString(ROWS_KEY, "") ?: "")
}

internal fun SharedPreferences.saveRows(rows: RowDataList) {
    this.edit(true) {
        putString(ROWS_KEY, rows.encodeToString())
    }
}