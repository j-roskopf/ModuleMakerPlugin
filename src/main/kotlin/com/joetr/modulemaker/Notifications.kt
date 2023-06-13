package com.joetr.modulemaker

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object Notifications {
    fun showExportError(project: Project) {
        val notification = Notification(
            "ModuleMaker",
            "Error",
            "An error occurred while exporting your settings",
            NotificationType.ERROR
        )

        notification.notify(project)
    }
}
