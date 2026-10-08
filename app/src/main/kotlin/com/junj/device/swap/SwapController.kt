package com.junj.device.swap

import com.junj.cli.ArgType
import com.junj.device.adb.runAdbRootShellCommand
import com.junj.output.logging.Logger
import com.junj.output.logging.LogType

fun disableZram() {
    runAdbRootShellCommand("swapoff /dev/block/zram0")
}

fun enableFlashSwap(flashSwapDeviceSizeMb: Int) {
    runAdbRootShellCommand("dd if=/dev/zero of=/data/per_boot/flash.swap bs=1M count=$flashSwapDeviceSizeMb")
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

fun initSwapByType(type: Int, flashSwapDeviceSizeMb: Int, logger: Logger) {
    when (type) {
        ArgType.NO_SWAP.code -> {
            logger.log(LogType.SUMMARY, "=== No Swap ===")
            disableZram()
        }
        ArgType.ONLY_ZRAM.code -> logger.log(LogType.SUMMARY, "=== Only zRAM ===")
        ArgType.ONLY_FLASH_SWAP.code -> {
            logger.log(LogType.SUMMARY, "=== Only Flash Swap ===")
            disableZram()
            enableFlashSwap(flashSwapDeviceSizeMb)
        }
        ArgType.ZSWAP.code -> {
            logger.log(LogType.SUMMARY, "=== ZSWAP ===")
            disableZram()
            enableFlashSwap(flashSwapDeviceSizeMb)
            enableZswap()
        }
    }
}
