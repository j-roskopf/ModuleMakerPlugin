package com.joetr.modulemaker

import com.joetr.modulemaker.file.FileWriter
import com.joetr.modulemaker.persistence.PreferenceService
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import com.joetr.modulemaker.template.GitIgnoreTemplate
import org.junit.Assert
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
            packageName = testPackageName,
            addReadme = true,
            addGitIgnore = false,
            rootPathString = folder.root.toString()
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
        val buildGradleFileApi =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + buildGradleFileName)
        val buildGradleFileGlue =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + buildGradleFileName)
        val buildGradleFileImpl =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + buildGradleFileName)
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
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false,
            rootPathString = folder.root.toString()
        )

        // assert build.gradle is generated for all 3 modules
        val buildGradleFileApi =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + buildGradleFileName)
        val buildGradleFileGlue =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + buildGradleFileName)
        val buildGradleFileImpl =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + buildGradleFileName)
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
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false,
            rootPathString = folder.root.toString()
        )

        // assert build.gradle is generated for all 3 modules
        val buildGradleFileApi =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + buildGradleFileName)
        val buildGradleFileGlue =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + buildGradleFileName)
        val buildGradleFileImpl =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + buildGradleFileName)
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

    @Test
    fun `readme is not generated in enhanced module when setting is disabled`() {
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
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false,
            rootPathString = folder.root.toString()
        )

        // assert readme was not generated in the api module
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + readmeFile).exists()
                .not()
        )
    }

    @Test
    fun `gitignore is not generated in enhanced module when setting is disabled`() {
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
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = false,
            rootPathString = folder.root.toString()
        )

        // assert gitignore was not generated in any of the modules module
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + ".gitignore").exists()
                .not()
        )
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + ".gitignore").exists()
                .not()
        )
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + ".gitignore").exists()
                .not()
        )
    }

    @Test
    fun `gitignore is generated in enhanced module with default settings when setting is enabled`() {
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
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = true,
            rootPathString = folder.root.toString()
        )

        val apiGitIgnore = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + ".gitignore")
        val apiGitignoreFileContents = readFromFile(file = apiGitIgnore)
        val glueGitIgnore = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + ".gitignore")
        val glueGitignoreFileContents = readFromFile(file = glueGitIgnore)
        val implGitIgnore = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + ".gitignore")
        val implGitignoreFileContents = readFromFile(file = implGitIgnore)

        Assert.assertEquals(
            GitIgnoreTemplate.data,
            apiGitignoreFileContents.joinToString("\n")
        )

        Assert.assertEquals(
            GitIgnoreTemplate.data,
            glueGitignoreFileContents.joinToString("\n")
        )

        Assert.assertEquals(
            GitIgnoreTemplate.data,
            implGitignoreFileContents.joinToString("\n")
        )
    }

    @Test
    fun `gitignore is generated in android module with custom settings when setting is enabled`() {
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
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = true,
            rootPathString = folder.root.toString()
        )

        val apiGitIgnore = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "api" + File.separator + ".gitignore")
        val apiGitignoreFileContents = readFromFile(file = apiGitIgnore)
        val glueGitIgnore = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "glue" + File.separator + ".gitignore")
        val glueGitignoreFileContents = readFromFile(file = glueGitIgnore)
        val implGitIgnore = File(folder.root.path + File.separator + modulePathAsFile + File.separator + "impl" + File.separator + ".gitignore")
        val implGitignoreFileContents = readFromFile(file = implGitIgnore)

        Assert.assertEquals(
            template,
            apiGitignoreFileContents.joinToString("\n")
        )

        Assert.assertEquals(
            template,
            glueGitignoreFileContents.joinToString("\n")
        )

        Assert.assertEquals(
            template,
            implGitignoreFileContents.joinToString("\n")
        )
    }

    @Test
    fun `create module works with 2 parameters`() {
        settingsGradleFile.delete()
        settingsGradleFile = folder.populateSettingsGradleKtsWithFakeFilePathData()
        val modulePath = ":repository:network"
        val modulePathAsFile = "repository/network"
        val rootPathString = folder.root.toString().removePrefix("/")

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
            enhancedModuleCreationStrategy = true,
            useKtsBuildFile = false,
            gradleFileFollowModule = false,
            packageName = testPackageName,
            addReadme = false,
            addGitIgnore = true,
            rootPathString = folder.root.toString()
        )

        val settingsGradleFileContents = readFromFile(file = settingsGradleFile)
        Assert.assertEquals(
            "include(\"$modulePath:api\", \"$rootPathString/$modulePathAsFile/api\")",
            settingsGradleFileContents[56]
        )
        Assert.assertEquals(
            "include(\"$modulePath:impl\", \"$rootPathString/$modulePathAsFile/impl\")",
            settingsGradleFileContents[57]
        )
        Assert.assertEquals(
            "include(\"$modulePath:glue\", \"$rootPathString/$modulePathAsFile/glue\")",
            settingsGradleFileContents[58]
        )
    }

    @Test
    fun `custom module names used when set`() {
        fakePreferenceService.preferenceState.glueModuleName = "customglue"
        fakePreferenceService.preferenceState.apiModuleName = "customapi"
        fakePreferenceService.preferenceState.implModuleName = "customimpl"

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
            packageName = testPackageName,
            addReadme = true,
            addGitIgnore = false,
            rootPathString = folder.root.toString()
        )

        // assert it was added to settings.gradle
        val settingsGradleFileContents = readFromFile(file = settingsGradleFile)
        assert(
            settingsGradleFileContents.contains("include(\":repository:customapi\")")
        )
        assert(
            settingsGradleFileContents.contains("include(\":repository:customglue\")")
        )
        assert(
            settingsGradleFileContents.contains("include(\":repository:customimpl\")")
        )

        // assert readme was generated in the api module
        assert(
            // root/repository/api/README.md
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "customapi" + File.separator + readmeFile).exists()
        )

        // assert build.gradle is generated for all 3 modules
        val buildGradleFileApi =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "customapi" + File.separator + buildGradleFileName)
        val buildGradleFileGlue =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "customglue" + File.separator + buildGradleFileName)
        val buildGradleFileImpl =
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "customimpl" + File.separator + buildGradleFileName)
        assert(buildGradleFileApi.exists())
        assert(buildGradleFileGlue.exists())
        assert(buildGradleFileImpl.exists())

        // assert package name is included in build.gradle
        val buildGradleApiFileContents = readFromFile(buildGradleFileApi)
        val buildGradleGlueFileContents = readFromFile(buildGradleFileGlue)
        val buildGradleImplFileContents = readFromFile(buildGradleFileImpl)
        assert(
            buildGradleApiFileContents.contains(
                "    namespace = \"$testPackageName.customapi\""
            )
        )
        assert(
            buildGradleGlueFileContents.contains(
                "    namespace = \"$testPackageName.customglue\""
            )
        )
        assert(
            buildGradleImplFileContents.contains(
                "    namespace = \"$testPackageName.customimpl\""
            )
        )

        // assert the correct package structure is generated
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "customapi/src/main/kotlin/com/joetr/test/customapi").exists()
        )
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "customglue/src/main/kotlin/com/joetr/test/customglue").exists()
        )
        assert(
            File(folder.root.path + File.separator + modulePathAsFile + File.separator + "customimpl/src/main/kotlin/com/joetr/test/customimpl").exists()
        )
    }
}
