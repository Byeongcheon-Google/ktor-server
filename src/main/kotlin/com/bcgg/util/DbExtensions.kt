package com.bcgg.util

import com.bcgg.schema.Service
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneId
import java.time.format.DateTimeFormatter


suspend fun <T> Service.dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

val dbDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val dbTimeFormatter: DateTimeFormatter  = DateTimeFormatter.ofPattern("HH:mm")

val dateNow = java.time.LocalDate.now(ZoneId.of("Asia/Seoul")).toKotlinLocalDate()
val dateTimeNow = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))

fun LocalDate.toDbString() = dbDateFormatter.format(toJavaLocalDate())
fun LocalTime.toDbString() = dbTimeFormatter.format(toJavaLocalTime())
fun String.appendSecondAndToLocalTime() = "${this}:00".toLocalTime()