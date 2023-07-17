package com.joetr.modulemaker.ui

import com.joetr.modulemaker.data.FileTreeNode
import java.awt.Component
import javax.swing.JFileChooser
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class ModuleMakerTreeCellRenderer : DefaultTreeCellRenderer() {

    private val chooser = JFileChooser()

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

        if (value is DefaultMutableTreeNode) {
            if (value.userObject is FileTreeNode) {
                val fileTreeNode: FileTreeNode = value.userObject as FileTreeNode
                icon = chooser.getIcon(fileTreeNode.file)

                text = fileTreeNode.displayName
            }
        }
        return this
    }
}
