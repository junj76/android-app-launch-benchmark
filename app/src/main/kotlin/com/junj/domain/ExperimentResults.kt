package com.junj.domain

import com.junj.domain.launch.LaunchApplicationItem
import com.junj.domain.metrics.SamplingItem
import com.junj.domain.survival.AppSurvivalResult

class ExperimentResults(
    val launches: MutableList<LaunchApplicationItem> = ArrayList(),
    val samples: MutableList<SamplingItem> = ArrayList(),
    val survivals: MutableList<AppSurvivalResult> = ArrayList(),
)
