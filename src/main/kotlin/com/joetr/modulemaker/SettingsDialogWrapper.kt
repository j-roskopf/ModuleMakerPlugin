@file:Suppress("UndesirableClassUsage")

package com.joetr.modulemaker

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.joetr.modulemaker.persistence.PreferenceServiceImpl
import com.joetr.modulemaker.template.AndroidModuleKtsTemplate
import com.joetr.modulemaker.template.AndroidModuleTemplate
import com.joetr.modulemaker.template.KotlinModuleKtsTemplate
import com.joetr.modulemaker.template.KotlinModuleTemplate
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ScrollPaneConstants
import javax.swing.SpringLayout
import javax.swing.event.DocumentListener

private const val WINDOW_WIDTH = 600
private const val WINDOW_HEIGHT = 900

const val DEFAULT_BASE_PACKAGE_NAME = "com.company.app"
const val DEFAULT_REFRESH_ON_MODULE_ADD = true

class SettingsDialogWrapper(
    private val onSave: () -> Unit,
    private val isKtsCurrentlyChecked: Boolean,
    private val isAndroidChecked: Boolean
) : DialogWrapper(true) {

    private lateinit var kotlinTemplateTextArea: JTextArea
    private lateinit var androidTemplateTextArea: JTextArea

    private lateinit var apiTemplateTextArea: JTextArea
    private lateinit var glueTemplateTextArea: JTextArea
    private lateinit var implTemplateTextArea: JTextArea

    private lateinit var packageNameTextField: JTextField

    private lateinit var refreshOnModuleAdd: JCheckBox

    private val preferenceService = PreferenceServiceImpl.instance

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
        val generalPanel = createGeneralPanel()

        val tabbedPane = JBTabbedPane()
        tabbedPane.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT)
        tabbedPane.add("Template Defaults", templateDefaultPanel)
        tabbedPane.add("Enhanced Template Defaults", templateEnhancedDefaultPanel)
        tabbedPane.add("General", generalPanel)
        dialogPanel.add(tabbedPane)
        dialogPanel.preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)

        return dialogPanel
    }

    private fun createGeneralPanel(): JComponent {
        val clearSettingsButton = JButton("Clear All Settings")
        clearSettingsButton.addActionListener {
            clearData()
        }

        val panel = JPanel()
        val layout = SpringLayout()
        panel.layout = layout

        val packageNameTextLabel = JLabel("Base Package Name: ")
        packageNameTextField = JTextField(preferenceService.preferenceState.packageName)

        refreshOnModuleAdd = JCheckBox("Refresh after creating module")
        refreshOnModuleAdd.isSelected = preferenceService.preferenceState.refreshOnModuleAdd

        panel.add(clearSettingsButton)
        panel.add(packageNameTextLabel)
        panel.add(packageNameTextField)
        panel.add(refreshOnModuleAdd)

        layout.putConstraint(
            SpringLayout.NORTH,
            packageNameTextLabel,
            EXTRA_PADDING,
            SpringLayout.NORTH,
            panel
        )
        layout.putConstraint(
            SpringLayout.WEST,
            packageNameTextLabel,
            EXTRA_PADDING,
            SpringLayout.WEST,
            panel
        )

        layout.putConstraint(
            SpringLayout.BASELINE,
            packageNameTextField,
            0,
            SpringLayout.BASELINE,
            packageNameTextLabel
        )
        layout.putConstraint(
            SpringLayout.WEST,
            packageNameTextField,
            0,
            SpringLayout.EAST,
            packageNameTextLabel
        )
        layout.putConstraint(
            SpringLayout.EAST,
            packageNameTextField,
            EXTRA_PADDING,
            SpringLayout.EAST,
            panel
        )

        layout.putConstraint(
            SpringLayout.WEST,
            refreshOnModuleAdd,
            EXTRA_PADDING,
            SpringLayout.WEST,
            panel
        )
        layout.putConstraint(
            SpringLayout.NORTH,
            refreshOnModuleAdd,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            packageNameTextField
        )

        layout.putConstraint(
            SpringLayout.NORTH,
            clearSettingsButton,
            EXTRA_PADDING,
            SpringLayout.SOUTH,
            refreshOnModuleAdd
        )
        layout.putConstraint(
            SpringLayout.WEST,
            clearSettingsButton,
            EXTRA_PADDING,
            SpringLayout.WEST,
            panel
        )

        return panel
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

        return templateDefaultPanel
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

        val templateDefaultPanel = JPanel()
        val templateDefaultPanelLayout = SpringLayout()
        templateDefaultPanel.layout = templateDefaultPanelLayout

        templateDefaultPanel.add(apiTemplateLabel)
        templateDefaultPanel.add(apiTemplateScrollPane)
        templateDefaultPanel.add(glueTemplateLabel)
        templateDefaultPanel.add(glueTemplateScrollPane)
        templateDefaultPanel.add(implTemplateLabel)
        templateDefaultPanel.add(implTemplateScrollPane)

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

        return templateDefaultPanel
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
            packageName = packageNameTextField.text,
            refreshOnModuleAdd = refreshOnModuleAdd.isSelected
        )
    }

    private fun clearData() {
        androidTemplateTextArea.text = ""
        kotlinTemplateTextArea.text = ""
        apiTemplateTextArea.text = ""
        implTemplateTextArea.text = ""
        glueTemplateTextArea.text = ""
        packageNameTextField.text = DEFAULT_BASE_PACKAGE_NAME
        refreshOnModuleAdd.isSelected = DEFAULT_REFRESH_ON_MODULE_ADD
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
}
