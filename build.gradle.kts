import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.compose) // Gradle Compose Compiler Plugin
    alias(libs.plugins.spotless)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

buildscript {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven {
            url = uri("https://www.jetbrains.com/intellij-repository/releases")
        }
    }
}

// Configure project's dependencies
repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    intellijPlatform {
        defaultRepositories()
    }
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

apply(
    from = "gradle/spotless.gradle"
)

sourceSets {
    create("uiTest") {
        kotlin.srcDir("src/uiTest/kotlin")
    }
}

dependencies {
    implementation(libs.freemarker)
    implementation(libs.serialization)
    implementation(libs.segment)

    testImplementation(libs.junit)

    "uiTestImplementation"(kotlin("stdlib"))
    "uiTestImplementation"(libs.remoteRobot)
    "uiTestImplementation"(libs.remoteRobotFixtures)
    "uiTestImplementation"(libs.junit)
    "uiTestImplementation"("com.squareup.okhttp3:okhttp:4.12.0")
    // The Compose compiler plugin applies to all source sets; uiTest needs the runtime on its classpath
    "uiTestImplementation"("org.jetbrains.compose.runtime:runtime-desktop:1.7.3")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(properties("platformType").get(), properties("platformVersion").get())

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(properties("platformPlugins").map { it.split(',') })

        // Compose support dependencies
        @Suppress("UnstableApiUsage")
        composeUI()

        // instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion").get()
        sinceBuild = properties("pluginSinceBuild").get()
        // untilBuild = properties("pluginUntilBuild").get()

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML
                )
            }
        }
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }
}

tasks.register<Test>("uiTest") {
    description = "Runs UI tests against a running IDE instance"
    group = "verification"
    testClassesDirs = sourceSets["uiTest"].output.classesDirs
    classpath = sourceSets["uiTest"].runtimeClasspath
    systemProperty("robot-server.port", System.getProperty("robot-server.port", "8082"))
    doNotTrackState("UI tests are not cacheable")
    // Gson (used by the remote-robot client) reflectively accesses private fields such as
    // Throwable.detailMessage when deserializing error responses from the robot server.
    // JDK 17+ JPMS blocks this by default; --add-opens restores access.
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // Echo test stdout/stderr directly to the Gradle console so diagnostic println calls
    // (accessibility tree dumps, screenshots, component class lists) are visible live.
    testLogging {
        showStandardStreams = true
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                        // Skip the "Trust Project?" dialog that blocks the IDE frame from appearing
                        "-Didea.trust.all.projects=true",
                        "-Didea.initially.ask.config=never",
                        // Suppress Tip of the Day, What's New, and other first-run dialogs
                        "-Dide.show.tips.on.startup.default.value=false",
                        "-Didea.is.internal=false",
                        "-Dide.no.platform.update=true",
                        // Skip import settings dialog
                        "-Didea.config.imported.in.current.session=true",
                        // Force the Swing menu bar so remote-robot can find menu items.
                        // Without this, macOS uses the native system menu bar which is
                        // invisible to the Swing component hierarchy that remote-robot inspects.
                        "-Dapple.laf.useScreenMenuBar=false"
                    )
                }
                // Open the test project so settings.gradle.kts is available for module creation
                args(layout.projectDirectory.dir("src/uiTest/testProject").asFile.absolutePath)
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = properties("pluginVersion").get()

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild").get()
            // untilBuild = properties("pluginUntilBuild").get()
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}
