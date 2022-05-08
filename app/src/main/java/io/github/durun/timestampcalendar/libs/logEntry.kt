package io.github.durun.timestampcalendar.libs

import java.time.LocalDateTime

data class LogEntry(
    val date: LocalDateTime,
    val text: String
)