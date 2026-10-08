package com.junj.utils

import com.junj.appInfos
import com.junj.logger
import java.io.File
import com.junj.testRoundCount
import com.junj.timeStamp

fun printStatistics() {
    var coldLaunchCnt = 0
    var warmLaunchCnt = 0
    var hotLaunchCnt = 0
    var unknownLaunchCnt = 0

    var totalResponseLat: Long = 0
    val lmkdCnt = File("../logs/$timeStamp/logcat.log").useLines { it.count() }

    var bigTempSum: Double = 0.0
    var midTempSum: Double = 0.0
    var littleTempSum: Double = 0.0
    var socThermTempSum: Double = 0.0
    var virtualSkinTempSum: Double = 0.0

    for (result in launchApplicationResult) {
        when (result.amStartLaunchState) {
            "COLD" -> coldLaunchCnt++
            "WARM" -> warmLaunchCnt++
            "HOT" -> hotLaunchCnt++
            "UNKNOWN (0)" -> unknownLaunchCnt++
            else -> println("Error: Failed to get launch state")
        }
        totalResponseLat += result.amStartTotalTime
    }

    for (item in sampleResults) {
        bigTempSum += item.bigTemp
        midTempSum += item.midTemp
        littleTempSum += item.littleTemp
        socThermTempSum += item.socThermTemp
        virtualSkinTempSum += item.virtualSkinTemp
    }

    println("\n")
    logger.log(LogType.SUMMARY, "logs path: logs/$timeStamp/*.log")
    logger.log(LogType.SUMMARY, "coldLaunchCnt: $coldLaunchCnt, warmLaunchCNT: $warmLaunchCnt, hotLaunchCnt: $hotLaunchCnt, unknownLaunchCnt: $unknownLaunchCnt\n" +
            "avgResponseLat: ${totalResponseLat / (testRoundCount * appInfos.size)}")
    logger.log(LogType.SUMMARY, "lmkdCnt: $lmkdCnt")
    logger.log(LogType.SUMMARY,
        "bigTempAvg: ${"%.2f".format(bigTempSum / sampleResults.size)}, " +
        "midTempAvg: ${"%.2f".format(midTempSum / sampleResults.size)}, " +
        "littleTempAvg: ${"%.2f".format(littleTempSum / sampleResults.size)}, " +
        "socThermTempAvg: ${"%.2f".format(socThermTempSum / sampleResults.size)}, " +
        "virtualSkinTempAvg: ${"%.2f".format(virtualSkinTempSum / sampleResults.size)}"
    )
    logger.log(LogType.SUMMARY,
        "psiSomeDelta: ${(sampleResults[sampleResults.size - 1].psiSome - sampleResults[0].psiSome) / 1000}ms, " +
        "psiFullDelta: ${(sampleResults[sampleResults.size - 1].psiFull - sampleResults[0].psiFull) / 1000}ms"
    )
}