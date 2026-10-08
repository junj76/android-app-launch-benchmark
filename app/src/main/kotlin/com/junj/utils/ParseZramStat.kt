package com.junj.utils

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

    return ZramMmStatResult(
        origDataSize = values[0].toLong(),
        comprDataSize = values[1].toLong(),
        memUsedTotal = values[2].toLong(),
        memUsedMax = values[4].toLong()
    )
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
    return ZramBdStatResult(
        bdCount = values[0].toLong(),
        bdReads = values[1].toLong(),
        bdWrites = values[2].toLong(),
    )
}