package me.strajk.intellijpluginmarkdownlint.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox

val nonFoundMsg = "🚫 Not found".replace(" ", "\u00a0") // otherwise it's gonna break to more lines

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
        checksGroup(isPluginEnabledCheckbox.selected)
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

    private fun Panel.checksGroup(enabled: ComponentPredicate) = group(
        title = "Checks",
        indent = false,
    ) {
        // TODO: "Not found" to presentation logic and style differently
        row {
            comment("Node installed at:").customize(Gaps(right = 5))
            comment(checkNodeInstalled(), maxLineLength = MAX_LINE_LENGTH_NO_WRAP).bold()
        }
        row {
            comment("Node version:").customize(Gaps(right = 5))
            comment(checkNodeVersion(), maxLineLength = MAX_LINE_LENGTH_NO_WRAP).bold()
        }
        row {
            comment("Global node modules path:").customize(Gaps(right = 5))
            comment(checkGlobalNodeModulesPath(), maxLineLength = MAX_LINE_LENGTH_NO_WRAP).bold()
        }
        row {
            comment("markdownlint-cli2 installed:").customize(Gaps(right = 5))
            comment(checkMarkdownlintInstalled(), maxLineLength = MAX_LINE_LENGTH_NO_WRAP).bold()
        }
    }.enabledIf(enabled).horizontalAlign(HorizontalAlign.FILL)

    private fun checkNodeInstalled(): String {
        return ProcessBuilder("which", "node")
            .start()
            .inputStream.bufferedReader().readText()
            .ifEmpty { nonFoundMsg }
    }

    private fun checkNodeVersion(): String {
        return ProcessBuilder("node", "--version")
            .start()
            .inputStream.bufferedReader().readText()
            .ifEmpty { nonFoundMsg }
    }

    private fun checkGlobalNodeModulesPath(): String {
        val jsCode = """
            const root = require('child_process').execSync('npm root -g').toString().trim()
            console.log(root)
        """.trimIndent()
        return ProcessBuilder("node", "-e", jsCode)
            .start()
            .inputStream.bufferedReader().readText()
            .ifEmpty { nonFoundMsg }
    }

    private fun checkMarkdownlintInstalled(): String {
        val jsCode = """
            const root = require('child_process').execSync('npm root -g').toString().trim()
            const lib = require(root + '/markdownlint-cli2')
            console.log(typeof lib === 'object' ? 'Installed' : '')
        """.trimIndent()
        return ProcessBuilder("node", "-e", jsCode)
            .start()
            .inputStream.bufferedReader().readText()
            .ifEmpty { nonFoundMsg }
    }
}
