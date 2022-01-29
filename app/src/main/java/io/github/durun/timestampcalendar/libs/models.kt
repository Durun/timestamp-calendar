package io.github.durun.timestampcalendar.libs

import android.os.Parcel
import android.os.Parcelable

data class RowData(val text: String): Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString()!!)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
    }

    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<RowData> {
        const val INTENT_KEY = "RowData"
        override fun createFromParcel(parcel: Parcel): RowData = RowData(parcel)
        override fun newArray(size: Int): Array<RowData?> = arrayOfNulls(size)
    }
}

class RowDataList(list: MutableList<RowData> = mutableListOf()): MutableList<RowData> by list {
    fun addRowDataIfNotBlank(text: String) {
        if (text.isBlank()) return
        val sanitized = text.replace('\n', ' ')
        val newRow = RowData(sanitized)
        this.add(newRow)
    }

    fun move(fromPosition: Int, toPosition: Int) {
        val row = this.removeAt(fromPosition)
        this.add(toPosition, row)
    }

    fun encodeToString(): String {
        return this.joinToString("\n") { it.text }
    }

    companion object {
        fun decodeFromString(text: String): RowDataList {
            val values = text.split('\n')
                .filter { it.isNotBlank() }
                .map { RowData(text = it) }
            return RowDataList(values.toMutableList())
        }
    }
}