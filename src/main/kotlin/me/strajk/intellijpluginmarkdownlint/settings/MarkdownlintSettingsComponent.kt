package me.strajk.intellijpluginmarkdownlint.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox

@Suppress("UnstableApiUsage")
class MarkdownlintSettingsComponent(
    private val settings: MarkdownlintSettings,
) {
    @Suppress("UnstableApiUsage")
    fun createPanel(): DialogPanel = panel {
        lateinit var isPluginEnabledCheckbox: JCheckBox
        row {
            isPluginEnabledCheckbox = checkBox("Enable Markdownlint")
                .bindSelected(settings::pluginEnabled)
                .component
        }

        configGroup(isPluginEnabledCheckbox.selected)
    }

    private fun Panel.configGroup(enabled: ComponentPredicate) = group(
        title = "Configuration",
        indent = false,
    ) {
        row {
            textFieldWithBrowseButton(
                "Configuration file",
                null,
                // TODO: Nice signature, sth like kwargs?
                FileChooserDescriptor(
                    /* chooseFiles = */ true,
                    /* chooseFolders = */ false,
                    /* chooseJars = */ false,
                    /* chooseJarsAsFiles = */ false,
                    /* chooseJarContents = */ false,
                    /* chooseMultiple = */ false
                ),
            )
                .bindText(settings::customConfigPath)
                .horizontalAlign(HorizontalAlign.FILL)
        }
    }.enabledIf(enabled)

}
