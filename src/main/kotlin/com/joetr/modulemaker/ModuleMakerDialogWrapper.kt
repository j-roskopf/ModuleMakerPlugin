package com.joetr.modulemaker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.joetr.modulemaker.data.analytics.ModuleCreationAnalytics
import com.joetr.modulemaker.data.analytics.ModuleCreationErrorAnalytics
import com.joetr.modulemaker.data.toProjectFile
import com.joetr.modulemaker.file.FileWriter
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import com.joetr.modulemaker.ui.LabelledCheckbox
import com.joetr.modulemaker.ui.file.FileTree
import com.joetr.modulemaker.ui.file.FileTreeView
import com.joetr.modulemaker.ui.theme.WidgetTheme
import com.segment.analytics.kotlin.core.Analytics
import org.jetbrains.annotations.Nullable
import org.jetbrains.jewel.bridge.JewelComposeNoThemePanel
import org.jetbrains.jewel.bridge.icon.fromPlatformIcon
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icon.IntelliJIconKey
import java.awt.event.ActionEvent
import java.io.File
import java.nio.file.Path
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

private const val WINDOW_WIDTH = 840
private const val WINDOW_HEIGHT = 800
private const val FILE_TREE_WIDTH = 300
private const val CONFIGURATION_PANEL_WIDTH = 540

const val ANDROID = "Android"
const val MULTIPLATFORM = "Multiplatform"
const val KOTLIN = "Kotlin / JVM"

private const val DEFAULT_MODULE_NAME = ":repository:database (as an example)"
private const val DEFAULT_SRC_VALUE = "EMPTY"

class ModuleMakerDialogWrapper(
    private val project: Project,
    private val startingLocation: VirtualFile?
) : DialogWrapper(true) {

    private val preferenceService = PreferenceServiceImpl.instance

    private val fileWriter = FileWriter(
        preferenceService = preferenceService
    )

    private var selectedSrcValue = mutableStateOf(DEFAULT_SRC_VALUE)
    private val threeModuleCreation = mutableStateOf(preferenceService.preferenceState.threeModuleCreationDefault)
    private val useKtsExtension = mutableStateOf(preferenceService.preferenceState.useKtsFileExtension)
    private val gradleFileNamedAfterModule =
        mutableStateOf(preferenceService.preferenceState.gradleFileNamedAfterModule)
    private val addReadme = mutableStateOf(preferenceService.preferenceState.addReadme)
    private val addGitIgnore = mutableStateOf(preferenceService.preferenceState.addGitIgnore)
    private val moduleTypeSelection = mutableStateOf(ANDROID)
    private val platformTypeSelection = mutableStateOf(ANDROID)
    private val moduleName = mutableStateOf(TextFieldValue(""))
    private val packageName = mutableStateOf(TextFieldValue(preferenceService.preferenceState.packageName))
    private val sourceSets = mutableStateListOf<String>()

    // Segment's write key isn't really a secret
    private var analytics: Analytics = Analytics("CNghGjhOHipwGB9YdWMBwkMTJbRFtizc") {
        application = "ModuleMaker"
        flushAt = 1
    }

    init {
        title = "Module Maker"
        init()

        selectedSrcValue.value = if (startingLocation != null) {
            // give default of starting location
            File(startingLocation.path).absolutePath.removePrefix(rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        } else {
            // give default value of the root project
            File(rootDirectoryString()).absolutePath.removePrefix(rootDirectoryStringDropLast())
                .removePrefix(File.separator)
        }
    }

    @OptIn(ExperimentalJewelApi::class)
    @Nullable
    override fun createCenterPanel(): JComponent {
        return JewelComposeNoThemePanel(focusOnClickInside = false) {
            WidgetTheme {
                Row {
                    val startingHeight = remember { mutableStateOf(WINDOW_HEIGHT) }
                    val fileTreeWidth = remember { mutableStateOf(FILE_TREE_WIDTH) }
                    val configurationPanelWidth = remember { mutableStateOf(CONFIGURATION_PANEL_WIDTH) }
                    FileTreeJPanel(
                        modifier = Modifier.height(startingHeight.value.dp).width(fileTreeWidth.value.dp)
                    )
                    ConfigurationPanel(
                        modifier = Modifier.height(startingHeight.value.dp)
                            .width(configurationPanelWidth.value.dp)
                    )
                }
            }
        }
    }

    override fun createLeftSideActions(): Array<Action> {
        return arrayOf(object : AbstractAction("Settings") {
            override fun actionPerformed(e: ActionEvent?) {
                SettingsDialogWrapper(
                    project = project,
                    onSave = {
                        onSettingsSaved()
                    },
                    isKtsCurrentlyChecked = useKtsExtension.value,
                    isAndroidChecked = moduleTypeSelection.value == ANDROID
                ).show()
            }
        })
    }

    private fun onSettingsSaved() {
        packageName.value = TextFieldValue(preferenceService.preferenceState.packageName)
        threeModuleCreation.value = preferenceService.preferenceState.threeModuleCreationDefault
        useKtsExtension.value = preferenceService.preferenceState.useKtsFileExtension
        gradleFileNamedAfterModule.value = preferenceService.preferenceState.gradleFileNamedAfterModule
        addReadme.value = preferenceService.preferenceState.addReadme
        addGitIgnore.value = preferenceService.preferenceState.addGitIgnore
    }

    override fun createActions(): Array<Action> {
        return arrayOf(
            DialogWrapperExitAction(
                "Cancel",
                2
            ),
            object : AbstractAction("Preview") {
                override fun actionPerformed(e: ActionEvent?) {
                    if (validateInput()) {
                        displayPreviewDialog()
                    } else {
                        MessageDialogWrapper("Please fill out required values").show()
                    }
                }
            },
            object : AbstractAction("Create") {
                override fun actionPerformed(e: ActionEvent?) {
                    if (validateInput()) {
                        create(previewMode = false)
                    } else {
                        MessageDialogWrapper("Please fill out required values").show()
                    }
                }
            }
        )
    }

    private fun displayPreviewDialog() {
        val filesToBeCreated = create(previewMode = true)
        PreviewDialogWrapper(filesToBeCreated = filesToBeCreated, root = rootFromPath(rootDirectoryString())).show()
    }

    private fun validateInput(): Boolean {
        return packageName.value.text.isNotEmpty() &&
            selectedSrcValue.value != DEFAULT_SRC_VALUE &&
            moduleName.value.text.isNotEmpty() &&
            moduleName.value.text != DEFAULT_MODULE_NAME
    }

    @Composable
    private fun FileTreeJPanel(
        modifier: Modifier = Modifier
    ) {
        val height = remember { mutableStateOf(WINDOW_HEIGHT) }
        val fileTree = remember { FileTree(root = File(rootDirectoryString()).toProjectFile()) }
        FileTreeView(
            modifier = modifier,
            model = fileTree,
            height = height.value.dp,
            onClick = { fileTreeNode ->

                // gran the absolute file path for the given node
                val absolutePathAtNode = fileTreeNode.file.absolutePath

                /**
                 * grab just the path relative to the root to display
                 *
                 * so if we have /Users/Joe/Code/ModuleMaker as a root of the project,
                 * and the file path that was selected was /Users/Joe/Code/ModuleMaker/app/test
                 *
                 * we ultimately just want to display 'app/test'
                 */
                val relativePath =
                    absolutePathAtNode.removePrefix(rootDirectoryStringDropLast()).removePrefix(File.separator)

                if (fileTreeNode.file.isDirectory) {
                    selectedSrcValue.value = relativePath
                }
            }
        )
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalJewelApi::class)
    @Composable
    private fun ConfigurationPanel(
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            val selectedRootState = remember { selectedSrcValue }
            Text("Selected root: ${selectedRootState.value}")

            Spacer(Modifier.height(16.dp))

            Row {
                val threeModuleCreationState = remember { threeModuleCreation }
                LabelledCheckbox(
                    label = "3 Module Creation",
                    checked = threeModuleCreationState.value,
                    onCheckedChange = {
                        threeModuleCreationState.value = it
                    }
                )
                IconButton(onClick = {
                    MessageDialogWrapper(
                        """
                                            The 3 module creation adds an api, glue, and impl module.

                                            More info can be found here https://www.droidcon.com/2019/11/15/android-at-scale-square/
                        """.trimIndent()
                    ).show()
                }) { _ ->
                    Icon(
                        key = IntelliJIconKey.fromPlatformIcon(AllIcons.General.Information),
                        contentDescription = "info",
                        iconClass = AllIcons::class.java,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val useKtsExtensionState = remember { useKtsExtension }
            LabelledCheckbox(
                label = "Use .kts file extension",
                checked = useKtsExtensionState.value,
                onCheckedChange = {
                    useKtsExtensionState.value = it
                }
            )

            Spacer(Modifier.height(8.dp))

            val gradleFileNamedAfterModuleState = remember { gradleFileNamedAfterModule }
            LabelledCheckbox(
                label = "Gradle file named after module",
                checked = gradleFileNamedAfterModuleState.value,
                onCheckedChange = {
                    gradleFileNamedAfterModuleState.value = it
                }
            )

            Spacer(Modifier.height(8.dp))

            val addReadmeState = remember { addReadme }
            LabelledCheckbox(
                label = "Add README.md",
                checked = addReadmeState.value,
                onCheckedChange = {
                    addReadmeState.value = it
                }
            )

            Spacer(Modifier.height(8.dp))

            val addGitIgnoreState = remember { addGitIgnore }
            LabelledCheckbox(
                label = "Add .gitignore",
                checked = addGitIgnoreState.value,
                onCheckedChange = {
                    addGitIgnoreState.value = it
                }
            )

            Spacer(Modifier.height(8.dp))

            val radioOptions = listOf(ANDROID, KOTLIN)
            val moduleTypeSelectionState = remember { moduleTypeSelection }
            Column {
                Text("Module Type")
                Spacer(Modifier.height(8.dp))
                radioOptions.forEach { text ->
                    RadioButtonRow(
                        text = text,
                        selected = (text == moduleTypeSelectionState.value),
                        onClick = { moduleTypeSelectionState.value = text },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            val platformTypeRadioOptions = listOf(ANDROID, MULTIPLATFORM)
            val platformTypeRadioOptionsState = remember { platformTypeSelection }
            Column {
                Text("Platform Type")

                Spacer(Modifier.height(8.dp))

                platformTypeRadioOptions.forEach { text ->
                    RadioButtonRow(
                        text = text,
                        selected = (text == platformTypeRadioOptionsState.value),
                        onClick = { platformTypeRadioOptionsState.value = text },
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    Spacer(Modifier.height(8.dp))
                }

                val selectedSourceSets = remember {
                    sourceSets
                }

                AnimatedVisibility(
                    platformTypeSelection.value == MULTIPLATFORM
                ) {
                    Spacer(Modifier.height(8.dp))

                    Column {
                        Text(modifier = Modifier.padding(vertical = 8.dp), text = "Selected Source Sets: ${selectedSourceSets.joinToString(separator = ", ")}")

                        FlowRow(Modifier.padding(vertical = 8.dp)) {
                            kotlinMultiplatformSourceSets.forEach { sourceSet ->
                                LabelledCheckbox(
                                    label = sourceSet,
                                    checked = selectedSourceSets.contains(sourceSet),
                                    onCheckedChange = {
                                        if (it) {
                                            selectedSourceSets.add(sourceSet)
                                        } else {
                                            selectedSourceSets.remove(sourceSet)
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text("Test Source Sets")

                        Spacer(Modifier.height(8.dp))

                        FlowRow(Modifier.padding(vertical = 8.dp)) {
                            kotlinMultiplatformTestSourceSets.forEach { sourceSet ->
                                LabelledCheckbox(
                                    label = sourceSet,
                                    checked = selectedSourceSets.contains(sourceSet),
                                    onCheckedChange = {
                                        if (it) {
                                            selectedSourceSets.add(sourceSet)
                                        } else {
                                            selectedSourceSets.remove(sourceSet)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val packageNameState = remember { packageName }
            Text("Package Name")
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = packageNameState.value,
                onValueChange = {
                    packageNameState.value = it
                }
            )

            Spacer(Modifier.height(8.dp))

            val moduleNameState = remember { moduleName }
            Text("Module Name")
            TextField(
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(DEFAULT_MODULE_NAME)
                },
                value = moduleNameState.value,
                onValueChange = {
                    moduleNameState.value = it
                }
            )
        }
    }

    /**
     * When grabbing the settings.gradle(.kts) file, we first want to look in the selected root
     *
     * This is helpful in case of multi-application projects.
     */
    private fun getSettingsGradleFile(): File? {
        val settingsGradleKtsCurrentlySelectedRoot =
            Path.of(getCurrentlySelectedFile().absolutePath, "settings.gradle.kts").toFile()
        val settingsGradleCurrentlySelectedRoot =
            Path.of(getCurrentlySelectedFile().absolutePath, "settings.gradle").toFile()
        val settingsGradleKtsPath = Path.of(rootDirectoryString(), "settings.gradle.kts").toFile()
        val settingsGradlePath = Path.of(rootDirectoryString(), "settings.gradle").toFile()

        return listOf(
            settingsGradleKtsCurrentlySelectedRoot,
            settingsGradleCurrentlySelectedRoot,
            settingsGradleKtsPath,
            settingsGradlePath
        ).firstOrNull {
            it.exists()
        } ?: run {
            MessageDialogWrapper("Can't find settings.gradle(.kts) file")
            null
        }
    }

    private fun create(previewMode: Boolean): List<File> {
        val settingsGradleFile = getSettingsGradleFile()
        val moduleType = moduleTypeSelection.value
        val currentlySelectedFile = getCurrentlySelectedFile()
        if (settingsGradleFile != null) {
            analytics.track(
                "module_created",
                ModuleCreationAnalytics(
                    moduleType = moduleType,
                    threeModule = threeModuleCreation.value,
                    addGitIgnore = addGitIgnore.value,
                    addReadme = addReadme.value,
                    gradleNameToFollow = gradleFileNamedAfterModule.value,
                    useKts = useKtsExtension.value
                )
            )
            val filesCreated = fileWriter.createModule(
                // at this point, selectedSrcValue has a value of something like /root/module/module2/
                // - we want to remove the root of the project to use as the file path in settings.gradle
                rootPathString = removeRootFromPath(selectedSrcValue.value),
                settingsGradleFile = settingsGradleFile,
                modulePathAsString = moduleName.value.text,
                moduleType = moduleType,
                showErrorDialog = {
                    analytics.track("module_creation_error", ModuleCreationErrorAnalytics(message = it))
                    MessageDialogWrapper(it).show()
                },
                showSuccessDialog = {
                    analytics.track("module_creation_success")
                    MessageDialogWrapper("Success").show()
                    refreshFileSystem(
                        settingsGradleFile = settingsGradleFile,
                        currentlySelectedFile = currentlySelectedFile
                    )
                    if (preferenceService.preferenceState.refreshOnModuleAdd) {
                        syncProject()
                    }
                },
                workingDirectory = currentlySelectedFile,
                enhancedModuleCreationStrategy = threeModuleCreation.value,
                useKtsBuildFile = useKtsExtension.value,
                gradleFileFollowModule = gradleFileNamedAfterModule.value,
                packageName = packageName.value.text,
                addReadme = addReadme.value,
                addGitIgnore = addGitIgnore.value,
                previewMode = previewMode,
                platformType = platformTypeSelection.value,
                sourceSets = sourceSets.toList()
            )

            return filesCreated
        } else {
            MessageDialogWrapper("Couldn't find settings.gradle(.kts)").show()
            return emptyList()
        }
    }

    private fun syncProject() {
        ExternalSystemUtil.refreshProject(
            project,
            ProjectSystemId("GRADLE"),
            rootDirectoryString(),
            false,
            ProgressExecutionMode.START_IN_FOREGROUND_ASYNC
        )
    }

    /**
     * Refresh the settings gradle file and the root file
     */
    private fun refreshFileSystem(settingsGradleFile: File, currentlySelectedFile: File) {
        VfsUtil.markDirtyAndRefresh(
            false,
            true,
            true,
            settingsGradleFile,
            currentlySelectedFile
        )
    }

    private fun getCurrentlySelectedFile(): File {
        return File(rootDirectoryStringDropLast() + File.separator + selectedSrcValue.value)
    }

    private fun rootDirectoryStringDropLast(): String {
        // rootDirectoryString() gives us back something like /Users/user/path/to/project
        // the first path element in the tree node starts with 'project' (last folder above)
        // so we remove it and join the nodes of the tree by our file separator
        return project.basePath!!.split(File.separator).dropLast(1).joinToString(File.separator)
    }

    private fun rootDirectoryString(): String {
        return project.basePath!!
    }

    private fun removeRootFromPath(path: String): String {
        return path.split(File.separator).drop(1).joinToString(File.separator)
    }

    private fun rootFromPath(path: String): String {
        return path.split(File.separator).last()
    }
}
