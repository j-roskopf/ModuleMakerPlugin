package com.joetr.modulemaker.persistence

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil.copyBean
import com.joetr.modulemaker.DEFAULT_BASE_PACKAGE_NAME
import org.jetbrains.annotations.Nullable

@State(name = "PreferenceService", storages = [(Storage("module_maker_preferences.xml"))])
class PreferenceServiceImpl : PersistentStateComponent<PreferenceServiceImpl.Companion.State>, PreferenceService {

    private var state = State()

    override var preferenceState: State
        get() = this.state
        set(value) {
            this.state = value
        }

    @Nullable
    override fun getState(): State {
        return this.preferenceState
    }

    override fun loadState(from: State) {
        copyBean(from, this.preferenceState)
    }

    companion object {

        data class State(
            var androidTemplate: String = "",
            var kotlinTemplate: String = "",
            var apiTemplate: String = "",
            var glueTemplate: String = "",
            var implTemplate: String = "",
            var packageName: String = DEFAULT_BASE_PACKAGE_NAME
        )

        @JvmStatic
        val instance: PreferenceServiceImpl
            get() = ApplicationManager.getApplication().getService(PreferenceServiceImpl::class.java)
    }
}
