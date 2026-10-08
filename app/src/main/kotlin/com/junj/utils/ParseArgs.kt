package com.junj.utils

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import com.junj.appSetNumber
import com.junj.flashSwapDevSize
import com.junj.logger
import com.junj.timeStamp
import com.junj.type

enum class ArgType {
    NO_SWAP,
    ONLY_ZRAM,
    ONLY_FLASH_SWAP,
    ZSWAP;

    val code: Int
        get() = ordinal
}

class AppCommand : CliktCommand(name = "android-app-launch-test") {
    val type: Int by option(
        "-t",
        "--type",
        help = "Swap type: 0=no swap, 1=zRAM, 2=flash swap, 3=zSwap",
    )
        .int()
        .required()
        .validate {
            require(it in ArgType.NO_SWAP.code..ArgType.ZSWAP.code) {
                "type must be between ${ArgType.NO_SWAP.code} and ${ArgType.ZSWAP.code}"
            }
        }

    val appSetNumber: Int by option(
        "-a",
        "--app-set",
        help = "app set number: 0=light, 1=mid, 2=heavy"
    )
        .int()
        .required()
        .validate {
            require(it in AppSetType.LIGHT.code..AppSetType.HEAVY.code) {
                "app-set must be between ${AppSetType.LIGHT.code} and ${AppSetType.HEAVY.code}"
            }
        }

    val logPath: String? by option(
        "-p",
        "--logPath",
        help = "Optional log output path",
    )

    override fun run() = Unit
}

fun parseArgs(args: Array<String>){
    val command = AppCommand().also { it.main(args) }
    appSetNumber = command.appSetNumber
    when (appSetNumber) {
        AppSetType.LIGHT.code -> timeStamp += "_light"
        AppSetType.MIDDLE.code -> timeStamp += "_middle"
        AppSetType.HEAVY.code -> timeStamp += "_heavy"
    }
    type = command.type
    command.logPath?.let { timeStamp += "_$it" }
}

fun disableZram() {
    runAdbRootShellCommand("swapoff /dev/block/zram0")
}

fun enableFlashSwap() {
    runAdbRootShellCommand("dd if=/dev/zero of=/data/per_boot/flash.swap bs=1M count=$flashSwapDevSize")
    runAdbRootShellCommand("chmod 600 /data/per_boot/flash.swap")
    runAdbRootShellCommand("mkswap /data/per_boot/flash.swap")
    runAdbRootShellCommand("swapon /data/per_boot/flash.swap")
    println(runAdbRootShellCommand("cat /proc/swaps").output)
}

fun enableZswap() {
    runAdbRootShellCommand("echo Y > /sys/module/zswap/parameters/enabled")
    println("zSwap enabled: " + runAdbRootShellCommand("cat /sys/module/zswap/parameters/enabled").output)
    runAdbRootShellCommand("mount -t debugfs debugfs /sys/kernel/debug")
}

fun initSwapByType(type: Int) : Unit {
    when (type) {
        ArgType.NO_SWAP.code -> {
            logger.log(LogType.SUMMARY, "=== No Swap ===")
            disableZram()
        }
        ArgType.ONLY_ZRAM.code -> {
            logger.log(LogType.SUMMARY, "=== Only zRAM ===")
        }
        ArgType.ONLY_FLASH_SWAP.code -> {
            logger.log(LogType.SUMMARY, "=== Only Flash Swap ===")
            disableZram()
            enableFlashSwap()
        }
        ArgType.ZSWAP.code -> {
            logger.log(LogType.SUMMARY, "=== ZSWAP ===")
            disableZram()
            enableFlashSwap()
            enableZswap()
        }
    }
}
