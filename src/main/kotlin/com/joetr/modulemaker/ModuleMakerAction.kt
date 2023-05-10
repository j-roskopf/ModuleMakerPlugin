package com.joetr.modulemaker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ModuleMakerAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ModuleMakerDialogWrapper().show()
    }
}
