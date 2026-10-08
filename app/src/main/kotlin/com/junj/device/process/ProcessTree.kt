package com.junj.device.process

import java.util.concurrent.TimeUnit

fun destroyProcessTree(process: Process) {
    val descendants = process.toHandle()
        .descendants()
        .toList()

    descendants.asReversed().forEach { handle ->
        if (handle.isAlive) handle.destroy()
    }

    process.destroy()

    if (!process.waitFor(3, TimeUnit.SECONDS)) {
        descendants.asReversed().forEach { handle ->
            if (handle.isAlive) handle.destroyForcibly()
        }

        if (process.isAlive) {
            process.destroyForcibly()
            process.waitFor()
        }
    }
}
