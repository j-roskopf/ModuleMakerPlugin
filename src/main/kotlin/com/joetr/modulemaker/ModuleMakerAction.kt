package com.joetr.modulemaker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

class ModuleMakerAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.getRequiredData(CommonDataKeys.PROJECT)

        ModuleMakerDialogWrapper(
            project = project
        ).show()
    }
}
