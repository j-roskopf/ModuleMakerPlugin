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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
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

    private val preferenceService = PreferenceServiceImpl.instance

    private val refreshOnModuleAdd = mutableStateOf(preferenceService.preferenceState.refreshOnModuleAdd)
    private val threeModuleCreation = mutableStateOf(preferenceService.preferenceState.threeModuleCreationDefault)
    private val ktsFileExtension = mutableStateOf(preferenceService.preferenceState.useKtsFileExtension)
    private val gradleFileNamedAfterModule =
        mutableStateOf(preferenceService.preferenceState.gradleFileNamedAfterModule)
    private val addReadme = mutableStateOf(preferenceService.preferenceState.addReadme)
    private val addGitignore = mutableStateOf(preferenceService.preferenceState.addGitIgnore)

    private val packageNameTextField = mutableStateOf(TextFieldValue(preferenceService.preferenceState.packageName))
    private val gitignoreTemplateTextArea =
        mutableStateOf(TextFieldValue(preferenceService.preferenceState.gitignoreTemplate))
    private val includeProjectKeywordTextField =
        mutableStateOf(TextFieldValue(preferenceService.preferenceState.includeProjectKeyword))

    private val apiTemplateTextArea = mutableStateOf(TextFieldValue(preferenceService.preferenceState.apiTemplate))
    private val glueTemplateTextArea = mutableStateOf(TextFieldValue(preferenceService.preferenceState.glueTemplate))
    private val implTemplateTextArea = mutableStateOf(TextFieldValue(preferenceService.preferenceState.implTemplate))

    private val apiModuleNameTextArea = mutableStateOf(TextFieldValue(preferenceService.preferenceState.apiModuleName))
    private val glueModuleNameTextArea =
        mutableStateOf(TextFieldValue(preferenceService.preferenceState.glueModuleName))
    private val implModuleNameTextArea =
        mutableStateOf(TextFieldValue(preferenceService.preferenceState.implModuleName))

    init {
        title = "Settings"
        init()
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)

        val templateDefaultPanel = createTemplateDefaultComponent()
        val templateEnhancedDefaultPanel = createEnhancedTemplateDefaultComponentCompose()
        val generalPanel = createGeneralPanelCompose()
        val gitignoreTemplateDefaultPanel = createGitIgnoreTemplateDefaultPanelCompose()

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

    private fun createGitIgnoreTemplateDefaultPanelCompose(): Component {
        return ComposePanel().apply {
            setContent {
                WidgetTheme {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = "You can override the .gitignore templates created with your own project specific default.\n\nIf nothing is specified here, a sensible default will be generated for you."
                        )

                        val gitIgnoreTemplateState = remember { gitignoreTemplateTextArea }

                        OutlinedTextField(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            value = gitIgnoreTemplateState.value,
                            onValueChange = {
                                gitIgnoreTemplateState.value = it
                            }
                        )
                    }
                }
            }
        }
    }

    private fun createGeneralPanelCompose(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT)
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
                    Column(modifier = Modifier.fillMaxSize()) {
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
            apiTemplateTextArea.value = TextFieldValue(state.apiTemplate)
            implTemplateTextArea.value = TextFieldValue(state.implTemplate)
            glueTemplateTextArea.value = TextFieldValue(state.glueTemplate)
            packageNameTextField.value = TextFieldValue(state.packageName)
            includeProjectKeywordTextField.value = TextFieldValue(state.includeProjectKeyword)
            refreshOnModuleAdd.value = state.refreshOnModuleAdd
            threeModuleCreation.value = state.threeModuleCreationDefault
            ktsFileExtension.value = state.useKtsFileExtension
            gradleFileNamedAfterModule.value = state.gradleFileNamedAfterModule
            addReadme.value = state.addReadme
            addGitignore.value = state.addGitIgnore
            gitignoreTemplateTextArea.value = TextFieldValue(state.gitignoreTemplate)
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
        val apiTemplate: String = apiTemplateTextArea.value.text
        val glueTemplate: String = glueTemplateTextArea.value.text
        val implTemplate: String = implTemplateTextArea.value.text
        val packageName: String = packageNameTextField.value.text
        val includeProject: String = includeProjectKeywordTextField.value.text
        val shouldRefresh = refreshOnModuleAdd.value
        val threeModuleCreationDefault = threeModuleCreation.value
        val useKtsFileExtension = ktsFileExtension.value
        val gradleFileNamedAfterModule = gradleFileNamedAfterModule.value
        val addReadme = addReadme.value
        val addGitignore = addGitignore.value
        val gitignoreTemplate = gitignoreTemplateTextArea.value.text

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

    private fun createEnhancedTemplateDefaultComponentCompose(): JComponent {
        return ComposePanel().apply {
            setContent {
                WidgetTheme {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        val apiTemplateState = remember { apiTemplateTextArea }
                        OutlinedTextField(
                            label = { Text("Api Template") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                                .defaultMinSize(minHeight = (WINDOW_HEIGHT / 3).dp),
                            value = apiTemplateState.value,
                            onValueChange = {
                                apiTemplateState.value = it
                            }
                        )

                        val glueTemplateState = remember { glueTemplateTextArea }
                        OutlinedTextField(
                            label = { Text("Glue Template") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                                .defaultMinSize(minHeight = (WINDOW_HEIGHT / 3).dp),
                            value = glueTemplateState.value,
                            onValueChange = {
                                glueTemplateState.value = it
                            }
                        )

                        val implTemplateState = remember { implTemplateTextArea }
                        OutlinedTextField(
                            label = { Text("Impl Template") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                                .defaultMinSize(minHeight = (WINDOW_HEIGHT / 3).dp),
                            value = implTemplateState.value,
                            onValueChange = {
                                implTemplateState.value = it
                            }
                        )

                        val apiModuleNameState = remember { apiModuleNameTextArea }

                        OutlinedTextField(
                            label = { Text("Api Module Name") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            value = apiModuleNameState.value,
                            onValueChange = {
                                apiModuleNameState.value = it
                            }
                        )

                        val glueModuleNameState = remember { glueModuleNameTextArea }

                        OutlinedTextField(
                            label = { Text("Glue Module Name") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            value = glueModuleNameState.value,
                            onValueChange = {
                                glueModuleNameState.value = it
                            }
                        )

                        val implModuleNameState = remember { implModuleNameTextArea }

                        OutlinedTextField(
                            label = { Text("Impl Module Name") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            value = implModuleNameState.value,
                            onValueChange = {
                                implModuleNameState.value = it
                            }
                        )
                    }
                }
            }
        }
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
            apiTemplate = apiTemplateTextArea.value.text,
            implTemplate = implTemplateTextArea.value.text,
            glueTemplate = glueTemplateTextArea.value.text,
            packageName = packageNameTextField.value.text,
            includeProjectKeyword = includeProjectKeywordTextField.value.text,
            refreshOnModuleAdd = refreshOnModuleAdd.value,
            threeModuleCreationDefault = threeModuleCreation.value,
            useKtsFileExtension = ktsFileExtension.value,
            gradleFileNamedAfterModule = gradleFileNamedAfterModule.value,
            addReadme = addReadme.value,
            addGitIgnore = addGitignore.value,
            gitignoreTemplate = gitignoreTemplateTextArea.value.text,
            apiModuleName = apiModuleNameTextArea.value.text,
            glueModuleName = glueModuleNameTextArea.value.text,
            implModuleName = implModuleNameTextArea.value.text
        )
    }

    private fun clearData() {
        androidTemplateTextArea.text = getDefaultTemplate()
        kotlinTemplateTextArea.text = getDefaultTemplate(isKotlin = true)
        apiTemplateTextArea.value = TextFieldValue(getDefaultTemplate())
        implTemplateTextArea.value = TextFieldValue(getDefaultTemplate())
        glueTemplateTextArea.value = TextFieldValue(getDefaultTemplate())
        gitignoreTemplateTextArea.value = TextFieldValue(GitIgnoreTemplate.data)
        packageNameTextField.value = TextFieldValue(DEFAULT_BASE_PACKAGE_NAME)
        includeProjectKeywordTextField.value = TextFieldValue(DEFAULT_INCLUDE_KEYWORD)
        refreshOnModuleAdd.value = DEFAULT_REFRESH_ON_MODULE_ADD
        threeModuleCreation.value = DEFAULT_THREE_MODULE_CREATION
        ktsFileExtension.value = DEFAULT_USE_KTS_FILE_EXTENSION
        gradleFileNamedAfterModule.value = DEFAULT_GRADLE_FILE_NAMED_AFTER_MODULE
        addReadme.value = DEFAULT_ADD_README
        addGitignore.value = DEFAULT_ADD_GIT_IGNORE

        implModuleNameTextArea.value = TextFieldValue(DEFAULT_IMPL_MODULE_NAME)
        glueModuleNameTextArea.value = TextFieldValue(DEFAULT_GLUE_MODULE_NAME)
        apiModuleNameTextArea.value = TextFieldValue(DEFAULT_API_MODULE_NAME)
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

    private fun getDefaultTemplate(isKotlin: Boolean = false): String {
        if (isKotlin) {
            return if (isKtsCurrentlyChecked) {
                KotlinModuleKtsTemplate.data
            } else {
                KotlinModuleTemplate.data
            }
        }

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
