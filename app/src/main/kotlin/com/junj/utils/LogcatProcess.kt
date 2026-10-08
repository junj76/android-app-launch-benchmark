package com.junj.utils

import com.junj.logcatFile
import com.junj.timeStamp
import java.io.File

fun startLogcatProcess(): Process {
    logcatFile = File("../logs/$timeStamp/logcat.log")
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
