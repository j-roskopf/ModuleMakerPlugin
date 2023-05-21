package com.joetr.modulemaker

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.ProjectManager

object Notifications {
    fun showExportError() {
        val notification = Notification(
            "ModuleMaker",
            "Error",
            "An error occurred while exporting your settings",
            NotificationType.ERROR
        )

        notification.notify(ProjectManager.getInstance().openProjects[0])
    }
}
