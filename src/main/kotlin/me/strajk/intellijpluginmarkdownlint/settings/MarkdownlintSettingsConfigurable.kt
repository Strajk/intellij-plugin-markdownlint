package me.strajk.intellijpluginmarkdownlint.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel

/* TODO: Check subtypes */
class MarkdownlintSettingsConfigurable(
    private val project: Project
) : BoundSearchableConfigurable(
    displayName = "Markdownlint",
    helpTopic = "Markdownlint",
    _id = "me.strajk.intellijpluginmarkdownlint.settings.MarkdownlintSettingsConfigurable",
) {
    val settings = project.service<MarkdownlintSettings>()
    private var settingsComponent: MarkdownlintSettingsComponent = MarkdownlintSettingsComponent(settings)

    // TODO: Consider inlining MarkdownlintSettingsComponent here
    override fun createPanel(): DialogPanel {
        return settingsComponent.createPanel()
    }

    override fun getDisplayName(): String {
        return "Markdownlint Settings"
    }

    override fun apply() {
        super.apply()
        // FIXME: This is not working, it does not actually re-run checks for open files
        println("Restarting DaemonCodeAnalyzer")
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

}
