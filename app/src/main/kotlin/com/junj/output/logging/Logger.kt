package com.junj.output.logging

import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LogType {
    SAMPLE,
    LAUNCH,
    SUMMARY,
}

fun getTimeStamp(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    return LocalDateTime.now().format(formatter)
}

class Logger(outputDirectory: File) : Closeable {
    private val writers: Map<LogType, BufferedWriter> = mapOf(
        LogType.SAMPLE to FileOutputStream(File(outputDirectory, "sample.log"), true)
            .bufferedWriter(Charsets.UTF_8),
        LogType.LAUNCH to FileOutputStream(File(outputDirectory, "launch.log"), true)
            .bufferedWriter(Charsets.UTF_8),
        LogType.SUMMARY to FileOutputStream(File(outputDirectory, "summary.txt"), true)
            .bufferedWriter(Charsets.UTF_8),
    )

    @Synchronized
    fun log(type: LogType, message: Any?) {
        val text = message.toString()
        println(text)
        writers.getValue(type).apply {
            appendLine(text)
            // 每条都落盘：日志量很小（采样 10s 一条），但程序一旦中途崩溃，
            // 不 flush 的话整个文件都是空的，等于白跑。
            flush()
        }
    }

    override fun close() {
        writers.values.forEach { it.close() }
    }
}
