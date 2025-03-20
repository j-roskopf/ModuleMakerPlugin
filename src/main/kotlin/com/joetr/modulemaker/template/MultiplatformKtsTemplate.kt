package com.joetr.modulemaker.template

object MultiplatformKtsTemplate {
    val data = """
        plugins {
            kotlin("multiplatform")
            kotlin("plugin.compose")
            id("com.android.library")
            id("org.jetbrains.compose")
        }

        version = "1.0-SNAPSHOT"

        kotlin {
            androidTarget()
            jvm("desktop")
            js {
                browser()
                useEsModules()
            }
            wasmJs { browser() }

            listOf(
                iosX64(),
                iosArm64(),
                iosSimulatorArm64()
            ).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = "shared"
                    isStatic = true
                }
            }

            applyDefaultHierarchyTemplate()

            sourceSets {
                all {
                    languageSettings {
                        optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                    }
                }

                commonMain.dependencies {
                    implementation(compose.runtime)
                    implementation(compose.foundation)
                    implementation(compose.material)
                    implementation(compose.components.resources)
                }

                androidMain.dependencies {

                }

                val jsWasmMain by creating {
                    dependsOn(commonMain.get())
                }

                val jsMain by getting {
                    dependsOn(jsWasmMain)
                }

                val wasmJsMain by getting {
                    dependsOn(jsWasmMain)
                }

                val desktopMain by getting
                desktopMain.dependencies {
                    implementation(compose.desktop.common)

                }
                val desktopTest by getting
                desktopTest.dependencies {
                    implementation(compose.desktop.currentOs)
                    implementation(compose.desktop.uiTestJUnit4)
                }
            }
        }
    """.trimIndent()
}
