package com.joetr.modulemaker.data.analytics

import kotlinx.serialization.Serializable

@Serializable
data class ModuleCreationAnalytics(
    val moduleType: String,
    val threeModule: Boolean,
    val addGitIgnore: Boolean,
    val addReadme: Boolean,
    val gradleNameToFollow: Boolean,
    val useKts: Boolean
)
