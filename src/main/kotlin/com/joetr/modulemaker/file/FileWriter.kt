package com.joetr.modulemaker.file

import com.joetr.modulemaker.persistence.PreferenceService
import com.joetr.modulemaker.template.GitIgnoreTemplate
import com.joetr.modulemaker.template.TemplateWriter
import java.io.File
import java.io.Writer
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
class FileWriter(
    private val preferenceService: PreferenceService
) {

    private val templateWriter = TemplateWriter(
        preferenceService = preferenceService
    )

    fun createModule(
        settingsGradleFile: File,
        workingDirectory: File,
        modulePathAsString: String,
        moduleType: String,
        showErrorDialog: (String) -> Unit,
        showSuccessDialog: () -> Unit,
        enhancedModuleCreationStrategy: Boolean,
        useKtsBuildFile: Boolean,
        gradleFileFollowModule: Boolean,
        packageName: String,
        addReadme: Boolean,
        addGitIgnore: Boolean,
        rootPathString: String
    ) {
        val fileReady = modulePathAsString.replace(":", "/")

        val path = Paths.get(workingDirectory.toURI())
        val modulePath = Paths.get(path.toString(), fileReady)
        val moduleFile = File(modulePath.absolutePathString())

        // get the actual module name, not the path. at this point, it will be something like :experiences:foo
        val moduleName = modulePathAsString.split(":").last()

        if (moduleName.isEmpty()) {
            // display alert
            showErrorDialog("Module name empty / not as expected (is it formatted as :module?)")
            return
        }

        // create if it doesn't exist
        moduleFile.mkdirs()

        // add to settings.gradle.kts
        addToSettingsAtCorrectLocation(
            rootPathAsString = rootPathString,
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
                packageName = packageName,
                addReadme = addReadme,
                addGitIgnore = addGitIgnore
            )
        } else {
            createDefaultModuleStructure(
                moduleFile = moduleFile,
                moduleName = moduleName,
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName,
                addReadme = addReadme,
                addGitIgnore = addGitIgnore
            )
        }

        showSuccessDialog()
    }

    private fun createEnhancedModuleStructure(
        moduleFile: File,
        moduleType: String,
        useKtsBuildFile: Boolean,
        gradleFileFollowModule: Boolean,
        packageName: String,
        addReadme: Boolean,
        addGitIgnore: Boolean
    ) {
        // make the 3 module
        moduleFile.toPath().resolve(preferenceService.preferenceState.glueModuleName).toFile().apply {
            mkdirs()
            // create the gradle file
            templateWriter.createGradleFile(
                moduleFile = this,
                moduleName = moduleFile.path.split(File.separator).toList().last().plus("-")
                    .plus(preferenceService.preferenceState.glueModuleName),
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                defaultKey = GLUE_KEY,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName.plus(".${preferenceService.preferenceState.glueModuleName}")
            )

            // create default packages
            createDefaultPackages(
                moduleFile = this,
                packageName = packageName.plus(".${preferenceService.preferenceState.glueModuleName}")
            )

            if (addGitIgnore) {
                createGitIgnore(
                    moduleFile = this
                )
            }
        }

        moduleFile.toPath().resolve(preferenceService.preferenceState.implModuleName).toFile().apply {
            mkdirs()
            templateWriter.createGradleFile(
                moduleFile = this,
                moduleName = moduleFile.path.split(File.separator).toList().last().plus("-")
                    .plus(preferenceService.preferenceState.implModuleName),
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                defaultKey = IMPL_KEY,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName.plus(".${preferenceService.preferenceState.implModuleName}")
            )

            // create default packages
            createDefaultPackages(
                moduleFile = this,
                packageName = packageName.plus(".${preferenceService.preferenceState.implModuleName}")
            )

            if (addGitIgnore) {
                createGitIgnore(
                    moduleFile = this
                )
            }
        }

        moduleFile.toPath().resolve(preferenceService.preferenceState.apiModuleName).toFile().apply {
            mkdirs()
            templateWriter.createGradleFile(
                moduleFile = this,
                moduleName = moduleFile.path.split(File.separator).toList().last().plus("-")
                    .plus(preferenceService.preferenceState.apiModuleName),
                moduleType = moduleType,
                useKtsBuildFile = useKtsBuildFile,
                defaultKey = API_KEY,
                gradleFileFollowModule = gradleFileFollowModule,
                packageName = packageName.plus(".${preferenceService.preferenceState.apiModuleName}")
            )

            if (addReadme) {
                // create readme file for the api module
                templateWriter.createReadmeFile(
                    moduleFile = this,
                    moduleName = preferenceService.preferenceState.apiModuleName
                )
            }

            // create default packages
            createDefaultPackages(
                moduleFile = this,
                packageName = packageName.plus(".${preferenceService.preferenceState.apiModuleName}")
            )

            if (addGitIgnore) {
                createGitIgnore(
                    moduleFile = this
                )
            }
        }
    }

    private fun createDefaultModuleStructure(
        moduleFile: File,
        moduleName: String,
        moduleType: String,
        useKtsBuildFile: Boolean,
        gradleFileFollowModule: Boolean,
        packageName: String,
        addReadme: Boolean,
        addGitIgnore: Boolean
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

        if (addReadme) {
            // create readme file
            templateWriter.createReadmeFile(
                moduleFile = moduleFile,
                moduleName = moduleName
            )
        }

        // create default packages
        createDefaultPackages(
            moduleFile = moduleFile,
            packageName = packageName
        )

        if (addGitIgnore) {
            createGitIgnore(
                moduleFile = moduleFile
            )
        }
    }

    private fun createGitIgnore(moduleFile: File) {
        val gitignoreFile = Paths.get(moduleFile.absolutePath).toFile()

        val writer: Writer = java.io.FileWriter(Paths.get(gitignoreFile.absolutePath, ".gitignore").toFile())

        val customPreferences = preferenceService.preferenceState.gitignoreTemplate
        val dataToWrite = customPreferences.ifEmpty {
            GitIgnoreTemplate.data
        }
        writer.write(dataToWrite)
        writer.flush()
        writer.close()
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
        showErrorDialog: (String) -> Unit,
        rootPathAsString: String
    ) {
        val settingsFile = Files.readAllLines(Paths.get(settingsGradleFile.toURI()))

        val includeKeywords = listOf(
            "includeProject",
            "includeBuild",
            "include"
        )
        val twoParametersPattern = """\(".+", ".+"\)""".toRegex()

        // TODO - add ability to specify keyword
        val lastNonEmptyLineInSettingsGradleFile = settingsFile.last {
            it.isNotEmpty()
        }
        val projectIncludeKeyword = includeKeywords.firstOrNull { includeKeyword ->
            lastNonEmptyLineInSettingsGradleFile.contains(includeKeyword)
        }

        if (projectIncludeKeyword == null) {
            showErrorDialog("Could not find any include statements in settings.gradle(.kts) file")
            return
        }

        val usesTwoParameters = settingsFile.any { line ->
            twoParametersPattern.containsMatchIn(line)
        }

        // get the last line numbers for an include statement
        val lastLineNumberOfFirstIncludeProjectStatement = settingsFile.indexOfLast {
            settingsFileContainsSpecialIncludeKeyword(it, projectIncludeKeyword)
        }

        // traverse backwards from there to find the first instance
        var tempIndexForSettingsFile = lastLineNumberOfFirstIncludeProjectStatement
        while (tempIndexForSettingsFile >= 0) {
            val currentLine = settingsFile[tempIndexForSettingsFile]
            if (currentLine.trim().isEmpty() || settingsFileContainsSpecialIncludeKeyword(currentLine, projectIncludeKeyword)) {
                tempIndexForSettingsFile--
            } else {
                break
            }
        }

        // assume tempIndexForSettingsFile is the first line
        val firstLineNumberOfFirstIncludeProjectStatement = tempIndexForSettingsFile + 1

        if (firstLineNumberOfFirstIncludeProjectStatement <= 0) {
            showErrorDialog("Could not find any include statements in settings.gradle(.kts) file")
            return
        }

        // sub list them and create a new list so we aren't modifying the original
        val includeProjectStatements = settingsFile.subList(
            firstLineNumberOfFirstIncludeProjectStatement,
            lastLineNumberOfFirstIncludeProjectStatement + 1
        ).toMutableList()

        val textToWrite = constructTextToWrite(
            enhancedModuleCreationStrategy = enhancedModuleCreationStrategy,
            usesTwoParameters = usesTwoParameters,
            projectIncludeKeyword = projectIncludeKeyword,
            modulePathAsString = modulePathAsString,
            rootPathAsString = rootPathAsString
        )

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

    private fun settingsFileContainsSpecialIncludeKeyword(
        stringToCheck: String,
        projectIncludeKeyword: String
    ): Boolean {
        return stringToCheck.contains("$projectIncludeKeyword(\"") ||
            stringToCheck.contains("$projectIncludeKeyword('") ||
            stringToCheck.contains("$projectIncludeKeyword \"") ||
            stringToCheck.contains("$projectIncludeKeyword '")
    }

    private fun constructTextToWrite(
        enhancedModuleCreationStrategy: Boolean,
        usesTwoParameters: Boolean,
        projectIncludeKeyword: String,
        modulePathAsString: String,
        rootPathAsString: String
    ): String {
        fun buildText(path: String): String {
            val parametersString = if (usesTwoParameters) {
                val filePath = "$rootPathAsString${path.replace(":", File.separator)}".removePrefix("/")
                "\"$path\", \"$filePath\""
            } else {
                "\"$path\""
            }
            return "$projectIncludeKeyword($parametersString)"
        }

        return if (enhancedModuleCreationStrategy) {
            val paths = arrayOf(
                "$modulePathAsString:${preferenceService.preferenceState.apiModuleName}",
                "$modulePathAsString:${preferenceService.preferenceState.implModuleName}",
                "$modulePathAsString:${preferenceService.preferenceState.glueModuleName}"
            )
            paths.joinToString("\n") { buildText(it) }
        } else {
            buildText(modulePathAsString)
        }
    }
}
