package com.junj.utils

import com.junj.timeStamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File
import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileOutputStream

enum class LogType {
    SAMPLE,
    LAUNCH,
    SUMMARY,
}

fun getTimeStamp(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    val timeStamp = LocalDateTime.now().format(formatter)

    return timeStamp
}

class Logger() : Closeable {

    private val writers: Map<LogType, BufferedWriter> = mapOf(
        LogType.SAMPLE to FileOutputStream(
            File("../logs/$timeStamp/sample.log"),
            true
        ).bufferedWriter(Charsets.UTF_8),

        LogType.LAUNCH to FileOutputStream(
            File("../logs/$timeStamp/launch.log"),
            true
        ).bufferedWriter(Charsets.UTF_8),

        LogType.SUMMARY to FileOutputStream(
            File("../logs/$timeStamp/summary.txt"),
            true
        ).bufferedWriter(Charsets.UTF_8)
    )

    @Synchronized
    fun log(type: LogType, message: Any?) {
        val text = message.toString()
        println(text)
        writers.getValue(type).apply {
            appendLine(text)
//            flush()
        }
    }

    override fun close() {
        writers.values.forEach { writer ->
            writer.close()
        }
    }
}