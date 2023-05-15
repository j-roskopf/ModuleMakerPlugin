package com.joetr.modulemaker.template

import com.joetr.modulemaker.ANDROID
import com.joetr.modulemaker.FREEMARKER_VERSION
import com.joetr.modulemaker.KOTLIN
import com.joetr.modulemaker.file.ANDROID_KEY
import com.joetr.modulemaker.file.API_KEY
import com.joetr.modulemaker.file.GLUE_KEY
import com.joetr.modulemaker.file.IMPL_KEY
import com.joetr.modulemaker.file.KOTLIN_KEY
import com.joetr.modulemaker.persistence.PreferenceService
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.nio.file.Paths

class TemplateWriter(
    private val preferenceService: PreferenceService
) {

    private val cfg = Configuration(FREEMARKER_VERSION).apply {
        setClassLoaderForTemplateLoading(TemplateWriter::class.java.classLoader, "")
    }

    /**
     * Creates gradle file for the module from base gradle template file
     */
    fun createGradleFile(
        moduleFile: File,
        moduleName: String,
        moduleType: String,
        useKtsBuildFile: Boolean,
        defaultKey: String?,
        gradleFileFollowModule: Boolean,
        packageName: String
    ) {
        try {
            // Build the data-model
            val data: MutableMap<String, Any> = HashMap()

            // load gradle file from template folder
            val gradleTemplate: Template = when (moduleType) {
                KOTLIN -> {
                    val customPreferences = getPreferenceFromKey(defaultKey, KOTLIN_KEY)
                    if (customPreferences.isNotEmpty()) {
                        Template(
                            null,
                            customPreferences,
                            cfg
                        )
                    } else {
                        val template = if (useKtsBuildFile) {
                            KotlinModuleKtsTemplate.data
                        } else {
                            KotlinModuleTemplate.data
                        }
                        Template(
                            null,
                            template,
                            cfg
                        )
                    }
                }
                ANDROID -> {
                    val customPreferences = getPreferenceFromKey(defaultKey, ANDROID_KEY)
                    data["packageName"] = packageName
                    if (customPreferences.isNotEmpty()) {
                        Template(
                            null,
                            customPreferences,
                            cfg
                        )
                    } else {
                        val template = if (useKtsBuildFile) {
                            AndroidModuleKtsTemplate.data
                        } else {
                            AndroidModuleTemplate.data
                        }
                        Template(
                            null,
                            template,
                            cfg
                        )
                    }
                }
                else -> throw IllegalArgumentException("Unknown module type")
            }

            // File output
            val extension = if (useKtsBuildFile) {
                ".gradle.kts"
            } else {
                ".gradle"
            }
            val fileName = if (gradleFileFollowModule) {
                moduleName.plus(extension)
            } else {
                "build".plus(extension)
            }
            val file: Writer = FileWriter(Paths.get(moduleFile.absolutePath, fileName).toFile())
            gradleTemplate.process(data, file)
            file.flush()
            file.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: TemplateException) {
            e.printStackTrace()
        }
    }

    fun createReadmeFile(moduleFile: File, moduleName: String) {
        try {
            val manifestTemplate = Template(
                null,
                ModuleReadMeTemplate.data,
                cfg
            )

            val data: MutableMap<String, Any> = HashMap()

            data["moduleName"] = moduleName

            // create directory for the readme
            val manifestFile = Paths.get(moduleFile.absolutePath).toFile()
            manifestFile.mkdirs()

            // File output
            val file: Writer = FileWriter(Paths.get(manifestFile.absolutePath, "README.md").toFile())
            manifestTemplate.process(data, file)
            file.flush()
            file.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: TemplateException) {
            e.printStackTrace()
        }
    }

    private fun getPreferenceFromKey(key: String?, fallback: String): String {
        return when (key ?: fallback) {
            IMPL_KEY -> preferenceService.preferenceState.implTemplate
            API_KEY -> preferenceService.preferenceState.apiTemplate
            GLUE_KEY -> preferenceService.preferenceState.glueTemplate
            ANDROID_KEY -> preferenceService.preferenceState.androidTemplate
            KOTLIN_KEY -> preferenceService.preferenceState.kotlinTemplate
            else -> ""
        }
    }
}
