@file:Suppress("UndesirableClassUsage")

package com.joetr.modulemaker

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import com.joetr.modulemaker.template.AndroidModuleKtsTemplate
import com.joetr.modulemaker.template.AndroidModuleTemplate
import com.joetr.modulemaker.template.GitIgnoreTemplate
import com.joetr.modulemaker.template.KotlinModuleKtsTemplate
import com.joetr.modulemaker.template.KotlinModuleTemplate
import com.joetr.modulemaker.template.TemplateVariable
import com.joetr.modulemaker.ui.theme.WidgetTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ScrollPaneConstants
import javax.swing.SpringLayout
import javax.swing.event.DocumentListener

private const val WINDOW_WIDTH = 800
private const val WINDOW_HEIGHT = 900

const val DEFAULT_BASE_PACKAGE_NAME = "com.company.app"
const val DEFAULT_INCLUDE_KEYWORD = ""
const val DEFAULT_REFRESH_ON_MODULE_ADD = true
const val DEFAULT_THREE_MODULE_CREATION = false
const val DEFAULT_USE_KTS_FILE_EXTENSION = true
const val DEFAULT_GRADLE_FILE_NAMED_AFTER_MODULE = false
const val DEFAULT_ADD_README = true
const val DEFAULT_ADD_GIT_IGNORE = false
const val DEFAULT_API_MODULE_NAME = "api"
const val DEFAULT_GLUE_MODULE_NAME = "glue"
const val DEFAULT_IMPL_MODULE_NAME = "impl"

class SettingsDialogWrapper(
    private val project: Project,
    private val onSave: () -> Unit,
    private val isKtsCurrentlyChecked: Boolean,
    private val isAndroidChecked: Boolean
) : DialogWrapper(true) {

    private lateinit var kotlinTemplateTextArea: JTextArea
    private lateinit var androidTemplateTextArea: JTextArea

    private lateinit var gitignoreTemplateTextArea: JTextArea

    private lateinit var apiTemplateTextArea: JTextArea
    private lateinit var glueTemplateTextArea: JTextArea
    private lateinit var implTemplateTextArea: JTextArea

    private lateinit var apiModuleNameTextArea: JTextField
    private lateinit var glueModuleNameTextArea: JTextField
    private lateinit var implModuleNameTextArea: JTextField

    private val preferenceService = PreferenceServiceImpl.instance

    private val refreshOnModuleAdd = mutableStateOf(preferenceService.preferenceState.refreshOnModuleAdd)
    private val threeModuleCreation = mutableStateOf(preferenceService.preferenceState.threeModuleCreationDefault)
    private val ktsFileExtension = mutableStateOf(preferenceService.preferenceState.useKtsFileExtension)
    private val gradleFileNamedAfterModule = mutableStateOf(preferenceService.preferenceState.gradleFileNamedAfterModule)
    private val addReadme = mutableStateOf(preferenceService.preferenceState.addReadme)
    private val addGitignore = mutableStateOf(preferenceService.preferenceState.addGitIgnore)

    private val packageNameTextField = mutableStateOf(TextFieldValue(preferenceService.preferenceState.packageName))
    private val includeProjectKeywordTextField = mutableStateOf(TextFieldValue(preferenceService.preferenceState.includeProjectKeyword))

    init {
        title = "Settings"
        init()
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)

        val templateDefaultPanel = createTemplateDefaultComponent()
        val templateEnhancedDefaultPanel = createEnhancedTemplateDefaultComponent()
        val generalPanel = createGeneralPanelCompose()
        val gitignoreTemplateDefaultPanel = createGitIgnoreTemplateDefaultPanel()

        val tabbedPane = JBTabbedPane()
        tabbedPane.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT)
        tabbedPane.add("Module Template Defaults", templateDefaultPanel)
        tabbedPane.add("Enhanced Template Defaults", templateEnhancedDefaultPanel)
        tabbedPane.add(".gitignore Template Defaults", gitignoreTemplateDefaultPanel)
        tabbedPane.add("General", generalPanel)
        dialogPanel.add(tabbedPane)
        dialogPanel.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)

        return dialogPanel
    }

    private fun createGitIgnoreTemplateDefaultPanel(): Component {
        val settingExplanationLabel = JLabel(
            """
            <html>
            You can override the .gitignore templates created with your own project specific default.
            <br/><br/>
            If nothing is specified here, a sensible default will be generated for you.
            </html>
            """.trimIndent()
        )

        val gitignoreTemplateLabel = JLabel(".gitignore Template")
        var gitIgnoreTemplateFromPref = preferenceService.preferenceState.gitignoreTemplate

        if (gitIgnoreTemplateFromPref.isBlank()) {
            gitIgnoreTemplateFromPref = GitIgnoreTemplate.data
        }

        gitignoreTemplateTextArea = JTextArea(
            gitIgnoreTemplateFromPref,
            gitIgnoreTemplateFromPref.getRowsFromText(),
            gitIgnoreTemplateFromPref.getColumnFromText()
        )
        gitignoreTemplateTextArea.addDocumentListener()

        gitignoreTemplateTextArea.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 2)
        val gitignoreTemplateScrollPane = JScrollPane(
            gitignoreTemplateTextArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        gitignoreTemplateScrollPane.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 2 - EXTRA_PADDING * 2)

        val templateDefaultPanel = JPanel()
        val templateDefaultPanelLayout = SpringLayout()
        templateDefaultPanel.layout = templateDefaultPanelLayout
        templateDefaultPanel.add(gitignoreTemplateLabel)
        templateDefaultPanel.add(gitignoreTemplateScrollPane)
        templateDefaultPanel.add(settingExplanationLabel)

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            settingExplanationLabel,
            EXTRA_PADDING,
            SpringLayout.NORTH,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            settingExplanationLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            gitignoreTemplateLabel,
            EXTRA_PADDING * 2,
            SpringLayout.SOUTH,
            settingExplanationLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            gitignoreTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            gitignoreTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            gitignoreTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            gitignoreTemplateLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            gitignoreTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )

        return templateDefaultPanel
    }

    private fun createGeneralPanelCompose(): JComponent {
        return ComposePanel().apply {
            setContent {
                ScrollablePane {
                    var basePackageName by remember { packageNameTextField }

                    TextField(
                        value = basePackageName,
                        onValueChange = { newValue ->
                            basePackageName = newValue
                        },
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif),
                        label = { Text("Base Package Name:") }
                    )

                    var includeKeyword by remember { includeProjectKeywordTextField }

                    TextField(
                        value = includeKeyword,
                        onValueChange = { newValue ->
                            includeKeyword = newValue
                        },
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif),
                        label = { Text("Include keyword for settings.gradle(.kts):") }
                    )

                    val refreshAfterModuleCreationState = remember { refreshOnModuleAdd }
                    LabelledCheckbox(
                        label = "Refresh after creating modules",
                        checked = refreshAfterModuleCreationState.value,
                        onCheckedChange = {
                            refreshAfterModuleCreationState.value = it
                        }
                    )

                    val threeModuleState = remember { threeModuleCreation }
                    LabelledCheckbox(
                        label = "3 module creation checked by default",
                        checked = threeModuleState.value,
                        onCheckedChange = {
                            threeModuleState.value = it
                        }
                    )

                    val useKtsState = remember { ktsFileExtension }
                    LabelledCheckbox(
                        label = "Use .kts file extension checked by default",
                        checked = useKtsState.value,
                        onCheckedChange = {
                            useKtsState.value = it
                        }
                    )

                    val gradleFileNameState = remember { gradleFileNamedAfterModule }
                    LabelledCheckbox(
                        label = "Gradle file named after module by default",
                        checked = gradleFileNameState.value,
                        onCheckedChange = {
                            gradleFileNameState.value = it
                        }
                    )

                    val readmeState = remember { addReadme }
                    LabelledCheckbox(
                        label = "Add README.md by default",
                        checked = readmeState.value,
                        onCheckedChange = {
                            readmeState.value = it
                        }
                    )

                    val gitIgnoreState = remember { addGitignore }
                    LabelledCheckbox(
                        label = "Add .gitignore by default",
                        checked = addGitignore.value,
                        onCheckedChange = {
                            gitIgnoreState.value = it
                        }
                    )

                    Button(
                        onClick = {
                            importSettings()
                        }
                    ) {
                        Text("Import Settings")
                    }

                    Button(
                        onClick = {
                            exportSettings()
                        }
                    ) {
                        Text("Export Settings")
                    }

                    Button(
                        onClick = {
                            clearData()
                        }
                    ) {
                        Text("Clear All Settings")
                    }
                }
            }
        }
    }

    @Composable
    fun ScrollablePane(
        modifier: Modifier = Modifier,
        content: @Composable
        (ColumnScope) -> Unit
    ) {
        WidgetTheme {
            Box(
                modifier = modifier.fillMaxSize()
                    .padding(8.dp)
            ) {
                val stateVertical = rememberScrollState(0)
                val stateHorizontal = rememberScrollState(0)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(stateVertical)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .horizontalScroll(stateHorizontal)
                ) {
                    Column {
                        content(this)
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(stateVertical)
                )
                HorizontalScrollbar(
                    modifier = Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    adapter = rememberScrollbarAdapter(stateHorizontal)
                )
            }
        }
    }

    @Composable
    fun LabelledCheckbox(
        modifier: Modifier = Modifier,
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    onCheckedChange(it)
                },
                enabled = true
            )
            Text(text = label)
        }
    }

    private fun importSettings() {
        FileChooser.chooseFile(
            FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false
            ),
            project,
            null
        ) {
            val path: Path = Paths.get(it.path)

            val data: String = Files.readAllLines(path)[0]

            val state = Json.decodeFromString<PreferenceServiceImpl.Companion.State>(data)

            androidTemplateTextArea.text = state.androidTemplate
            kotlinTemplateTextArea.text = state.kotlinTemplate
            apiTemplateTextArea.text = state.apiTemplate
            implTemplateTextArea.text = state.implTemplate
            glueTemplateTextArea.text = state.glueTemplate
            packageNameTextField.value = TextFieldValue(state.packageName)
            includeProjectKeywordTextField.value = TextFieldValue(state.includeProjectKeyword)
            refreshOnModuleAdd.value = state.refreshOnModuleAdd
            threeModuleCreation.value = state.threeModuleCreationDefault
            ktsFileExtension.value = state.useKtsFileExtension
            gradleFileNamedAfterModule.value = state.gradleFileNamedAfterModule
            addReadme.value = state.addReadme
            addGitignore.value = state.addGitIgnore
            gitignoreTemplateTextArea.text = state.gitignoreTemplate
        }
    }

    private fun exportSettings() {
        val test = FileChooserFactory.getInstance().createSaveFileDialog(
            FileSaverDescriptor(
                "Select Location",
                "",
                ".json"
            ),
            project
        )
        val wrapper = test.save("module_maker_settings.json")
        if (wrapper != null) {
            try {
                val writer = FileWriter(wrapper.file.absolutePath)
                val json = getJsonFromSettings()
                writer.write(json)
                writer.close()
            } catch (e: Exception) {
                Notifications.showExportError(project)
            }
        }
    }

    private fun getJsonFromSettings(): String {
        val androidTemplate: String = androidTemplateTextArea.text
        val kotlinTemplate: String = kotlinTemplateTextArea.text
        val apiTemplate: String = apiTemplateTextArea.text
        val glueTemplate: String = glueTemplateTextArea.text
        val implTemplate: String = implTemplateTextArea.text
        val packageName: String = packageNameTextField.value.text
        val includeProject: String = includeProjectKeywordTextField.value.text
        val shouldRefresh = refreshOnModuleAdd.value
        val threeModuleCreationDefault = threeModuleCreation.value
        val useKtsFileExtension = ktsFileExtension.value
        val gradleFileNamedAfterModule = gradleFileNamedAfterModule.value
        val addReadme = addReadme.value
        val addGitignore = addGitignore.value
        val gitignoreTemplate = gitignoreTemplateTextArea.text

        // if more parameters get added, add support to import / export
        val newState = PreferenceServiceImpl.Companion.State(
            androidTemplate = androidTemplate,
            kotlinTemplate = kotlinTemplate,
            apiTemplate = apiTemplate,
            glueTemplate = glueTemplate,
            implTemplate = implTemplate,
            packageName = packageName,
            includeProjectKeyword = includeProject,
            refreshOnModuleAdd = shouldRefresh,
            threeModuleCreationDefault = threeModuleCreationDefault,
            useKtsFileExtension = useKtsFileExtension,
            gradleFileNamedAfterModule = gradleFileNamedAfterModule,
            addReadme = addReadme,
            addGitIgnore = addGitignore,
            gitignoreTemplate = gitignoreTemplate
        )

        return Json.encodeToString(newState)
    }

    private fun createTemplateDefaultComponent(): JComponent {
        val settingExplanationLabel = JLabel(
            """
            <html>
            You can override the gradle templates created with your own project specific defaults.
            <br/><br/>
            If nothing is specified here, a sensible default will be generated for you.
            </html>
            """.trimIndent()
        )

        val supportedVariablesString = TemplateVariable.values().joinToString("<br/>") {
            it.templateVariable
        }

        val supportedVariablesLabel = JLabel(
            """
            <html>
            If you do have a custom template, there are some variable names that will be automatically replaced for you.
            <br/><br/>
            Supported variables are:
            <br/><br/>
            $supportedVariablesString
            </html>
            """.trimIndent()
        )

        val kotlinTemplateLabel = JLabel("Kotlin Template")
        var kotlinTemplateFromPref = preferenceService.preferenceState.kotlinTemplate

        if (kotlinTemplateFromPref.isBlank()) {
            kotlinTemplateFromPref = if (isKtsCurrentlyChecked) {
                KotlinModuleTemplate.data
            } else {
                KotlinModuleKtsTemplate.data
            }
        }

        kotlinTemplateTextArea = JTextArea(
            kotlinTemplateFromPref,
            kotlinTemplateFromPref.getRowsFromText(),
            kotlinTemplateFromPref.getColumnFromText()
        )
        kotlinTemplateTextArea.addDocumentListener()

        kotlinTemplateTextArea.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 3)
        val kotlinTemplateScrollPane = JScrollPane(
            kotlinTemplateTextArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        kotlinTemplateScrollPane.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 3 - EXTRA_PADDING * 2)

        val androidTemplateLabel = JLabel("Android Template")
        var androidTemplateFromPref = preferenceService.preferenceState.androidTemplate

        if (androidTemplateFromPref.isBlank()) {
            androidTemplateFromPref = if (isKtsCurrentlyChecked) {
                AndroidModuleKtsTemplate.data
            } else {
                AndroidModuleTemplate.data
            }
        }

        androidTemplateTextArea = JTextArea(
            androidTemplateFromPref,
            androidTemplateFromPref.getRowsFromText(),
            androidTemplateFromPref.getColumnFromText()
        )
        androidTemplateTextArea.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 3 + EXTRA_PADDING * 2)
        androidTemplateTextArea.addDocumentListener()
        val androidTemplateScrollPane = JScrollPane(
            androidTemplateTextArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        androidTemplateScrollPane.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 3 - EXTRA_PADDING * 2)

        val templateDefaultPanel = JPanel()

        val templateDefaultPanelLayout = SpringLayout()
        templateDefaultPanel.layout = templateDefaultPanelLayout
        templateDefaultPanel.add(kotlinTemplateLabel)
        templateDefaultPanel.add(kotlinTemplateScrollPane)
        templateDefaultPanel.add(androidTemplateLabel)
        templateDefaultPanel.add(androidTemplateScrollPane)
        templateDefaultPanel.add(settingExplanationLabel)
        templateDefaultPanel.add(supportedVariablesLabel)

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            settingExplanationLabel,
            EXTRA_PADDING,
            SpringLayout.NORTH,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            settingExplanationLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            kotlinTemplateLabel,
            EXTRA_PADDING * 2,
            SpringLayout.SOUTH,
            settingExplanationLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            kotlinTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            kotlinTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            kotlinTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            kotlinTemplateLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            kotlinTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            androidTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            androidTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            kotlinTemplateScrollPane
        )

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            androidTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            androidTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            androidTemplateLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            androidTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )

        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            supportedVariablesLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            androidTemplateScrollPane
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            supportedVariablesLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )

        templateDefaultPanel.preferredSize = templateDefaultPanel.getPreferredDimensionForComponent()

        return JScrollPane(
            templateDefaultPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
    }

    private fun createEnhancedTemplateDefaultComponent(): JComponent {
        val apiTemplateLabel = JLabel("Api Template")
        var apiTemplateFromPref = preferenceService.preferenceState.apiTemplate

        if (apiTemplateFromPref.isBlank()) {
            apiTemplateFromPref = getDefaultTemplate()
        }

        apiTemplateTextArea = JTextArea(
            apiTemplateFromPref,
            apiTemplateFromPref.getRowsFromText(),
            apiTemplateFromPref.getColumnFromText()
        )
        apiTemplateTextArea.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 4)
        apiTemplateTextArea.addDocumentListener()
        val apiTemplateScrollPane = JScrollPane(
            apiTemplateTextArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        apiTemplateScrollPane.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 4 - EXTRA_PADDING * 2)

        val glueTemplateLabel = JLabel("Glue Template")
        var glueTemplateFromPref = preferenceService.preferenceState.glueTemplate

        if (glueTemplateFromPref.isBlank()) {
            glueTemplateFromPref = getDefaultTemplate()
        }

        glueTemplateTextArea = JTextArea(
            glueTemplateFromPref,
            glueTemplateFromPref.getRowsFromText(),
            glueTemplateFromPref.getColumnFromText()
        )
        glueTemplateTextArea.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 4)
        glueTemplateTextArea.addDocumentListener()
        val glueTemplateScrollPane = JScrollPane(
            glueTemplateTextArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        glueTemplateScrollPane.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 4 - EXTRA_PADDING * 2)

        val implTemplateLabel = JLabel("Impl Template")
        var implTemplateFromPref = preferenceService.preferenceState.implTemplate

        if (implTemplateFromPref.isBlank()) {
            implTemplateFromPref = getDefaultTemplate()
        }

        implTemplateTextArea = JTextArea(
            implTemplateFromPref,
            implTemplateFromPref.getRowsFromText(),
            implTemplateFromPref.getColumnFromText()
        )
        implTemplateTextArea.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 4)
        implTemplateTextArea.addDocumentListener()
        val implTemplateScrollPane = JScrollPane(
            implTemplateTextArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        implTemplateScrollPane.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT / 4 - EXTRA_PADDING * 2)

        val apiModuleNameLabel = JLabel("api module name")
        apiModuleNameTextArea = JTextField(preferenceService.preferenceState.apiModuleName)

        val glueModuleNameLabel = JLabel("glue module name")
        glueModuleNameTextArea = JTextField(preferenceService.preferenceState.glueModuleName)

        val implModuleNameLabel = JLabel("impl module name")
        implModuleNameTextArea = JTextField(preferenceService.preferenceState.implModuleName)

        val templateDefaultPanel = JPanel()
        val templateDefaultPanelLayout = SpringLayout()
        templateDefaultPanel.layout = templateDefaultPanelLayout

        templateDefaultPanel.add(apiTemplateLabel)
        templateDefaultPanel.add(apiTemplateScrollPane)
        templateDefaultPanel.add(glueTemplateLabel)
        templateDefaultPanel.add(glueTemplateScrollPane)
        templateDefaultPanel.add(implTemplateLabel)
        templateDefaultPanel.add(implTemplateScrollPane)

        templateDefaultPanel.add(apiModuleNameTextArea)
        templateDefaultPanel.add(implModuleNameTextArea)
        templateDefaultPanel.add(glueModuleNameTextArea)

        templateDefaultPanel.add(apiModuleNameLabel)
        templateDefaultPanel.add(glueModuleNameLabel)
        templateDefaultPanel.add(implModuleNameLabel)

        // api label
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            apiTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.NORTH,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            apiTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )

        // api text area
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            apiTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            apiTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            apiTemplateLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            apiTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )

        // glue label
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            glueTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            glueTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            apiTemplateScrollPane
        )

        // glue text area
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            glueTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            glueTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            glueTemplateLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            glueTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )

        // impl label
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            implTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            implTemplateLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            glueTemplateScrollPane
        )

        // impl text area
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            implTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            implTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            implTemplateLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            implTemplateScrollPane,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )

        // api module name label + text area
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            apiModuleNameTextArea,
            EXTRA_PADDING,
            SpringLayout.EAST,
            apiModuleNameLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            apiModuleNameTextArea,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.BASELINE,
            apiModuleNameTextArea,
            0,
            SpringLayout.BASELINE,
            apiModuleNameLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            apiModuleNameLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            apiModuleNameLabel,
            EXTRA_PADDING * 2,
            SpringLayout.SOUTH,
            implTemplateScrollPane
        )

        // glue module name label + text area
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            glueModuleNameTextArea,
            EXTRA_PADDING,
            SpringLayout.EAST,
            glueModuleNameLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            glueModuleNameTextArea,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.BASELINE,
            glueModuleNameTextArea,
            0,
            SpringLayout.BASELINE,
            glueModuleNameLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            glueModuleNameLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            glueModuleNameLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            apiModuleNameTextArea
        )

        // impl module name label + text area
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            implModuleNameTextArea,
            EXTRA_PADDING,
            SpringLayout.EAST,
            implModuleNameLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.EAST,
            implModuleNameTextArea,
            EXTRA_PADDING,
            SpringLayout.EAST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.BASELINE,
            implModuleNameTextArea,
            0,
            SpringLayout.BASELINE,
            implModuleNameLabel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.WEST,
            implModuleNameLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            templateDefaultPanel
        )
        templateDefaultPanelLayout.putConstraint(
            SpringLayout.NORTH,
            implModuleNameLabel,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            glueModuleNameTextArea
        )

        templateDefaultPanel.preferredSize = templateDefaultPanel.getPreferredDimensionForComponent()

        return JScrollPane(
            templateDefaultPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        )
    }

    override fun createActions(): Array<Action> {
        return arrayOf(
            DialogWrapperExitAction(
                "Cancel",
                DEFAULT_EXIT_CODE
            ),
            object : AbstractAction("Save") {
                override fun actionPerformed(e: ActionEvent?) {
                    saveDate()
                    onSave()
                    close(DEFAULT_EXIT_CODE)
                }
            }
        )
    }

    private fun saveDate() {
        preferenceService.preferenceState = preferenceService.preferenceState.copy(
            androidTemplate = androidTemplateTextArea.text,
            kotlinTemplate = kotlinTemplateTextArea.text,
            apiTemplate = apiTemplateTextArea.text,
            implTemplate = implTemplateTextArea.text,
            glueTemplate = glueTemplateTextArea.text,
            packageName = packageNameTextField.value.text,
            includeProjectKeyword = includeProjectKeywordTextField.value.text,
            refreshOnModuleAdd = refreshOnModuleAdd.value,
            threeModuleCreationDefault = threeModuleCreation.value,
            useKtsFileExtension = ktsFileExtension.value,
            gradleFileNamedAfterModule = gradleFileNamedAfterModule.value,
            addReadme = addReadme.value,
            addGitIgnore = addGitignore.value,
            gitignoreTemplate = gitignoreTemplateTextArea.text,
            apiModuleName = apiModuleNameTextArea.text,
            glueModuleName = glueModuleNameTextArea.text,
            implModuleName = implModuleNameTextArea.text
        )
    }

    private fun clearData() {
        androidTemplateTextArea.text = ""
        kotlinTemplateTextArea.text = ""
        apiTemplateTextArea.text = ""
        implTemplateTextArea.text = ""
        glueTemplateTextArea.text = ""
        gitignoreTemplateTextArea.text = ""
        packageNameTextField.value = TextFieldValue(DEFAULT_BASE_PACKAGE_NAME)
        includeProjectKeywordTextField.value = TextFieldValue(DEFAULT_INCLUDE_KEYWORD)
        refreshOnModuleAdd.value = DEFAULT_REFRESH_ON_MODULE_ADD
        threeModuleCreation.value = DEFAULT_THREE_MODULE_CREATION
        ktsFileExtension.value = DEFAULT_USE_KTS_FILE_EXTENSION
        gradleFileNamedAfterModule.value = DEFAULT_GRADLE_FILE_NAMED_AFTER_MODULE
        addReadme.value = DEFAULT_ADD_README
        addGitignore.value = DEFAULT_ADD_GIT_IGNORE

        implModuleNameTextArea.text = DEFAULT_IMPL_MODULE_NAME
        glueModuleNameTextArea.text = DEFAULT_GLUE_MODULE_NAME
        apiModuleNameTextArea.text = DEFAULT_API_MODULE_NAME
    }

    private fun String.getRowsFromText(): Int {
        return this.lines().count()
    }

    private fun String.getColumnFromText(): Int {
        return this.lines().maxOf {
            it.length
        }
    }

    private fun JTextArea.addDocumentListener() {
        val currentTextArea = this
        this.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
                currentTextArea.rows = currentTextArea.text.getRowsFromText()
                currentTextArea.columns = currentTextArea.text.getColumnFromText()
            }

            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
                currentTextArea.rows = currentTextArea.text.getRowsFromText()
                currentTextArea.columns = currentTextArea.text.getColumnFromText()
            }

            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
                currentTextArea.rows = currentTextArea.text.getRowsFromText()
                currentTextArea.columns = currentTextArea.text.getColumnFromText()
            }
        })
    }

    private fun getDefaultTemplate(): String {
        return if (isAndroidChecked) {
            if (isKtsCurrentlyChecked) {
                AndroidModuleKtsTemplate.data
            } else {
                AndroidModuleTemplate.data
            }
        } else {
            if (isKtsCurrentlyChecked) {
                KotlinModuleKtsTemplate.data
            } else {
                KotlinModuleTemplate.data
            }
        }
    }

    private fun JComponent.getPreferredDimensionForComponent(): Dimension {
        var totalHeight = 0
        for (component in this.components) {
            val preferredSize = component.preferredSize
            totalHeight += preferredSize.height
        }

        return Dimension(WINDOW_WIDTH - SCROLLBAR_WIDTH, totalHeight + SCROLLBAR_WIDTH * 2)
    }
}
