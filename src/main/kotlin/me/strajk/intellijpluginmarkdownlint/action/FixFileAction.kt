package me.strajk.intellijpluginmarkdownlint.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import me.strajk.intellijpluginmarkdownlint.*
import me.strajk.intellijpluginmarkdownlint.settings.MarkdownlintSettingsConfigurable

class FixFileAction: AnAction() {
    private val logger = logger<FixFileAction>()

    override fun update(ev: AnActionEvent) {
        val project = ev.getData(CommonDataKeys.PROJECT) ?: return
        val file: VirtualFile = ev.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!file.fileSystem.isReadOnly && file.extension in MARKDOWN_FILE_EXTENSIONS) { // TODO: Maybe take from current language than from file extension?
            val pluginEnabled = MarkdownlintSettingsConfigurable(project).settings.pluginEnabled
            ev.presentation.isEnabledAndVisible = pluginEnabled
        } else {
            ev.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(ev: AnActionEvent) {
        // TODO: Handle virtual file, handle read-only file system
        val project = ev.getData(CommonDataKeys.PROJECT) ?: return
        val psiFile = ev.getData(CommonDataKeys.PSI_FILE) as PsiFile

        runCatching { runAction(project, psiFile) }
            .onFailure { logger.error("Unexpected error while performing FixFileAction", it) }
            .getOrThrow()

    }

    private fun runAction(project: Project, psiFile: PsiFile) {
        val document = psiFile.viewProvider.document ?: return

        val fixedContent = execMarkdownlintFixViaNode(
            psiFile,
            MarkdownlintSettingsConfigurable(project).settings.customConfigPath,
        )

        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(fixedContent)
        }
    }
}
