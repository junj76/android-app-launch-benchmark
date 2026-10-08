package com.junj.domain

import com.junj.domain.launch.LaunchApplicationItem
import com.junj.domain.metrics.SamplingItem

class ExperimentResults(
    val launches: MutableList<LaunchApplicationItem> = ArrayList(),
    val samples: MutableList<SamplingItem> = ArrayList(),
)
