package me.strajk.intellijpluginmarkdownlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import me.strajk.intellijpluginmarkdownlint.settings.MarkdownlintSettingsConfigurable

class MarkdownLintExternalAnnotator : ExternalAnnotator<PsiFile, List<MarkdownLintError>>() {
    private val logger = logger<MarkdownLintExternalAnnotator>()

    // collect some data about a file needed for launching a tool
    override fun collectInformation(file: PsiFile): PsiFile = file

    // execute a tool and collect highlighting data
    override fun doAnnotate(psiFile: PsiFile): List<MarkdownLintError> {
        val project = psiFile.project

        val pluginEnabled = MarkdownlintSettingsConfigurable(project).settings.pluginEnabled
        if (!pluginEnabled) return emptyList()

        // TODO: Maybe more checks? Like writeable, extension, main-file, etc.

        logger.info("🔧 Checking file: ${psiFile.name}")
        return execMarkdownlintViaNode(
            psiFile,
            MarkdownlintSettingsConfigurable(project).settings.customConfigPath,
        )
    }

    // apply highlighting data to a file
    override fun apply(
        psiFile: PsiFile,
        annotationResult: List<MarkdownLintError>,
        holder: AnnotationHolder
    ) {
        val document = getDocumentFromFile(psiFile)
        val documentLineCount = document?.getLineCount() ?: 0

        for (error in annotationResult) {
            // TODO: Still getting java.lang.IndexOutOfBoundsException: Wrong line: XX. Available lines count: XX
            try {
                val lineNumberAbs = (error.lineNumber!! - 1)
                    .coerceAtLeast(0)
                    .coerceAtMost(documentLineCount) // TODO: This seems not to be enough
                val start = document?.getLineStartOffset(lineNumberAbs) ?: 0
                val end = document?.getLineEndOffset(lineNumberAbs) ?: 0
                val range = TextRange(start, end)
                val description = buildString {
                    append("Markdownlint: ")
                    append(error.ruleDescription!!)
                    append(" (")
                    append(error.ruleNames!!.joinToString(", "))
                    append(")")
                } // NOTE: not sure if including "markdownlint" in description is the best practice
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    description
                ).range(range).create()
            } catch (e: Exception) {
                if (e is IndexOutOfBoundsException) {
                    logger.warn("IndexOutOfBoundsException - we check these in code, but still sometimes happen when document is out of sync")
                    logger.warn("${e.message}")
                } else {
                    logger.error("Error while applying annotation", e)
                }
            }
        }
    }

}
