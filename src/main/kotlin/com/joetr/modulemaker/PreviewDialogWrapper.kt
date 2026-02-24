/*
 * Copyrightest (c) 2024 Joseph Roskopf
 */

package com.joetr.modulemaker

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.io.FileUtilRt
import com.joetr.modulemaker.data.toProjectFile
import com.joetr.modulemaker.ui.file.FileTree
import com.joetr.modulemaker.ui.file.FileTreeView
import com.joetr.modulemaker.ui.theme.WidgetTheme
import java.io.File
import javax.swing.Action
import javax.swing.JComponent

private const val WINDOW_WIDTH = 400
private const val WINDOW_HEIGHT = 600

class PreviewDialogWrapper(val filesToBeCreated: List<File>, val root: String) : DialogWrapper(true) {

    private var tempRoot: File

    init {
        title = "Preview"
        init()

        tempRoot = FileUtilRt.createTempDirectory(root, null, true)

        createFileStructure(
            tempRoot,
            filesToBeCreated.map {
                val pathToRoot = tempRoot.absolutePath
                val split = it.absolutePath.split(root)

                // splice together the files to have a root of our temp folder
                File(pathToRoot, split.drop(1).joinToString(separator = ""))
            }
        )
    }

    override fun dispose() {
        super.dispose()
        tempRoot.parentFile.deleteRecursively()
    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT)
            setContent {
                WidgetTheme {
                    FileTreeJPanel(
                        modifier = Modifier.height(WINDOW_HEIGHT.dp).width(WINDOW_WIDTH.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun FileTreeJPanel(
        modifier: Modifier = Modifier
    ) {
        val height = remember { mutableStateOf(WINDOW_HEIGHT) }

        FileTreeView(
            modifier = modifier,
            model = FileTree(root = tempRoot.toProjectFile()),
            height = height.value.dp,
            onClick = { }
        )
    }

    private fun List<File>.root(): File {
        return this.minBy { file ->
            file.absolutePath.count { it.toString() == File.separator }
        }
    }

    private fun createFileStructure(root: File, structure: List<File>) {
        root.mkdirs()
        structure.forEach {
            it.mkdirs()
            if (it.isDirectory.not() && it.extension.isEmpty().not()) {
                it.writeText("")
            }
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(
            DialogWrapperExitAction(
                "Okay",
                2
            )
        )
    }
}
