package com.junj.utils

import java.util.concurrent.TimeUnit

private const val DEFAULT_REBOOT_TIMEOUT_SECONDS = 300L
private const val REBOOT_POLL_INTERVAL_MS = 1_000L
private const val SCREEN_WAKE_WAIT_MS = 1_000L
private const val KEYGUARD_DISMISS_WAIT_MS = 1_000L
private const val ANDROID_BOOT_ID_PATH = "/proc/sys/kernel/random/boot_id"

data class CommandResult(
    val exitCode: Int,
    val output: String
)

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

fun destroyProcessTree(process: Process) {
    val descendants = process.toHandle()
        .descendants()
        .toList()

    descendants.asReversed().forEach { handle ->
        if (handle.isAlive) {
            handle.destroy()
        }
    }

    process.destroy()

    if (!process.waitFor(3, TimeUnit.SECONDS)) {
        descendants.asReversed().forEach { handle ->
            if (handle.isAlive) {
                handle.destroyForcibly()
            }
        }

        if (process.isAlive) {
            process.destroyForcibly()
            process.waitFor()
        }
    }
}

/**
 * Reboots the connected Android device and blocks until Android has completed
 * booting. ADB becoming available is not sufficient: `sys.boot_completed` must
 * also be `1`, and, when readable, the kernel boot ID must have changed.
 *
 * @throws IllegalStateException when the reboot command fails or the device
 * does not finish booting before [timeoutSeconds].
 */
fun rebootDevice(timeoutSeconds: Long = DEFAULT_REBOOT_TIMEOUT_SECONDS) {
    require(timeoutSeconds > 0) { "timeoutSeconds must be greater than 0" }

    val bootIdBeforeReboot = runAdbShellCommand(
        "cat $ANDROID_BOOT_ID_PATH"
    ).takeIf { it.exitCode == 0 }
        ?.output
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    println("Rebooting Android device...")
    runAdbRootShellCommand("reboot")

    val deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
    var lastStatus = "waiting for the device to reconnect"

    while (System.nanoTime() < deadlineNs) {
        val bootCompletedResult = runAdbShellCommand("getprop sys.boot_completed")

        if (bootCompletedResult.exitCode == 0) {
            val bootCompleted = bootCompletedResult.output.trim() == "1"
            val bootIdAfterReboot = runAdbShellCommand(
                "cat $ANDROID_BOOT_ID_PATH"
            ).takeIf { it.exitCode == 0 }
                ?.output
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            val bootIdChanged = bootIdBeforeReboot == null ||
                (bootIdAfterReboot != null && bootIdAfterReboot != bootIdBeforeReboot)

            if (bootCompleted && bootIdChanged) {
                println("Android device has finished booting.")
                wakeAndUnlockDevice()
                return
            }

            lastStatus = when {
                !bootIdChanged -> "ADB is online, but the device has not rebooted yet"
                !bootCompleted -> "ADB is online, waiting for Android boot completion"
                else -> "waiting for Android boot completion"
            }
        } else {
            lastStatus = bootCompletedResult.output.trim().ifEmpty {
                "device is offline"
            }
        }

        val remainingMs = TimeUnit.NANOSECONDS
            .toMillis(deadlineNs - System.nanoTime())
        if (remainingMs > 0) {
            Thread.sleep(minOf(REBOOT_POLL_INTERVAL_MS, remainingMs))
        }
    }

    error(
        "Timed out after ${timeoutSeconds}s waiting for Android to boot. " +
            "Last status: $lastStatus"
    )
}

/**
 * Wakes the display and dismisses a non-secure keyguard. Android does not allow
 * this operation to bypass a PIN, password, pattern, or other secure credential.
 */
private fun wakeAndUnlockDevice() {
    val wakeResult = runAdbRootShellCommand("input keyevent KEYCODE_WAKEUP")
    check(wakeResult.exitCode == 0) {
        "Failed to wake Android device: ${wakeResult.output.trim()}"
    }

    Thread.sleep(SCREEN_WAKE_WAIT_MS)

    val unlockResult = runAdbRootShellCommand("wm dismiss-keyguard")
    check(unlockResult.exitCode == 0) {
        "Failed to dismiss Android keyguard: ${unlockResult.output.trim()}"
    }

    Thread.sleep(KEYGUARD_DISMISS_WAIT_MS)
    println("Android display is awake and the non-secure keyguard was dismissed.")
}
