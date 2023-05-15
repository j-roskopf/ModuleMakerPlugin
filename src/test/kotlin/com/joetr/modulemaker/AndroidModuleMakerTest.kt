package com.joetr.modulemaker

import com.joetr.modulemaker.file.FileWriter
import com.joetr.modulemaker.persistence.PreferenceService
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AndroidModuleMakerTest {

    @JvmField
    @Rule
    var folder = TemporaryFolder()

    var testState = PreferenceServiceImpl.Companion.State()

    private val fakePreferenceService = object : PreferenceService {
        override var preferenceState: PreferenceServiceImpl.Companion.State
            get() = testState
            set(value) {
                testState = value
            }
    }

    private val fileWriter = FileWriter(
        preferenceService = fakePreferenceService
    )

    private lateinit var settingsGradleFile: File

    @Before
    fun before() {
        settingsGradleFile = folder.populateSettingsGradleKtsWithFakeData()
    }

    @Test
    fun `android module created successfully`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = ANDROID,
            showErrorDialog = {
                fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert it was added to settings.gradle
        val settingsGradleFileContents = readFromFile(file = settingsGradleFile)
        assert(
            settingsGradleFileContents.contains("include(\":repository\")")
        )

        // assert readme was generated
        assert(
            // root/repository/README.md
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + readmeFile).exists()
        )

        // assert build.gradle is generated
        val buildGradleFile = File(folder.root.path + File.separator + modulePathAsFile + File.separator + buildGradleFileName)
        assert(
            // root/repository/build.gradle
            buildGradleFile.exists()
        )

        // assert package name is included in build.gradle
        val buildGradleFileContents = readFromFile(buildGradleFile)
        assert(
            buildGradleFileContents.contains(
                "    namespace = \"$testPackageName\""
            )
        )

        // assert the correct package structure is generated
        assert(
            // root/repository/build.gradle
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "src/main/kotlin/com/joetr/test").exists()
        )
    }

    @Test
    fun `when a template is set, that is used instead of default for creating build gradle`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"
        val template = "test template"

        fakePreferenceService.preferenceState.androidTemplate = template

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = ANDROID,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert build.gradle is generated
        val buildGradleFile = File(folder.root.path + File.separator + modulePathAsFile + File.separator + buildGradleFileName)
        assert(
            // root/repository/build.gradle
            buildGradleFile.exists()
        )

        // assert package name is included in build.gradle
        val buildGradleFileContents = readFromFile(buildGradleFile)
        assert(
            buildGradleFileContents.contains(
                template
            )
        )
    }

    @Test
    fun `when a template is set, the package name variable is replaced`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"

        val template = """
            this is a custom template

            android {
                namespace = "${'$'}{packageName}"
            }
        """.trimIndent()

        fakePreferenceService.preferenceState.androidTemplate = template

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = ANDROID,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert build.gradle file exists and contains the package name when using a custom template
        val buildGradleFile = File(folder.root.path + File.separator + modulePathAsFile + File.separator + buildGradleFileName)

        assert(buildGradleFile.exists())

        val buildGradleFileContents = readFromFile(buildGradleFile)

        assert(
            buildGradleFileContents.contains(
                "    namespace = \"$testPackageName\""
            )
        )
    }

    @Test
    fun `android module created successfully when using nested modules`() {
        val modulePath = ":repository:database"
        val modulePathAsFile = "repository/database"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = ANDROID,
            showErrorDialog = {
                fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert it was added to settings.gradle
        val settingsGradleFileContents = readFromFile(file = settingsGradleFile)
        assert(
            settingsGradleFileContents.contains("include(\":repository:database\")")
        )

        // assert readme was generated
        assert(
            // root/repository/database/README.md
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + readmeFile).exists()
        )

        // assert build.gradle is generated
        val buildGradleFile = File(folder.root.path + File.separator + modulePathAsFile + File.separator + buildGradleFileName)
        assert(
            // root/repository/database/build.gradle
            buildGradleFile.exists()
        )

        // assert package name is included in build.gradle
        val buildGradleFileContents = readFromFile(buildGradleFile)
        assert(
            buildGradleFileContents.contains(
                "    namespace = \"$testPackageName\""
            )
        )

        // assert the correct package structure is generated
        assert(
            // root/repository/build.gradle
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "src/main/kotlin/com/joetr/test").exists()
        )
    }

    @Test
    fun `android module created successfully with a kts build file`() {
        val modulePath = ":repository:database"
        val modulePathAsFile = "repository/database"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = ANDROID,
            showErrorDialog = {
                fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = true,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert build.gradle.kts is generated
        val buildGradleFile = File(folder.root.path + File.separator + modulePathAsFile + File.separator + buildGradleKtsFileName)
        assert(
            // root/repository/database/build.gradle
            buildGradleFile.exists()
        )
    }

    @Test
    fun `android module created successfully when include with no parenthesis`() {
        settingsGradleFile = folder.populateSettingsGradleWithFakeData()
        val modulePath = ":repository:database"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = ANDROID,
            showErrorDialog = {
                fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = true,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert it was added to settings.gradle
        val settingsGradleFileContents = readFromFile(file = settingsGradleFile)
        assert(
            settingsGradleFileContents.contains("include(\":repository:database\")")
        )
    }
}
