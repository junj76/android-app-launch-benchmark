package com.junj.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.junj.logger
import com.junj.sampleIntervalMs

data class SamplingItem(
    val sampleStamp: Int,

    // cat /proc/meminfo
    val meminfoMemTotal: Long,
    val meminfoMemFree: Long,
    val meminfoMemAvailable: Long,
    val meminfoSwapTotal: Long,
    val meminfoSwapFree: Long,
    val meminfoSwapCached: Long,

    // cat /proc/pressure/memory
    val psiSome: Long,
    val psiFull: Long,

    // vmstat
    val vmstatR: Int,
    val vmstatB: Int,
    val vmstatSwpd: Long,
    val vmstatFree: Long,
    val vmstatBuff: Long,
    val vmstatCache: Long,
    val vmstatSi: Long,
    val vmstatSo: Long,
    val vmstatBi: Long,
    val vmstatBo: Long,
    val vmstatIn: Long,
    val vmstatCs: Long,
    val vmstatUs: Int,
    val vmstatSy: Int,
    val vmstatId: Int,
    val vmstatWa: Int,

    // dumpsys thermalservice
    val bigTemp: Double,
    val midTemp: Double,
    val littleTemp: Double,
    val socThermTemp: Double,
    val virtualSkinTemp: Double,

    // cat /sys/block/zram0/mm_stat
    val zramOrigDataSize: Long,
    val zramComprDataSize: Long,
    val zramMemUsedTotal: Long,
    val zramMemUsedMax: Long,

    // cat /sys/block/zram0/bd_stat
    val zramBdCount: Long,
    val zramBdReads: Long,
    val zramBdWrites: Long,

    // cat /sys/kernel/debug/zswap/xxx
    val zswapStoredPages: Long,
    val zswapPoolTotalSize: Long,
    val zswapPoolLimitHit: Long,
    val zswapWrittenBackPages: Long,
)

val sampleResults = ArrayList<SamplingItem>()
var sampleStamp: Int = 0

fun doSamplingJob() {
    val catProcMemInfoCommandResult = runAdbShellCommand("""cat /proc/meminfo""")
    val catProcPressureMemoryCommandResult = runAdbRootShellCommand("""cat /proc/pressure/memory""")
    val vmstatCommandResult = runAdbRootShellCommand("""vmstat""")
    val vmstatResult = parseVmstat(vmstatCommandResult.output)
    val dumpsysThermalCommandResult = runAdbRootShellCommand("""dumpsys thermalservice""")
    val catZramMmStatCommandResult = runAdbRootShellCommand("""cat /sys/block/zram0/mm_stat""")
    val catZramMmStatResult = parseZramMmStatResult(catZramMmStatCommandResult.output)
    val catZramBdStatCommandResult = runAdbRootShellCommand("""cat /sys/block/zram0/bd_stat""")
    val catZramBdStatResult = parseZramBdStatResult(catZramBdStatCommandResult.output)

    sampleResults += SamplingItem(
        sampleStamp = sampleStamp,

        meminfoMemTotal = regexFindField(CommandType.CAT_PROC_MEMINFO, ResultField.MEM_TOTAL, catProcMemInfoCommandResult.output)?.toLongOrNull() ?: 0,
        meminfoMemFree = regexFindField(CommandType.CAT_PROC_MEMINFO, ResultField.MEM_FREE, catProcMemInfoCommandResult.output)?.toLongOrNull() ?: 0,
        meminfoMemAvailable = regexFindField(CommandType.CAT_PROC_MEMINFO, ResultField.MEM_AVAILABLE, catProcMemInfoCommandResult.output)?.toLongOrNull() ?: 0,
        meminfoSwapTotal = regexFindField(CommandType.CAT_PROC_MEMINFO, ResultField.SWAP_TOTAL, catProcMemInfoCommandResult.output)?.toLongOrNull() ?: 0,
        meminfoSwapFree = regexFindField(CommandType.CAT_PROC_MEMINFO, ResultField.SWAP_FREE, catProcMemInfoCommandResult.output)?.toLongOrNull() ?: 0,
        meminfoSwapCached = regexFindField(CommandType.CAT_PROC_MEMINFO, ResultField.SWAP_CACHED, catProcMemInfoCommandResult.output)?.toLongOrNull() ?: 0,

        psiSome = regexFindField(CommandType.CAT_PROC_PRESSURE_MEMORY, ResultField.PSI_SOME_TOTAL, catProcPressureMemoryCommandResult.output)?.toLongOrNull() ?: 0,
        psiFull = regexFindField(CommandType.CAT_PROC_PRESSURE_MEMORY, ResultField.PSI_FULL_TOTAL, catProcPressureMemoryCommandResult.output)?.toLongOrNull() ?: 0,

        vmstatR = vmstatResult?.vmstatR ?: 0,           // Runnable processes
        vmstatB = vmstatResult?.vmstatB ?: 0,           // Processes blocked on I/O
        vmstatSwpd = vmstatResult?.vmstatSwpd ?: 0L,    // Virtual memory used (KiB)
        vmstatFree = vmstatResult?.vmstatFree ?: 0L,    // Idle memory (KiB)
        vmstatBuff = vmstatResult?.vmstatBuff ?: 0L,    // Buffer memory (KiB)
        vmstatCache = vmstatResult?.vmstatCache ?: 0L,  // Cache memory (KiB)
        vmstatSi = vmstatResult?.vmstatSi ?: 0L,        // Memory swapped in per second (KiB/s)
        vmstatSo = vmstatResult?.vmstatSo ?: 0L,        // Memory swapped out per second (KiB/s)
        vmstatBi = vmstatResult?.vmstatBi ?: 0L,        // Blocks received from devices per second
        vmstatBo = vmstatResult?.vmstatBo ?: 0L,        // Blocks sent to devices per second
        vmstatIn = vmstatResult?.vmstatIn ?: 0L,        // Interrupts per second
        vmstatCs = vmstatResult?.vmstatCs ?: 0L,        // Context switches per second
        vmstatUs = vmstatResult?.vmstatUs ?: 0,         // User CPU time (%)
        vmstatSy = vmstatResult?.vmstatSy ?: 0,         // Kernel CPU time (%)
        vmstatId = vmstatResult?.vmstatId ?: 0,         // Idle CPU time (%)
        vmstatWa = vmstatResult?.vmstatWa ?: 0,         // CPU time waiting for I/O (%)

        bigTemp = regexFindField(CommandType.DUMPSYS_THERMALSERVICE, ResultField.BIG_TEMP, dumpsysThermalCommandResult.output)?.toDoubleOrNull() ?: 0.0,
        midTemp = regexFindField(CommandType.DUMPSYS_THERMALSERVICE, ResultField.MID_TEMP, dumpsysThermalCommandResult.output)?.toDoubleOrNull() ?: 0.0,
        littleTemp = regexFindField(CommandType.DUMPSYS_THERMALSERVICE, ResultField.LITTLE_TEMP, dumpsysThermalCommandResult.output)?.toDoubleOrNull() ?: 0.0,
        socThermTemp = regexFindField(CommandType.DUMPSYS_THERMALSERVICE, ResultField.SOC_THERM_TEMP, dumpsysThermalCommandResult.output)?.toDoubleOrNull() ?: 0.0,
        virtualSkinTemp = regexFindField(CommandType.DUMPSYS_THERMALSERVICE, ResultField.VIRTUAL_SKIN_TEMP, dumpsysThermalCommandResult.output)?.toDoubleOrNull() ?: 0.0,

        zramOrigDataSize = catZramMmStatResult?.origDataSize ?: 0,
        zramComprDataSize = catZramMmStatResult?.comprDataSize ?: 0,
        zramMemUsedTotal = catZramMmStatResult?.memUsedTotal ?: 0,
        zramMemUsedMax = catZramMmStatResult?.memUsedMax ?: 0,

        zramBdCount = catZramBdStatResult?.bdCount ?: 0,    // Amount of data currently written to the backing device (4KB).
        zramBdReads = catZramBdStatResult?.bdReads ?: 0,    // Number of reads from the backing device (4KB).
        zramBdWrites = catZramBdStatResult?.bdWrites ?: 0,  // Number of writes to the backing device (4KB).

        zswapStoredPages = runAdbRootShellCommand("cat /sys/kernel/debug/zswap/stored_pages").output.trim().toLongOrNull() ?: 0L,
        zswapPoolTotalSize = runAdbRootShellCommand("cat /sys/kernel/debug/zswap/pool_total_size").output.trim().toLongOrNull() ?: 0L,
        zswapPoolLimitHit = runAdbRootShellCommand("cat /sys/kernel/debug/zswap/pool_limit_hit").output.trim().toLongOrNull() ?: 0L,
        zswapWrittenBackPages = runAdbRootShellCommand("cat /sys/kernel/debug/zswap/written_back_pages").output.trim().toLongOrNull() ?: 0L,
    )
    logger.log(LogType.SAMPLE, sampleResults[sampleStamp])
    sampleStamp++
}

fun CoroutineScope.startSamplingJob(): Job = launch(Dispatchers.IO) {
    try {
        println("Starting sampling job")
        while (isActive) {
            doSamplingJob()
            delay(sampleIntervalMs.milliseconds)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
