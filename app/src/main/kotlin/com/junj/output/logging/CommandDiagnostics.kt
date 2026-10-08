package com.junj.output.logging

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 命令"没正常返回数据"时的留档工具（Windows 移植新增）。
 *
 * 真机测试里命令失败以前是完全静默的：统计里只看到 0，控制台也看不出原因。
 * 这里把失败的命令、退出码和设备的原始回显写进 `<输出目录>/command.log`。
 *
 * 采样每 10s 会重复执行同样的命令，所以同一个问题只在第一次出现时记录完整内容，
 * 出现次数留在结尾的 `printSummary()` 里汇总，避免把日志刷成几千行噪声。
 */
object CommandDiagnostics {
    private val counts = ConcurrentHashMap<Pair<String, String>, Int>()

    /**
     * 预期"一定有输出"的命令前缀：只有这些命令出现"退出码 0 但没输出"才算异常。
     *
     * 其余命令（`logcat -c`、`swapoff`、`swapon`、`mkswap`、`chmod`、`mount`、`echo`、
     * `reboot`、`input keyevent`、`wm dismiss-keyguard` …）本来就是静默成功，
     * 记进来只会让 command.log 变成噪声。
     */
    private val outputExpectedPrefixes = listOf(
        "cat ",
        "dumpsys ",
        "vmstat",
        "am start",
        "logcat -d",
        "getprop ",
        "pm ",
        "cmd ",
    )

    @Volatile
    private var outputFile: File? = null

    /** main 建好输出目录后调用一次，之后所有诊断都写到这个目录下的 command.log。 */
    fun attach(outputDirectory: File) {
        outputFile = File(outputDirectory, "command.log")
    }

    /** 记录一条诊断（去重：同一个 label + command 只写第一次）。 */
    fun record(label: String, command: String, output: String) {
        val key = label to command
        val count = counts.merge(key, 1) { old, _ -> old + 1 } ?: 1
        if (count > 1) return

        runCatching {
            val file = outputFile ?: return
            file.parentFile?.mkdirs()
            file.appendText("[$label] $command\n${output.trim().take(4000)}\n\n", Charsets.UTF_8)
        }
    }

    /**
     * 命令执行完后调用：退出码非 0，或者退出码为 0 但一个字都没输出时记一笔。
     *
     * "看起来成功、其实没拿到数据"是这套 adb 调用最容易被忽略的失败方式，所以要对
     * [outputExpectedPrefixes] 里的读命令做空输出告警；静默成功的写命令不记（见该列表说明）。
     */
    fun recordCommandProblem(command: String, exitCode: Int, output: String) {
        val failed = exitCode != 0
        val emptyOutput = output.isBlank() && outputExpectedPrefixes.any { command.startsWith(it) }
        if (!failed && !emptyOutput) return

        if (failed) {
            println("[command failed] exit=$exitCode :: $command")
        }

        record(if (failed) "exit=$exitCode" else "exit=0/empty", command, output)
    }

    /** 整轮跑完后，把每个问题出现的次数打印出来，方便判断是偶发还是一直如此。 */
    fun printSummary() {
        if (counts.isEmpty()) return

        println("\n--- command problems ---")
        counts.entries
            .sortedByDescending { it.value }
            .forEach { (key, count) ->
                println("[${key.first}] x$count :: ${key.second}")
            }
    }
}
