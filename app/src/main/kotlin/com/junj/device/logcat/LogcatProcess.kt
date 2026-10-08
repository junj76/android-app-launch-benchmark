package com.junj.device.logcat

import com.junj.device.adb.POWERSHELL_EXECUTABLE
import com.junj.device.adb.runAdbRootShellCommand
import java.io.File

/** 需要抓取的低内存杀手日志关键字（原来写在 `grep -iE` 里）。 */
private const val LMK_KEYWORD = "lowmemorykiller: kill"

fun startLogcatProcess(logcatFile: File): Process {
    runAdbRootShellCommand("logcat -c")

    // Windows 上没有 bash/grep，这里改用 PowerShell：
    // `Select-String` 对应 `grep -iE`（默认忽略大小写），`ForEach-Object { $_.Line }`
    // 只输出匹配到的整行并逐行写出，等价于原来的 `--line-buffered`。
    val script =
        "[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new(\$false)\n" +
            "& adb.exe shell logcat -v threadtime | " +
            "Select-String -SimpleMatch -Pattern '$LMK_KEYWORD' | " +
            "ForEach-Object { \$_.Line }"

    return ProcessBuilder(
        POWERSHELL_EXECUTABLE,
        "-NoProfile",
        "-NonInteractive",
        "-Command",
        script,
    )
        .redirectOutput(logcatFile)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
}
