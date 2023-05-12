package com.joetr.modulemaker.template

object AndroidModuleTemplate {
    val data = """
        apply plugin: "com.android.library"
        apply plugin: "kotlin-android"

        android {
            namespace = "${'$'}{packageName}"
        }

        dependencies {

        }
    """.trimIndent()
}
