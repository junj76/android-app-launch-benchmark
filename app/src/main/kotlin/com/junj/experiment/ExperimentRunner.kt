package com.junj.experiment

import com.junj.config.ExperimentConfig
import com.junj.device.adb.AdbExecutor
import com.junj.domain.ExperimentResults
import com.junj.domain.app.ApplicationInfo
import com.junj.domain.launch.LaunchApplicationItem
import com.junj.metrics.parser.CommandType
import com.junj.metrics.parser.ResultField
import com.junj.metrics.parser.regexFindField
import com.junj.output.logging.LogType
import com.junj.output.logging.Logger
import java.lang.Thread.sleep

class ExperimentRunner(
    private val adb: AdbExecutor,
    private val apps: List<ApplicationInfo>,
    private val results: ExperimentResults,
    private val logger: Logger,
    private val config: ExperimentConfig,
) {
    fun runWarmUpRound() {
        println("===Warm up round START!===")
        for (app in apps) {
            println(adb.shell("am start -W -n ${app.componentName}").output)
            sleep(15_000)
        }
        println("===Warm up round END!===")
    }

    fun runTestRound(round: Int) {
        for (app in apps) {
            val launchStartNs = System.nanoTime()
            val nextLaunchNs = launchStartNs + config.launchIntervalMs * 1_000_000L
            adb.rootShell("dumpsys gfxinfo ${app.packageName} reset")
            val amStartResult = adb.shell("am start -W -n ${app.componentName}")
            sleepUntil(launchStartNs + config.collectDelayMs * 1_000_000L)

            val result = LaunchApplicationItem(
                amStartRound = round,
                amStartAppName = app.name,
                amStartLaunchState = regexFindField(CommandType.AM_START, ResultField.LAUNCH_STATE, amStartResult.output) ?: "",
                amStartTotalTime = regexFindField(CommandType.AM_START, ResultField.TOTAL_TIME, amStartResult.output)?.toLongOrNull() ?: 0,
                amStartWaitTime = regexFindField(CommandType.AM_START, ResultField.WAIT_TIME, amStartResult.output)?.toLongOrNull() ?: 0,
                amStartStatus = regexFindField(CommandType.AM_START, ResultField.STATUS, amStartResult.output) ?: "",
                dumpsysGfxInfoJankyFrames = 0.0,
                dumpsysGfxInfoP50RenderLat = 0,
                dumpsysGfxInfoP90RenderLat = 0,
                dumpsysGfxInfoP95RenderLat = 0,
                dumpsysGfxInfoP99RenderLat = 0,
                dumpsysMemInfoTotalPss = 0,
            )
            results.launches += result
            logger.log(LogType.LAUNCH, result)
            sleepUntil(nextLaunchNs)
        }
    }

    private fun sleepUntil(deadlineNs: Long) {
        while (true) {
            val remainingNs = deadlineNs - System.nanoTime()
            if (remainingNs <= 0) return
            sleep(remainingNs / 1_000_000L, (remainingNs % 1_000_000L).toInt())
        }
    }
}
