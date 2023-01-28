package me.strajk.intellijpluginmarkdownlint.settings

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selected
import me.strajk.intellijpluginmarkdownlint.safeCliExec
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

    // TODO: Lower spacing between rows
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
            comment("markdownlint-cli2 installed at:").customize(Gaps(right = 5))
            comment(checkMarkdownlintInstalled(), maxLineLength = MAX_LINE_LENGTH_NO_WRAP).bold()
        }
        row {
            comment("Plugin version:").customize(Gaps(right = 5))
            comment(checkPluginMeta(), maxLineLength = MAX_LINE_LENGTH_NO_WRAP).bold()
        }
        row {
            comment("If you get some non passing checks, check FAQ section on GitHub README").bold()
        }
    }.enabledIf(enabled).horizontalAlign(HorizontalAlign.FILL)

    private fun checkNodeInstalled(): String {
        return safeCliExec("which", "node")
            .ifEmpty { nonFoundMsg }
    }

    private fun checkNodeVersion(): String {
        return safeCliExec("node", "--version")
            .ifEmpty { nonFoundMsg }
    }

    private fun checkGlobalNodeModulesPath(): String {
        val jsCode = """
            const root = require('child_process').execSync('npm root -g').toString().trim()
            console.log(root)
        """.trimIndent()
        return safeCliExec("node", "-e", jsCode)
            .ifEmpty { nonFoundMsg }
    }

    private fun checkMarkdownlintInstalled(): String {
        val jsCode = """
            const root = require('child_process').execSync('npm root -g').toString().trim()
            const wholePath = root + '/markdownlint-cli2'
            const lib = require(wholePath)
            console.log(typeof lib === 'object' ? wholePath : '')
        """.trimIndent()
        return safeCliExec("node", "-e", jsCode)
            .ifEmpty { nonFoundMsg }
    }

    private fun checkPluginMeta(): String {
        val ideaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("me.strajk.intellijpluginmarkdownlint"))
        return ideaPluginDescriptor?.version ?: "unknown"
    }
}
