package com.joetr.modulemaker.ui

import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom tree node
 */
class ModuleMakerTreeNode(
    private val isDirectory: Boolean,
    val data: Any
) : DefaultMutableTreeNode(
    data
) {
    /**
     * Since we only load one level at a time in the file tree, to know if a leaf node is a leaf node,
     * look at the file directory status.
     */
    override fun isLeaf(): Boolean {
        return isDirectory.not()
    }
}
