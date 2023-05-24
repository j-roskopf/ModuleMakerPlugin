package com.joetr.modulemaker

import com.joetr.modulemaker.file.FileWriter
import com.joetr.modulemaker.persistence.PreferenceService
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import com.joetr.modulemaker.template.GitIgnoreTemplate
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KotlinModuleMakerTest {

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
    fun `kotlin module created successfully`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName,
            addReadme = true,
            addGitIgnore = false
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
        assert(
            // root/repository/build.gradle
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + buildGradleFileName).exists()
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
        fakePreferenceService.preferenceState.kotlinTemplate = template

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false
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
    fun `kotlin module created successfully with a kts build file named after module`() {
        val modulePath = ":repository:database"
        val modulePathAsFile = "repository/database"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = true,
            gradleFileFollowModule = true,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false
        )

        // assert build.gradle.kts is generated
        val buildGradleFile =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "database.gradle.kts")
        assert(
            // root/repository/database/build.gradle
            buildGradleFile.exists()
        )
    }

    @Test
    fun `kotlin module created successfully with a gradle build file named after module`() {
        val modulePath = ":repository:database"
        val modulePathAsFile = "repository/database"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = true,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false
        )

        // assert build.gradle.kts is generated
        val buildGradleFile =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "database.gradle")
        assert(
            // root/repository/database/build.gradle
            buildGradleFile.exists()
        )
    }

    @Test
    fun `readme is not added to kotlin module when setting is disabled`() {
        val modulePath = ":repository:database"
        val modulePathAsFile = "repository/database"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = true,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false
        )

        // assert readme is NOT generated
        val buildGradleFile =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "README.md")
        assert(
            buildGradleFile.exists().not()
        )
    }

    @Test
    fun `readme is added to kotlin module when setting is enabled`() {
        val modulePath = ":repository:database"
        val modulePathAsFile = "repository/database"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = true,
            packageName = testPackageName,
            addReadme = true,
            addGitIgnore = false
        )

        // assert readme is generated
        val buildGradleFile =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "README.md")
        assert(
            buildGradleFile.exists()
        )
    }

    @Test
    fun `gitignore is not generated in kotlin module when setting is disabled`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false
        )

        // assert gitignore was not generated
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + File.separator + ".gitignore").exists()
                .not()
        )
    }

    @Test
    fun `gitignore is generated in kotlin module with default settings when setting is enabled`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = true
        )

        // assert gitignore was generated and has the expected contents
        val gitignoreFile = File(folder.root.path + File.separator + modulePathAsFile + File.separator + File.separator + ".gitignore")
        val gitignoreFileContents = readFromFile(file = gitignoreFile)
        Assert.assertEquals(
            GitIgnoreTemplate.data,
            gitignoreFileContents.joinToString("\n")
        )
    }

    @Test
    fun `gitignore is generated in kotlin module with custom settings when setting is enabled`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"

        val template = """
            this is a custom template
        """.trimIndent()

        fakePreferenceService.preferenceState.gitignoreTemplate = template

        fileWriter.createModule(
            settingsGradleFile = settingsGradleFile,
            workingDirectory = folder.root,
            modulePathAsString = modulePath,
            moduleType = KOTLIN,
            showErrorDialog = {
                Assert.fail("No errors should be thrown")
            },
            showSuccessDialog = {
                assert(true)
            },
            enhancedModuleCreationStrategy = false,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = true
        )

        // assert gitignore was generated and has the expected contents
        val gitignoreFile = File(folder.root.path + File.separator + modulePathAsFile + File.separator + File.separator + ".gitignore")
        val gitignoreFileContents = readFromFile(file = gitignoreFile)
        Assert.assertEquals(
            template,
            gitignoreFileContents.joinToString("\n")
        )
    }
}
