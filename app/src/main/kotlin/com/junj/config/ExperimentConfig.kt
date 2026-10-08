package com.junj.config

import java.io.File

const val DEFAULT_TEST_ROUND_COUNT = 5
const val DEFAULT_SAMPLE_INTERVAL_MS = 10_000L
const val DEFAULT_LAUNCH_INTERVAL_MS = 15_000L
const val DEFAULT_COLLECT_DELAY_MS = 10_000L

/**
 * flash swap 文件大小（MB）。
 *
 * 注意（Windows 分支的本地改动）：上游默认是 4096MB，但当前测试机在 4GB 时
 * 直接把 /data 写满、系统死机过一次，因此本地改为 1024MB（旧版扁平布局时期的
 * 已知可用值）。需要更大时用命令行 `-s <MB>` 覆盖，或把这里改回 1024 * 4。
 */
const val DEFAULT_FLASH_SWAP_DEVICE_SIZE_MB = 1024

data class ExperimentConfig(
    val swapType: Int,
    val appSetNumber: Int,
    val timestamp: String,
    val outputDirectory: File,
    val testRoundCount: Int = DEFAULT_TEST_ROUND_COUNT,
    val sampleIntervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS,
    val launchIntervalMs: Long = DEFAULT_LAUNCH_INTERVAL_MS,
    val collectDelayMs: Long = DEFAULT_COLLECT_DELAY_MS,
    val flashSwapDeviceSizeMb: Int = DEFAULT_FLASH_SWAP_DEVICE_SIZE_MB,
)
