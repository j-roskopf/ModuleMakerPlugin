package com.joetr.modulemaker.data

import java.io.File

/**
 * Represents data for a node in our file tree
 */
data class FileTreeNode(
    val displayName: String,
    val file: File,
    val isFolder: Boolean
)
