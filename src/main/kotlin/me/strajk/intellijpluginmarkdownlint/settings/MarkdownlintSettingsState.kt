package me.strajk.intellijpluginmarkdownlint.settings

import com.intellij.openapi.components.*

@Service(Service.Level.PROJECT)
@State(
    name = "intellijpluginmarkdownlintSettings", // Maybe just Markdownlint
    storages = [Storage("intellijpluginmarkdownlintSettings.xml", roamingType = RoamingType.DISABLED)], // Maybe more specific name
    category = SettingsCategory.TOOLS,
)
class MarkdownlintSettings : SimplePersistentStateComponent<MarkdownlintSettings.State>(State()) {
    // TODO: DRY: Seems weird to re-specify all properties from state here
    var pluginEnabled: Boolean
        get() = state.pluginEnabled
        set(value) {
            state.pluginEnabled = value
        }

    var customConfigPath: String
        get() = state.customConfigPath!!
        set(value) {
            state.customConfigPath = value
        }

    class State : BaseState() {
        var pluginEnabled by property(false)
        var customConfigPath by string("/Users/strajk/Projects/setup/.markdownlint-cli2.yaml")
        // var ignorePatterns by stringSet() // TODO
    }
}

