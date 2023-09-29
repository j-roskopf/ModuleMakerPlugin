package com.joetr.modulemaker.data

interface File {
    val name: String
    val absolutePath: String
    val isDirectory: Boolean
    val children: List<File>
    val hasChildren: Boolean
}
