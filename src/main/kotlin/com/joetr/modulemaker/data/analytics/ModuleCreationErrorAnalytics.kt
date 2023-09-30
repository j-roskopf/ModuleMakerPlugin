package com.joetr.modulemaker.data.analytics

import kotlinx.serialization.Serializable

@Serializable
data class ModuleCreationErrorAnalytics(
    val message: String
)
