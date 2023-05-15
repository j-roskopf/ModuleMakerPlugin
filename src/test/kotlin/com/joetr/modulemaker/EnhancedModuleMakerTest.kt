package com.joetr.modulemaker

import com.joetr.modulemaker.file.FileWriter
import com.joetr.modulemaker.persistence.PreferenceService
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EnhancedModuleMakerTest {

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
    fun `enhanced module created successfully`() {
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
            enhancedModuleCreationStrategy = true,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert it was added to settings.gradle
        val settingsGradleFileContents = readFromFile(file = settingsGradleFile)
        assert(
            settingsGradleFileContents.contains("include(\":repository:api\")")
        )
        assert(
            settingsGradleFileContents.contains("include(\":repository:glue\")")
        )
        assert(
            settingsGradleFileContents.contains("include(\":repository:impl\")")
        )

        // assert readme was generated in the api module
        assert(
            // root/repository/api/README.md
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + readmeFile).exists()
        )

        // assert build.gradle is generated for all 3 modules
        val buildGradleFileApi = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + buildGradleFileName)
        val buildGradleFileGlue = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + buildGradleFileName)
        val buildGradleFileImpl = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + buildGradleFileName)
        assert(buildGradleFileApi.exists())
        assert(buildGradleFileGlue.exists())
        assert(buildGradleFileImpl.exists())

        // assert package name is included in build.gradle
        val buildGradleApiFileContents = readFromFile(buildGradleFileApi)
        val buildGradleGlueFileContents = readFromFile(buildGradleFileGlue)
        val buildGradleImplFileContents = readFromFile(buildGradleFileImpl)
        assert(
            buildGradleApiFileContents.contains(
                "    namespace = \"$testPackageName.api\""
            )
        )
        assert(
            buildGradleGlueFileContents.contains(
                "    namespace = \"$testPackageName.glue\""
            )
        )
        assert(
            buildGradleImplFileContents.contains(
                "    namespace = \"$testPackageName.impl\""
            )
        )

        // assert the correct package structure is generated
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api/src/main/kotlin/com/joetr/test/api").exists()
        )
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue/src/main/kotlin/com/joetr/test/glue").exists()
        )
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl/src/main/kotlin/com/joetr/test/impl").exists()
        )
    }

    @Test
    fun `when a template is set, the package name variable is replaced for each module created`() {
        val modulePath = ":repository"
        val modulePathAsFile = "repository"

        val template = """
            this is a custom template

            android {
                namespace = "${'$'}{packageName}"
            }
        """.trimIndent()

        fakePreferenceService.preferenceState.apiTemplate = template
        fakePreferenceService.preferenceState.glueTemplate = template
        fakePreferenceService.preferenceState.implTemplate = template

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
            enhancedModuleCreationStrategy = true,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert build.gradle is generated for all 3 modules
        val buildGradleFileApi = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + buildGradleFileName)
        val buildGradleFileGlue = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + buildGradleFileName)
        val buildGradleFileImpl = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + buildGradleFileName)
        assert(buildGradleFileApi.exists())
        assert(buildGradleFileGlue.exists())
        assert(buildGradleFileImpl.exists())

        // assert package name is included in build.gradle
        val buildGradleApiFileContents = readFromFile(buildGradleFileApi)
        val buildGradleGlueFileContents = readFromFile(buildGradleFileGlue)
        val buildGradleImplFileContents = readFromFile(buildGradleFileImpl)
        assert(
            buildGradleApiFileContents.contains(
                "    namespace = \"$testPackageName.api\""
            )
        )
        assert(
            buildGradleGlueFileContents.contains(
                "    namespace = \"$testPackageName.glue\""
            )
        )
        assert(
            buildGradleImplFileContents.contains(
                "    namespace = \"$testPackageName.impl\""
            )
        )
    }

    @Test
    fun `when a template is set, that is used instead of default for creating build gradle`() {
        val modulePath = ":repository:database"
        val modulePathAsFile = "repository/database"

        val template = "test template"

        fakePreferenceService.preferenceState.apiTemplate = template
        fakePreferenceService.preferenceState.glueTemplate = template
        fakePreferenceService.preferenceState.implTemplate = template

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
            enhancedModuleCreationStrategy = true,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName
        )

        // assert build.gradle is generated for all 3 modules
        val buildGradleFileApi = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + buildGradleFileName)
        val buildGradleFileGlue = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + buildGradleFileName)
        val buildGradleFileImpl = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + buildGradleFileName)
        assert(buildGradleFileApi.exists())
        assert(buildGradleFileGlue.exists())
        assert(buildGradleFileImpl.exists())

        // assert package name is included in build.gradle
        val buildGradleApiFileContents = readFromFile(buildGradleFileApi)
        val buildGradleGlueFileContents = readFromFile(buildGradleFileGlue)
        val buildGradleImplFileContents = readFromFile(buildGradleFileImpl)
        assert(
            buildGradleApiFileContents.contains(
                template
            )
        )
        assert(
            buildGradleGlueFileContents.contains(
                template
            )
        )
        assert(
            buildGradleImplFileContents.contains(
                template
            )
        )
    }
}
