package com.joetr.modulemaker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.joetr.modulemaker.data.FileTreeNode
import com.joetr.modulemaker.file.FileWriter
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import com.joetr.modulemaker.ui.LabelledCheckbox
import com.joetr.modulemaker.ui.ModuleMakerFileTree
import com.joetr.modulemaker.ui.ModuleMakerTreeCellRenderer
import com.joetr.modulemaker.ui.ModuleMakerTreeNode
import com.joetr.modulemaker.ui.theme.WidgetTheme
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.File
import java.nio.file.Path
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
import javax.swing.SpringLayout
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.IllegalArgumentException

private const val WINDOW_WIDTH = 840
private const val WINDOW_HEIGHT = 600
private const val FILE_TREE_WIDTH = 300

const val ANDROID = "Android"
const val KOTLIN = "Kotlin"

private const val DEFAULT_MODULE_NAME = ":repository:database (as an example)"
private const val DEFAULT_SRC_VALUE = "EMPTY"

class ModuleMakerDialogWrapper(
    private val project: Project
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
    private val moduleName = mutableStateOf("")
    private val packageName = mutableStateOf(preferenceService.preferenceState.packageName)

    init {
        title = "Module Maker"
        init()
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)

        val fileTreeJPanel = createFileTreeJPanel()
        val configurationJPanel = createConfigurationPanelCompose()

        val baseLayout = SpringLayout()
        dialogPanel.layout = baseLayout

        dialogPanel.add(fileTreeJPanel)
        dialogPanel.add(configurationJPanel)

        baseLayout.putConstraint(
            SpringLayout.WEST,
            fileTreeJPanel,
            DEFAULT_PADDING,
            SpringLayout.WEST,
            contentPane
        )
        baseLayout.putConstraint(
            SpringLayout.NORTH,
            fileTreeJPanel,
            DEFAULT_PADDING,
            SpringLayout.NORTH,
            contentPane
        )
        baseLayout.putConstraint(
            SpringLayout.SOUTH,
            fileTreeJPanel,
            DEFAULT_PADDING,
            SpringLayout.SOUTH,
            contentPane
        )
        baseLayout.putConstraint(
            SpringLayout.WEST,
            configurationJPanel,
            DEFAULT_PADDING,
            SpringLayout.EAST,
            fileTreeJPanel
        )
        baseLayout.putConstraint(
            SpringLayout.NORTH,
            configurationJPanel,
            DEFAULT_PADDING,
            SpringLayout.NORTH,
            contentPane
        )
        return dialogPanel
    }

    override fun createLeftSideActions(): Array<Action> {
        return arrayOf(
            object : AbstractAction("Settings") {
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
            }
        )
    }

    private fun onSettingsSaved() {
        packageName.value = preferenceService.preferenceState.packageName
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
            object : AbstractAction("Create") {
                override fun actionPerformed(e: ActionEvent?) {
                    if (validateInput()) {
                        create()
                    } else {
                        MessageDialogWrapper("Please fill out required values").show()
                    }
                }
            }
        )
    }

    private fun validateInput(): Boolean {
        return packageName.value.isNotEmpty() &&
            selectedSrcValue.value != DEFAULT_SRC_VALUE &&
            moduleName.value.isNotEmpty() && moduleName.value != DEFAULT_MODULE_NAME
    }

    private fun createFileTreeJPanel(): Component {
        val tree = ModuleMakerFileTree(
            createDefaultTreeModel(rootDirectoryString())
        )

        // get all the nodes for the root project
        val rootNodes = tree.getNodesAtFilePath(
            rootDirectoryString()
        )

        // add nodes to the root
        for (node in rootNodes) {
            (tree.customTreeModel.root as DefaultMutableTreeNode).add(node)
        }

        // reload
        tree.customTreeModel.reload((tree.customTreeModel.root as DefaultMutableTreeNode))

        // set custom cell renderer, so we can get our names / icons
        tree.cellRenderer = ModuleMakerTreeCellRenderer()

        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                // grab the expanded node
                val expandedNode = event.path.lastPathComponent as DefaultMutableTreeNode

                // grab the absolute file path from that node
                val path = (expandedNode.userObject as FileTreeNode).file.absolutePath

                // delete any previously added children
                expandedNode.removeAllChildren()

                // gran the new nodes at the path
                val newNodes = tree.getNodesAtFilePath(
                    path
                )

                // Add the child nodes to the expanded node
                for (node in newNodes) {
                    expandedNode.add(node)
                }

                // reload
                tree.customTreeModel.reload(expandedNode)
            }

            override fun treeCollapsed(event: TreeExpansionEvent?) {
                // no-op
            }
        })

        val fileTreeJPanel = JPanel()
        tree.showsRootHandles = true

        // JBScrollPane doesn't seem to always show scroll bars as desired
        @Suppress("UndesirableClassUsage")
        val scrollPane = JScrollPane(
            tree,
            VERTICAL_SCROLLBAR_ALWAYS,
            HORIZONTAL_SCROLLBAR_ALWAYS
        )

        scrollPane.autoscrolls = true
        tree.addTreeSelectionListener { treeSelectedEvent ->
            /**
             * When a tree selected event occurs, we expect the only type to be our custom [ModuleMakerTreeNode]
             */
            when (val lastPathComponent = treeSelectedEvent.paths.first().lastPathComponent) {
                is ModuleMakerTreeNode -> {
                    // grab the node data
                    val fileTreeNode = (lastPathComponent.userObject as FileTreeNode)

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

                else -> {
                    throw IllegalArgumentException("Unknown component type")
                }
            }
        }

        // select the root item by default
        val treePath = TreePath(ModuleMakerTreeNode(true, createRootFileTreeNode(rootDirectoryString())))
        tree.selectionPath = treePath

        scrollPane.border = BorderFactory.createEmptyBorder()
        scrollPane.setViewportView(tree)
        scrollPane.preferredSize = Dimension(FILE_TREE_WIDTH, WINDOW_HEIGHT - DEFAULT_PADDING * 2)
        fileTreeJPanel.preferredSize = Dimension(FILE_TREE_WIDTH, WINDOW_HEIGHT + DEFAULT_PADDING * 2)
        fileTreeJPanel.add(scrollPane)

        val fileTreePaneLayout = SpringLayout()
        fileTreeJPanel.layout = fileTreePaneLayout
        fileTreePaneLayout.putConstraint(
            SpringLayout.WEST,
            scrollPane,
            0,
            SpringLayout.WEST,
            fileTreeJPanel
        )
        fileTreePaneLayout.putConstraint(
            SpringLayout.NORTH,
            scrollPane,
            0,
            SpringLayout.NORTH,
            fileTreeJPanel
        )
        fileTreePaneLayout.putConstraint(
            SpringLayout.EAST,
            scrollPane,
            0,
            SpringLayout.EAST,
            fileTreeJPanel
        )
        return fileTreeJPanel
    }

    private fun createConfigurationPanelCompose(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, WINDOW_WIDTH - FILE_TREE_WIDTH, WINDOW_HEIGHT)
            setContent {
                WidgetTheme {
                    Surface {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                            val selectedRootState = remember { selectedSrcValue }
                            Text("Selected root: ${selectedRootState.value}")

                            Row {
                                val threeModuleCreationState = remember { threeModuleCreation }
                                LabelledCheckbox(
                                    label = "3 Module Creation",
                                    checked = threeModuleCreationState.value,
                                    onCheckedChange = {
                                        threeModuleCreationState.value = it
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        MessageDialogWrapper(
                                            """
                                            The 3 module creation adds an api, glue, and impl module.

                                            More info can be found here https://www.droidcon.com/2019/11/15/android-at-scale-square/
                                            """.trimIndent()
                                        ).show()
                                    },
                                    content = {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = "info",
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                )
                            }

                            val useKtsExtensionState = remember { useKtsExtension }
                            LabelledCheckbox(
                                label = "Use .kts file extension",
                                checked = useKtsExtensionState.value,
                                onCheckedChange = {
                                    useKtsExtensionState.value = it
                                }
                            )

                            val gradleFileNamedAfterModuleState = remember { gradleFileNamedAfterModule }
                            LabelledCheckbox(
                                label = "Gradle file named after module",
                                checked = gradleFileNamedAfterModuleState.value,
                                onCheckedChange = {
                                    gradleFileNamedAfterModuleState.value = it
                                }
                            )

                            val addReadmeState = remember { addReadme }
                            LabelledCheckbox(
                                label = "Add README.md",
                                checked = addReadmeState.value,
                                onCheckedChange = {
                                    addReadmeState.value = it
                                }
                            )

                            val addGitIgnoreState = remember { addGitIgnore }
                            LabelledCheckbox(
                                label = "Add .gitignore",
                                checked = addGitIgnoreState.value,
                                onCheckedChange = {
                                    addGitIgnoreState.value = it
                                }
                            )

                            val radioOptions = listOf(ANDROID, KOTLIN)
                            val moduleTypeSelectionState = remember { moduleTypeSelection }
                            Column {
                                radioOptions.forEach { text ->
                                    Row(
                                        modifier = Modifier
                                            .selectable(
                                                selected = (text == moduleTypeSelectionState.value),
                                                onClick = {
                                                    moduleTypeSelectionState.value = text
                                                }
                                            ).padding(end = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (text == moduleTypeSelectionState.value),
                                            onClick = {
                                                moduleTypeSelectionState.value = text
                                            }
                                        )
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.body1.merge(),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }

                            val packageNameState = remember { packageName }
                            OutlinedTextField(
                                label = { Text("Package Name") },
                                modifier = Modifier.fillMaxWidth(),
                                value = packageNameState.value,
                                onValueChange = {
                                    packageNameState.value = it
                                }
                            )

                            val moduleNameState = remember { moduleName }
                            OutlinedTextField(
                                label = { Text("Module Name") },
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
                }
            }
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

    private fun create() {
        val settingsGradleFile = getSettingsGradleFile()
        val moduleType = moduleTypeSelection.value
        val currentlySelectedFile = getCurrentlySelectedFile()
        if (settingsGradleFile != null) {
            fileWriter.createModule(
                // at this point, selectedSrcValue has a value of something like /root/module/module2/
                // - we want to remove the root of the project to use as the file path in settings.gradle
                rootPathString = removeRootFromPath(selectedSrcValue.value),
                settingsGradleFile = settingsGradleFile,
                modulePathAsString = moduleName.value,
                moduleType = moduleType,
                showErrorDialog = {
                    MessageDialogWrapper(it).show()
                },
                showSuccessDialog = {
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
                packageName = packageName.value,
                addReadme = addReadme.value,
                addGitIgnore = addGitIgnore.value
            )
        } else {
            MessageDialogWrapper("Couldn't find settings.gradle(.kts)").show()
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
        return project.basePath!!.split(File.separator).dropLast(1)
            .joinToString(File.separator)
    }

    private fun rootDirectoryString(): String {
        return project.basePath!!
    }

    private fun removeRootFromPath(path: String): String {
        return path.split(File.separator).drop(1).joinToString(File.separator)
    }

    private fun createRootFileTreeNode(rootFilePath: String): FileTreeNode {
        val rootFile = File(rootFilePath)
        return FileTreeNode(
            displayName = rootFile.name,
            file = rootFile,
            isFolder = rootFile.isDirectory
        )
    }

    /**
     * Creates a [DefaultTreeModel] with one node - the root of the project
     */
    private fun createDefaultTreeModel(filePath: String): DefaultTreeModel {
        val node = File(filePath)
        val rootFileTreeNode = createRootFileTreeNode(filePath)
        val treeNode = ModuleMakerTreeNode(node.isDirectory, rootFileTreeNode)

        return DefaultTreeModel(
            treeNode
        )
    }
}
