package com.junj.device.logcat

import com.junj.device.adb.runAdbRootShellCommand
import java.io.File

fun startLogcatProcess(logcatFile: File): Process {
    runAdbRootShellCommand("logcat -c")

    return ProcessBuilder(
        "/bin/bash",
        "-lc",
        "adb.exe shell logcat -v threadtime | grep --line-buffered -iE \"lowmemorykiller: kill\""
    )
        .redirectOutput(logcatFile)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
}
