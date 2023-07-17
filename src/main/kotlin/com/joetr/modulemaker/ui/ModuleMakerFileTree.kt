package com.joetr.modulemaker.ui

import com.joetr.modulemaker.data.FileTreeNode
import java.io.File
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

class ModuleMakerFileTree(val customTreeModel: DefaultTreeModel) : JTree(customTreeModel) {

    fun getNodesAtFilePath(filePath: String): List<MutableTreeNode> {
        val node = File(filePath)

        val toReturn = mutableListOf<MutableTreeNode>()
        if (node.isDirectory) {
            for (child in node.listFiles()!!) {
                val fileTreeNode = FileTreeNode(
                    displayName = child.name,
                    file = child,
                    isFolder = child.isDirectory
                )
                val childNode = ModuleMakerTreeNode(child.isDirectory, fileTreeNode)
                toReturn.add(childNode)
            }
        }

        // sort by files to display first, then by display name
        return toReturn.sortedWith(
            compareBy(
                {
                    ((it as DefaultMutableTreeNode).userObject as FileTreeNode).isFolder.not()
                },
                {
                    ((it as DefaultMutableTreeNode).userObject as FileTreeNode).displayName
                }
            )
        )
    }
}
