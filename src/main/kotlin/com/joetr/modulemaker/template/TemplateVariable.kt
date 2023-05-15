package com.joetr.modulemaker.template

enum class TemplateVariable(val templateVariable: String) {
    PACKAGE_NAME(
        """
            "${'$'}{packageName}"
        """.trimIndent()
    )
}
