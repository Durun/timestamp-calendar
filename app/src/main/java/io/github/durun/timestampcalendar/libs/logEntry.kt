package io.github.durun.timestampcalendar.libs

import java.time.OffsetDateTime

data class LogEntry(
    val date: OffsetDateTime,
    val text: String
)