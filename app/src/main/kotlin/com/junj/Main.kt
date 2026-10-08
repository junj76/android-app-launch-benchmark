package com.junj

import com.junj.utils.*
import com.junj.utils.globalAppInfos
import java.lang.Thread.sleep
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import java.io.File

fun createLogAndResultFiles() {
    File("../logs/$timeStamp").mkdir()
    File("../logs/$timeStamp/sample.log").createNewFile()
    File("../logs/$timeStamp/launch.log").createNewFile()
    File("../logs/$timeStamp/sample.csv").createNewFile()
    File("../logs/$timeStamp/launch.csv").createNewFile()
    File("../logs/$timeStamp/summary.txt").createNewFile()
}

private fun sleepUntil(deadlineNs: Long) {
    while (true) {
        val remainingNs = deadlineNs - System.nanoTime()
        if (remainingNs <= 0) return
        val millis = remainingNs / 1_000_000L
        val nanos = (remainingNs % 1_000_000L).toInt()
        sleep(millis, nanos)
    }
}

fun runWarmUpRound() {
    println("===Warm up round START!===")
    for (app in appInfos) {
        println(runAdbShellCommand("""am start -W -n ${app.componentName}""").output)
        sleep(15000)
    }
    println("===Warm up round END!===")
}

fun runTestRound(round: Int) {
    val launchIntervalMs = 15_000L
    val collectDelayMs = 10_000L
    for ((index, app) in appInfos.withIndex()) {
        val launchStartNs = System.nanoTime()
        val nextLaunchNs = launchStartNs + launchIntervalMs * 1_000_000L
        runAdbRootShellCommand("""dumpsys gfxinfo ${app.packageName} reset""")
        val amStartCommandResult = runAdbShellCommand("""am start -W -n ${app.componentName}""")
        sleepUntil(launchStartNs + collectDelayMs * 1_000_000L)
//        simulateUserClickEvents(app)
//        val dumpsysMemInfoCommandResult = runAdbRootShellCommand("""dumpsys meminfo ${app.packageName}""")
//        val dumpsysGfxInfoCommandResult = runAdbRootShellCommand("""dumpsys gfxinfo ${app.packageName}""")

        launchApplicationResult += LaunchApplicationItem(
            amStartRound = round,
            amStartAppName = app.name,
            amStartLaunchState = regexFindField(CommandType.AM_START, ResultField.LAUNCH_STATE, amStartCommandResult.output) ?: "",
            amStartTotalTime = regexFindField(CommandType.AM_START, ResultField.TOTAL_TIME, amStartCommandResult.output)?.toLongOrNull() ?: 0,
            amStartWaitTime = regexFindField(CommandType.AM_START, ResultField.WAIT_TIME, amStartCommandResult.output)?.toLongOrNull() ?: 0,
            amStartStatus = regexFindField(CommandType.AM_START, ResultField.STATUS, amStartCommandResult.output) ?: "",

            dumpsysGfxInfoJankyFrames = 0.0, //regexFindField(CommandType.DUMPSYS_GFXINFO, ResultField.JANKY_FRAMES_PCT, dumpsysGfxInfoCommandResult.output)?.toDoubleOrNull() ?: 0.0,
            dumpsysGfxInfoP50RenderLat =  0, // regexFindField(CommandType.DUMPSYS_GFXINFO, ResultField.P50_RENDER_LAT, dumpsysGfxInfoCommandResult.output)?.toIntOrNull() ?: 0,
            dumpsysGfxInfoP90RenderLat = 0, //regexFindField(CommandType.DUMPSYS_GFXINFO, ResultField.P90_RENDER_LAT, dumpsysGfxInfoCommandResult.output)?.toIntOrNull() ?: 0,
            dumpsysGfxInfoP95RenderLat = 0, // regexFindField(CommandType.DUMPSYS_GFXINFO, ResultField.P95_RENDER_LAT, dumpsysGfxInfoCommandResult.output)?.toIntOrNull() ?: 0,
            dumpsysGfxInfoP99RenderLat = 0, //regexFindField(CommandType.DUMPSYS_GFXINFO, ResultField.P99_RENDER_LAT, dumpsysGfxInfoCommandResult.output)?.toIntOrNull() ?: 0,

            dumpsysMemInfoTotalPss =  0, //regexFindField(CommandType.DUMPSYS_MEMINFO, ResultField.TOTAL_PSS, dumpsysMemInfoCommandResult.output)?.toIntOrNull() ?: 0,
        )
        logger.log(LogType.LAUNCH, launchApplicationResult.getOrNull(index + appInfos.size * (round -1)))
        sleepUntil(nextLaunchNs)
    }
}

const val testRoundCount = 5
const val sampleIntervalMs = 10_000
const val flashSwapDevSize = 1024 * 4 // in MB

var type: Int = -1
var appSetNumber: Int = -1
lateinit var timeStamp: String
lateinit var logcatFile: File
lateinit var logger: Logger
lateinit var appInfos: List<ApplicationInfo>

fun main(args: Array<String>) = runBlocking {
    rebootDevice()
    timeStamp = getTimeStamp()
    parseArgs(args)
    createLogAndResultFiles()
    logger = Logger()
    initSwapByType(type)
    appInfos = globalAppInfos.filter { it.name in appNameSet[appSetNumber] }

//    runWarmUpRound()

    val testStartNs = System.nanoTime()
    val samplingJob = startSamplingJob()
    val logcatProcess = startLogcatProcess()

    try {
        for (i in 1..testRoundCount) {
            runTestRound(i)
        }
    } finally {
        val testEndNs = System.nanoTime()
        val elapsedNs = testEndNs - testStartNs
        samplingJob.cancelAndJoin()
        destroyProcessTree(logcatProcess)
        printStatistics()
        logger.log(LogType.SUMMARY, "Test round runtime: ${elapsedNs / 1_000_000_000}s")
        saveCsv()
        logger.close()
    }
}
