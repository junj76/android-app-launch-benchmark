package com.junj.device.adb

data class CommandResult(
    val exitCode: Int,
    val output: String
)

interface AdbExecutor {
    fun shell(command: String): CommandResult
    fun rootShell(command: String): CommandResult
}

object ProcessAdbExecutor : AdbExecutor {
    override fun shell(command: String): CommandResult = runAdbShellCommand(command)
    override fun rootShell(command: String): CommandResult = runAdbRootShellCommand(command)
}

fun runBashCommand(command: String): CommandResult {
    val process = ProcessBuilder(
        "/bin/bash",
        "-lc",
        command
    )
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
    val exitCode = process.waitFor()
    
    return CommandResult(exitCode, output)
}

fun runAdbShellCommand(command: String) : CommandResult {
    val process = ProcessBuilder(
        "/bin/bash",
        "-lc",
        "adb.exe shell $command"
    )
        .redirectErrorStream(true)
        .start()
    
    val output = process.inputStream
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
    val exitCode = process.waitFor()
    
    return CommandResult(exitCode, output)
}

//fun runAdbRootShellCommand(command: String) : CommandResult {
//    val hostScript = "exec adb.exe shell su -c \"\$1\""
//    val process = ProcessBuilder(
//        "/bin/bash",
//        "-c",
//        hostScript,
//        "runAdbRootShellCommand", // Bash 的 $0 占位符
//        command,                  // Bash 的 $1，即完整 Android 命令
//    )
//        .redirectErrorStream(true)
//        .start()
//
//    val output = process.inputStream
//        .bufferedReader(Charsets.UTF_8)
//        .use { it.readText() }
//    val exitCode = process.waitFor()
//
//    return CommandResult(exitCode, output)
//}

fun runAdbRootShellCommand(command: String): CommandResult {
    val hostScript =
        "printf '%s\\n' \"\$ADB_ROOT_COMMAND\" | adb.exe shell su -c sh"

    val processBuilder = ProcessBuilder(
        "/bin/bash",
        "-c",
        hostScript,
    )
        .redirectErrorStream(true)

    // command 作为环境变量数据传入，不会被本机 Bash 当作命令解析。
    processBuilder.environment()["ADB_ROOT_COMMAND"] = command

    val process = processBuilder.start()

    val output = process.inputStream
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

    val exitCode = process.waitFor()

    return CommandResult(exitCode, output)
}

