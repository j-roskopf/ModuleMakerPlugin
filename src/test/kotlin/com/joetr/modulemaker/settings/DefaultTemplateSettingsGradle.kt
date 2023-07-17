package com.joetr.modulemaker.settings

object DefaultTemplateSettingsGradle {
    val data = """
        pluginManagement {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                google()
                mavenCentral()
            }
        }
        rootProject.name = "ModuleMakerTest"
        include ':app'
    """.trimIndent()
}
