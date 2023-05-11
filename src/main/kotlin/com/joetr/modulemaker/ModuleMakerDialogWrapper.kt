package com.joetr.modulemaker

import com.intellij.icons.AllIcons
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.io.exists
import com.joetr.modulemaker.file.FileWriter
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.nio.file.Path
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
import javax.swing.SpringLayout
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

private const val WINDOW_WIDTH = 840
private const val WINDOW_HEIGHT = 600
private const val FILE_TREE_WIDTH = 300

const val ANDROID = "Android"
const val KOTLIN = "Kotlin"

private const val DEFAULT_MODULE_NAME = ":repository:database (as an example)"
private const val DEFAULT_SRC_VALUE = "EMPTY"

class ModuleMakerDialogWrapper : DialogWrapper(true) {

    private val preferenceService = PreferenceServiceImpl.instance

    private val fileWriter = FileWriter(
        preferenceService = preferenceService
    )

    private var selectedSrcValue = DEFAULT_SRC_VALUE
    private lateinit var selectedSrcJLabel: JLabel
    private lateinit var packageNameTextField: JTextField
    private lateinit var moduleNameTextField: JTextField
    private lateinit var moduleTypeRadioGroup: ButtonGroup
    private lateinit var androidTypeRadioButton: JRadioButton
    private lateinit var kotlinTypeRadioButton: JRadioButton
    private lateinit var threeModuleCreationCheckbox: JCheckBox
    private lateinit var ktsCheckbox: JCheckBox
    private lateinit var gradleFileNamedAfterModule: JCheckBox

    init {
        title = "Module Maker"
        init()
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        customInitialize()

        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)

        val fileTreeJPanel = createFileTreeJPanel()
        val configurationJPanel = createConfigurationPanel()

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
                        onSave = {
                            onSettingsSaved()
                        }
                    ).show()
                }
            }
        )
    }

    private fun onSettingsSaved() {
        packageNameTextField.text = preferenceService.preferenceState.packageName
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
        return packageNameTextField.text.isNotEmpty() &&
            selectedSrcValue != DEFAULT_SRC_VALUE &&
            moduleNameTextField.text.isNotEmpty() && moduleNameTextField.text != DEFAULT_MODULE_NAME
    }

    private fun customInitialize() {
        initSelectedSrcJLabel()
    }

    private fun initSelectedSrcJLabel() {
        selectedSrcJLabel = JLabel("Selected src: $selectedSrcValue")
    }

    private fun createFileTreeJPanel(): Component {
        val tree = FileTree(
            rootDirectoryString()
        )

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
        tree.addTreeSelectionListener {
            val treePath = it.paths.first().path.toList().joinToString(File.separator)
            val treeFile = File(
                rootDirectoryStringDropLast() + File.separator + treePath
            )
            if (treeFile.isDirectory) {
                selectedSrcValue = treePath
                selectedSrcJLabel.text = "Selected root: $selectedSrcValue"
            }
        }

        // select the root item by default
        val treePath = TreePath(lastPathInRootDirectory())
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

    private fun createConfigurationPanel(): Component {
        val configurationJPanel = JPanel()
        val configurationLayout = SpringLayout()
        val moduleNameTextLabel = JLabel("Module Name: ")
        val packageNameTextLabel = JLabel("Package Name: ")
        val threeModuleQuestionMarkIcon = AllIcons.Actions.Help
        val threeModuleQuestionMarkButton = JLabel(threeModuleQuestionMarkIcon).apply {
            preferredSize = Dimension(24, 24)
        }
        threeModuleQuestionMarkButton.addMouseListener(
            object : MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    MessageDialogWrapper(
                        """
                            The 3 module creation adds an api, glue, and impl module.

                            More info can be found here https://www.droidcon.com/2019/11/15/android-at-scale-square/
                        """.trimIndent()
                    ).show()
                }

                override fun mousePressed(e: MouseEvent?) {
                }

                override fun mouseReleased(e: MouseEvent?) {
                }

                override fun mouseEntered(e: MouseEvent?) {
                }

                override fun mouseExited(e: MouseEvent?) {
                }
            }
        )
        configurationJPanel.layout = configurationLayout
        threeModuleCreationCheckbox = JCheckBox("3 Module Creation")
        ktsCheckbox = JCheckBox("Use .kts file extension")
        gradleFileNamedAfterModule = JCheckBox("Gradle file named after module")
        packageNameTextField = JTextField(preferenceService.preferenceState.packageName)
        moduleNameTextField = JTextField(DEFAULT_MODULE_NAME)

        configurationJPanel.add(selectedSrcJLabel)
        configurationJPanel.add(threeModuleCreationCheckbox)
        configurationJPanel.add(ktsCheckbox)
        configurationJPanel.add(gradleFileNamedAfterModule)
        configurationJPanel.add(packageNameTextField)
        configurationJPanel.add(moduleNameTextLabel)
        configurationJPanel.add(moduleNameTextField)
        configurationJPanel.add(packageNameTextLabel)
        configurationJPanel.add(threeModuleQuestionMarkButton)

        kotlinTypeRadioButton = JRadioButton(KOTLIN)
        androidTypeRadioButton = JRadioButton(ANDROID).apply {
            isSelected = true
        }
        moduleTypeRadioGroup = ButtonGroup()
        moduleTypeRadioGroup.add(kotlinTypeRadioButton)
        moduleTypeRadioGroup.add(androidTypeRadioButton)

        configurationJPanel.add(kotlinTypeRadioButton)
        configurationJPanel.add(androidTypeRadioButton)

        // selected src label
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            selectedSrcJLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            selectedSrcJLabel,
            EXTRA_PADDING,
            SpringLayout.NORTH,
            configurationJPanel
        )

        // 3 module creation
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            threeModuleCreationCheckbox,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            threeModuleCreationCheckbox,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            selectedSrcJLabel
        )

        // kts checkbox
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            ktsCheckbox,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            ktsCheckbox,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            threeModuleCreationCheckbox
        )

        // gradle file name after module
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            gradleFileNamedAfterModule,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            gradleFileNamedAfterModule,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            ktsCheckbox
        )

        // type radio group
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            androidTypeRadioButton,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            androidTypeRadioButton,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            gradleFileNamedAfterModule
        )
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            kotlinTypeRadioButton,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            androidTypeRadioButton
        )
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            kotlinTypeRadioButton,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )

        // package label
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            packageNameTextLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            kotlinTypeRadioButton
        )
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            packageNameTextLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )

        // package text field
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            packageNameTextField,
            EXTRA_PADDING,
            SpringLayout.EAST,
            packageNameTextLabel
        )
        configurationLayout.putConstraint(
            SpringLayout.EAST,
            packageNameTextField,
            -EXTRA_PADDING,
            SpringLayout.EAST,
            configurationJPanel
        )
        configurationLayout.putConstraint(
            SpringLayout.BASELINE,
            packageNameTextField,
            0,
            SpringLayout.BASELINE,
            packageNameTextLabel
        )

        // module label
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            moduleNameTextLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            packageNameTextField
        )
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            moduleNameTextLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            configurationJPanel
        )

        // module name text field
        configurationLayout.putConstraint(
            SpringLayout.WEST,
            moduleNameTextField,
            EXTRA_PADDING,
            SpringLayout.EAST,
            moduleNameTextLabel
        )
        configurationLayout.putConstraint(
            SpringLayout.EAST,
            moduleNameTextField,
            -EXTRA_PADDING,
            SpringLayout.EAST,
            configurationJPanel
        )
        configurationLayout.putConstraint(
            SpringLayout.BASELINE,
            moduleNameTextField,
            0,
            SpringLayout.BASELINE,
            moduleNameTextLabel
        )

        configurationLayout.putConstraint(
            SpringLayout.WEST,
            threeModuleQuestionMarkButton,
            DEFAULT_PADDING,
            SpringLayout.EAST,
            threeModuleCreationCheckbox
        )
        configurationLayout.putConstraint(
            SpringLayout.NORTH,
            threeModuleQuestionMarkButton,
            0,
            SpringLayout.NORTH,
            threeModuleCreationCheckbox
        )
        configurationLayout.putConstraint(
            SpringLayout.SOUTH,
            threeModuleQuestionMarkButton,
            0,
            SpringLayout.SOUTH,
            threeModuleCreationCheckbox
        )

        configurationJPanel.preferredSize = Dimension(WINDOW_WIDTH - FILE_TREE_WIDTH, WINDOW_HEIGHT)
        return configurationJPanel
    }

    private fun getSettingsGradleFile(): File? {
        val settingsGradleKtsPath = Path.of(rootDirectoryString(), "settings.gradle.kts")
        val settingsGradlePath = Path.of(rootDirectoryString(), "settings.gradle")
        return if (settingsGradlePath.exists()) {
            settingsGradlePath.toFile()
        } else if (settingsGradleKtsPath.exists()) {
            settingsGradleKtsPath.toFile()
        } else {
            MessageDialogWrapper("Can't find settings.gradle(.kts) file")
            null
        }
    }

    private fun create() {
        val settingsGradleFile = getSettingsGradleFile()
        val moduleType = if (androidTypeRadioButton.isSelected) {
            ANDROID
        } else if (kotlinTypeRadioButton.isSelected) {
            KOTLIN
        } else {
            throw RuntimeException("No valid module type selected")
        }
        val currentlySelectedFile = getCurrentlySelectedFile()
        if (settingsGradleFile != null) {
            fileWriter.createModule(
                settingsGradleFile = settingsGradleFile,
                modulePathAsString = moduleNameTextField.text,
                moduleType = moduleType,
                showErrorDialog = {
                    MessageDialogWrapper("Error").show()
                },
                showSuccessDialog = {
                    MessageDialogWrapper("Success").show()
                    refreshFileSystem(
                        settingsGradleFile = settingsGradleFile,
                        currentlySelectedFile = currentlySelectedFile
                    )
                    syncProject()
                },
                workingDirectory = currentlySelectedFile,
                enhancedModuleCreationStrategy = threeModuleCreationCheckbox.isSelected,
                useKtsBuildFile = ktsCheckbox.isSelected,
                gradleFileFollowModule = gradleFileNamedAfterModule.isSelected,
                packageName = packageNameTextField.text
            )
        } else {
            MessageDialogWrapper("Couldn't find settings.gradle(.kts)").show()
        }
    }

    private fun syncProject() {
        ExternalSystemUtil.refreshProject(
            ProjectManager.getInstance().openProjects[0],
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
        return File(rootDirectoryStringDropLast() + File.separator + selectedSrcValue)
    }

    private fun rootDirectoryStringDropLast(): String {
        // rootDirectoryString() gives us back something like /Users/user/path/to/project
        // the first path element in the tree node starts with 'project' (last folder above)
        // so we remove it and join the nodes of the tree by our file separator
        return ProjectManager.getInstance().openProjects[0].basePath!!.split(File.separator).dropLast(1)
            .joinToString(File.separator)
    }

    private fun lastPathInRootDirectory(): String {
        // rootDirectoryString() gives us back something like /Users/user/path/to/project
        // the first path element in the tree node starts with 'project' (last folder above)
        // so we remove it and join the nodes of the tree by our file separator
        return ProjectManager.getInstance().openProjects[0].basePath!!.split(File.separator).takeLast(1).first()
    }

    private fun rootDirectoryString(): String {
        return ProjectManager.getInstance().openProjects[0].basePath!!
    }
}

class FileTree(path: String) : JTree(scan(File(path))) {
    companion object {
        private fun scan(node: File): MutableTreeNode {
            val ret = DefaultMutableTreeNode(node.name)
            if (node.isDirectory) for (child in node.listFiles()!!) ret.add(scan(child))
            return ret
        }
    }
}
