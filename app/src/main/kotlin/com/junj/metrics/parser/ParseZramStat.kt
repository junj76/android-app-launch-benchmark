package com.junj.metrics.parser

data class ZramMmStatResult(
    val origDataSize: Long, //
    val comprDataSize: Long,
    val memUsedTotal: Long,
    val memUsedMax: Long,
)

data class ZramBdStatResult(
    val bdCount: Long,
    val bdReads: Long,
    val bdWrites: Long,
)

fun parseZramMmStatResult(output: String): ZramMmStatResult? {
    val values = output
        .lineSequence()
        .firstOrNull(){ it.isNotEmpty() }
        ?.trim()
        ?.split(Regex("""\s+"""))
        ?: return null

    if (values.size < 9)
        return null

    // 拿到的不是数字（比如设备端返回了一行报错）时返回 null，
    // 不要让一个异常把整个采样线程干掉、导致一个样本都写不出来。
    return runCatching {
        ZramMmStatResult(
            origDataSize = values[0].toLong(),
            comprDataSize = values[1].toLong(),
            memUsedTotal = values[2].toLong(),
            memUsedMax = values[4].toLong()
        )
    }.getOrNull()
}

fun parseZramBdStatResult(output: String): ZramBdStatResult? {
    val values = output
        .lineSequence()
        .firstOrNull(){ it.isNotEmpty() }
        ?.trim()
        ?.split(Regex("""\s+"""))
        ?: return null
    if (values.size < 3)
        return null

    return runCatching {
        ZramBdStatResult(
            bdCount = values[0].toLong(),
            bdReads = values[1].toLong(),
            bdWrites = values[2].toLong(),
        )
    }.getOrNull()
}
