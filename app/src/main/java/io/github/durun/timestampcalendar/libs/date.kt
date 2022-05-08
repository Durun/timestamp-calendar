package io.github.durun.timestampcalendar.libs

import java.time.format.DateTimeFormatter
import java.util.*

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy' at 'hh:mma", Locale("en"))