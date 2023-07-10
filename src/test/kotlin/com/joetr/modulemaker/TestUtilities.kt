package com.joetr.modulemaker

import org.junit.rules.TemporaryFolder
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

fun TemporaryFolder.populateSettingsGradleKtsWithFakeData(): File {
    val settingsGradleKts = this.newFile(settingsGradleKts)
    val writer = FileWriter(settingsGradleKts)
    writer.write(NowInAndroidSettingsGradleKts.data)
    writer.close()
    return settingsGradleKts
}

fun TemporaryFolder.populateSettingsGradleKtsWithFakeFilePathData(): File {
    val settingsGradleKts = this.newFile(settingsGradleKts)
    val writer = FileWriter(settingsGradleKts)
    writer.write(NowInAndroidSettingsGradleKts.filePathData)
    writer.close()
    return settingsGradleKts
}

fun TemporaryFolder.populateSettingsGradleKtsWithFakeFilePathDataAndCustomInclude(): File {
    val settingsGradleKts = this.newFile(settingsGradleKts)
    val writer = FileWriter(settingsGradleKts)
    writer.write(NowInAndroidSettingsGradleKts.filePathDataWithCustomIncludeBuildData)
    writer.close()
    return settingsGradleKts
}

fun TemporaryFolder.populateSettingsGradleWithFakeData(): File {
    val settingsGradle = this.newFile(settingsGradle)
    val writer = FileWriter(settingsGradle)
    writer.write(DefaultTemplateSettingsGradle.data)
    writer.close()
    return settingsGradle
}

fun readFromFile(file: File): List<String> {
    val fileReader = FileReader(file)
    val bufferedReader = BufferedReader(fileReader)
    val data = mutableListOf<String>()
    try {
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            data.add(line.orEmpty())
        }
    } finally {
        fileReader.close()
        bufferedReader.close()
    }
    return data
}
