rootProject.name = "Module Maker"

pluginManagement {
    plugins {
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}
