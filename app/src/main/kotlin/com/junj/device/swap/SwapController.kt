package com.junj.device.swap

import com.junj.cli.ArgType
import com.junj.device.adb.runAdbRootShellCommand
import com.junj.output.logging.Logger
import com.junj.output.logging.LogType

/** flash swap 文件路径（所有相关命令共用）。 */
const val FLASH_SWAP_FILE = "/data/per_boot/flash.swap"

/**
 * 创建 flash swap 文件前，要求 /data 在 swap 文件之外至少还剩这么多空间（MB）。
 *
 * 把数据分区写满会让系统没有可写空间，轻则卡顿重则直接死机，
 * 所以要留出余量给系统自己的写入。
 */
private const val FREE_SPACE_MARGIN_MB = 512

fun disableZram() {
    runAdbRootShellCommand("swapoff /dev/block/zram0")
}

/**
 * 检查 /data 剩余空间是否够放 flash swap 文件（+ 余量），不够就直接报错中止。
 *
 * 背景：dd 写几 GB 的零到 /data 会把数据分区写满，系统随即卡死；
 * 之前 4GB 的配置在真机上就是这么把手机写死的。宁可提前失败，也不要写死设备。
 */
private fun requireEnoughDataSpace(flashSwapDeviceSizeMb: Int) {
    val dfResult = runAdbRootShellCommand("df -Pk /data")
    val availableKb = dfResult.output
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("Filesystem") }
        .lastOrNull()
        ?.split(Regex("""\s+"""))
        ?.getOrNull(3)
        ?.toLongOrNull()

    if (dfResult.exitCode != 0 || availableKb == null) {
        println(
            "警告：无法读取 /data 剩余空间（exit=${dfResult.exitCode}），跳过空间检查。" +
                "原始输出：${dfResult.output.trim().take(200)}"
        )
        return
    }

    val availableMb = availableKb / 1024
    val requiredMb = flashSwapDeviceSizeMb + FREE_SPACE_MARGIN_MB
    check(availableMb >= requiredMb) {
        "/data 剩余空间不足：可用 ${availableMb}MB，flash swap 需要 ${flashSwapDeviceSizeMb}MB" +
            "（另留 ${FREE_SPACE_MARGIN_MB}MB 余量）。请用 -s 减小 swap 文件，" +
            "或先清理 /data 后重试，否则可能把手机写死。"
    }

    println("/data 可用 ${availableMb}MB，flash swap 文件 ${flashSwapDeviceSizeMb}MB，空间检查通过。")
}

fun enableFlashSwap(flashSwapDeviceSizeMb: Int) {
    requireEnoughDataSpace(flashSwapDeviceSizeMb)

    runAdbRootShellCommand("dd if=/dev/zero of=$FLASH_SWAP_FILE bs=1M count=$flashSwapDeviceSizeMb")
    runAdbRootShellCommand("chmod 600 $FLASH_SWAP_FILE")
    runAdbRootShellCommand("mkswap $FLASH_SWAP_FILE")
    runAdbRootShellCommand("swapon $FLASH_SWAP_FILE")
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
