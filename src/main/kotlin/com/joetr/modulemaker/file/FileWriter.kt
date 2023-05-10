package com.joetr.modulemaker.file

import com.joetr.modulemaker.template.TemplateWriter
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

const val ANDROID_KEY = "android"
const val KOTLIN_KEY = "kotlin"
const val GLUE_KEY = "glue"
const val API_KEY = "api"
const val IMPL_KEY = "key"

/**
 * This class is responsible for writing files into the project
 */
class FileWriter {

    private val templateWriter = TemplateWriter()

    fun createModule(
        settingsGradleFile: File,
        workingDirectory: File,
        modulePathAsString: String,
        moduleType: String,
        showErrorDialog: () -> Unit,
        showSuccessDialog: () -> Unit,
        enhancedModuleCreationStrategy: Boolean,
        useKtsBuildFile: Boolean,
        gradleFileFollowModule: Boolean,
        packageName: String
    ) {
        val fileReady = modulePathAsString.replace(":", "/")

        val path = Paths.get(workingDirectory.toURI())
        val modulePath = Paths.get(path.toString(), fileReady)
        val moduleFile = File(modulePath.absolutePathString())

        // get the actual module name, not the path. at this point, it will be something like :experiences:foo
        val moduleName = modulePathAsString.split(":").last()

        if (moduleName.isEmpty()) {
            // display alert
            showErrorDialog()
            return
        }

        // create if it doesn't exist
        moduleFile.mkdirs()

        // add to settings.gradle.kts
        addToSettingsAtCorrectLocation(
            modulePathAsString = modulePathAsString,
            settingsGradleFile = settingsGradleFile,
            enhancedModuleCreationStrategy = enhancedModuleCreationStrategy,
            showErrorDialog = showErrorDialog
        )

        if (enhancedModuleCreationStrategy) {
            createEnhancedModuleStructure(
                moduleFile = moduleFile,
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName
            )
        } else {
            createDefaultModuleStructure(
                moduleFile = moduleFile,
                moduleName = moduleName,
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName
            )
        }

        showSuccessDialog()
    }

    private fun createEnhancedModuleStructure(
        moduleFile: File,
        moduleType: String,
        useKtsBuildFile: Boolean,
        gradleFileFollowModule: Boolean,
        packageName: String
    ) {
        // make the 3 module
        moduleFile.toPath().resolve("glue").toFile().apply {
            mkdirs()
            // create the gradle file
            templateWriter.createGradleFile(
                moduleFile = this,
                moduleName = moduleFile.path.split(File.separator).toList().last().plus("-").plus("glue"),
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                defaultKey = GLUE_KEY,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName.plus(".glue")
            )

            // create default packages
            createDefaultPackages(
                moduleFile = this,
                packageName = packageName.plus(".glue")
            )
        }
        moduleFile.toPath().resolve("impl").toFile().apply {
            mkdirs()
            templateWriter.createGradleFile(
                moduleFile = this,
                moduleName = moduleFile.path.split(File.separator).toList().last().plus("-").plus("impl"),
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                defaultKey = IMPL_KEY,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName.plus(".impl")
            )

            // create default packages
            createDefaultPackages(
                moduleFile = this,
                packageName = packageName.plus(".impl")
            )
        }
        moduleFile.toPath().resolve("api").toFile().apply {
            mkdirs()
            templateWriter.createGradleFile(
                moduleFile = this,
                moduleName = moduleFile.path.split(File.separator).toList().last().plus("-").plus("api"),
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                defaultKey = API_KEY,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName.plus(".api")
            )

            // create readme file for the api module
            templateWriter.createReadmeFile(
                moduleFile = this,
                moduleName = "api"
            )

            // create default packages
            createDefaultPackages(
                moduleFile = this,
                packageName = packageName.plus(".api")
            )
        }
    }

    private fun createDefaultModuleStructure(
        moduleFile: File,
        moduleName: String,
        moduleType: String,
        useKtsBuildFile: Boolean,
        gradleFileFollowModule: Boolean,
        packageName: String
    ) {
        // create gradle files
        templateWriter.createGradleFile(
            moduleFile = moduleFile,
            moduleName = moduleName,
            moduleType = moduleType,
            useKtsBuildFile = useKtsBuildFile,
            defaultKey = null,
            gradleFileFollowModule = gradleFileFollowModule,
            packageName = packageName
        )

        // create readme file
        templateWriter.createReadmeFile(
            moduleFile = moduleFile,
            moduleName = moduleName
        )

        // create default packages
        createDefaultPackages(
            moduleFile = moduleFile,
            packageName = packageName
        )
    }

    /**
     * Creates the default package name
     *
     * Gives the module a src/main/kotlin folder with com.<group> name
     */
    private fun createDefaultPackages(
        moduleFile: File,
        packageName: String
    ) {
        // create src/main
        val srcPath = Paths.get(moduleFile.absolutePath, "src/main/kotlin").toFile()
        val packagePath = Paths.get(srcPath.path, packageName.split(".").joinToString(File.separator)).toFile()
        packagePath.mkdirs()

        // create default package
        val stringBuilder = StringBuilder()

        Paths.get(srcPath.absolutePath, stringBuilder.toString()).toFile().mkdirs()
    }

    /**
     * Inserts the entry into settings.gradle.kts at the correct spot to maintain alphabetical order
     *
     * This assumes the file was in alphabetical order to begin with
     */
    private fun addToSettingsAtCorrectLocation(
        settingsGradleFile: File,
        modulePathAsString: String,
        enhancedModuleCreationStrategy: Boolean,
        showErrorDialog: () -> Unit

    ) {
        val settingsFile = Files.readAllLines(Paths.get(settingsGradleFile.toURI()))

        val includeProject = "includeProject"
        val include = "include"

        // TODO - add ability to specify keyword
        val projectIncludeKeyword = if (settingsFile.any { it.contains("includeProject") }) {
            includeProject
        } else {
            include
        }

        // get the first and last line numbers for an include statement
        val firstLineNumberOfFirstIncludeProjectStatement = settingsFile.indexOfFirst {
            it.contains("$projectIncludeKeyword(\"")
        }

        val lastLineNumberOfFirstIncludeProjectStatement = settingsFile.indexOfLast {
            it.contains("$projectIncludeKeyword(\"")
        }

        if (firstLineNumberOfFirstIncludeProjectStatement <= 0) {
            showErrorDialog()
            return
        }

        // sub list them and create a new list so we aren't modifying the original
        val includeProjectStatements = settingsFile.subList(
            firstLineNumberOfFirstIncludeProjectStatement,
            lastLineNumberOfFirstIncludeProjectStatement + 1
        ).toMutableList()

        val textToWrite = if (enhancedModuleCreationStrategy) {
            "$projectIncludeKeyword(\"".plus(modulePathAsString.plus(":api")).plus("\")").plus("\n")
                .plus("$projectIncludeKeyword(\"".plus(modulePathAsString.plus(":impl")).plus("\")")).plus("\n")
                .plus("$projectIncludeKeyword(\"".plus(modulePathAsString.plus(":glue")).plus("\")"))
        } else {
            "$projectIncludeKeyword(\"".plus(modulePathAsString).plus("\")")
        }

        // the spot we want to insert it is the first line we find that is after it alphabetically
        val insertionIndex = includeProjectStatements.indexOfFirst {
            it.isNotEmpty() && it.lowercase() >= textToWrite.lowercase()
        }

        if (insertionIndex < 0) {
            // insert it at the end as nothing is past it
            settingsFile.add(lastLineNumberOfFirstIncludeProjectStatement + 1, textToWrite)
        } else {
            // insert it in our original list adding the original offset of the first line
            settingsFile.add(insertionIndex + firstLineNumberOfFirstIncludeProjectStatement, textToWrite)
        }

        Files.write(Paths.get(settingsGradleFile.toURI()), settingsFile)
    }
}
