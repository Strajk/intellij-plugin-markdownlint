package me.strajk.intellijpluginmarkdownlint
// TODO: Proper logging

import me.strajk.intellijpluginmarkdownlint.settings.MarkdownlintSettingsConfigurable
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.json.JSONArray

const val defaultCustomConfigPath = "/Users/strajk/Projects/setup/.markdownlint-cli2.yaml"

// TODO: Check UnfairLocalInspectionTool
@Suppress("unused")
class MarkdownLintInspection : LocalInspectionTool() {
    private val logger = logger<MarkdownLintInspection>() // TIP: See "Run configuration" for the log file location

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean, /*  true if inspection was run in non-batch mode - not sure what to do with that info */
    ): Array<ProblemDescriptor>? {
        val project = file.project
        val document = getDocumentFromFile(file)
        val isMainFile = file == getMainPsi(file)
        val pluginEnabled = MarkdownlintSettingsConfigurable(project).settings.pluginEnabled
        val isMdExtension = file.fileType.defaultExtension == "md"

        // "Return early" if not relevant
        if (!pluginEnabled) return null
        if (!isMainFile) return null // Inspired by editor-config plugin
        if (!isMdExtension) return null
        if (!file.isWritable) return null
        // TODO: Check isEnabled
        // TODO: Check isIgnored
        // TODO: Check charsetData

        val lintErrors = doWithMarkdownlintViaNode(
            file,
            MarkdownlintSettingsConfigurable(project).settings.customConfigPath,
        )

        val documentLineCount = document?.getLineCount() ?: 0

        val problemDescriptors = lintErrors.map { error ->
            val lineNumberAbs = (error.lineNumber!! - 1)
                .coerceAtLeast(0)
                // TODO: This should ideally not be needed,
                //  but I was getting `java.lang.IndexOutOfBoundsException: Wrong line: 5. Available lines count: 0`
                //  when rapidly deleting the contents of a file
                //  (inspired by https://github.com/andrepdo/findbugs-idea/commit/8f431b776400749d0c70ea9efa19c1f5cda303a7)
                // NOTE: This still crashes some times :/
                .coerceAtMost(documentLineCount)

            val start = document?.getLineStartOffset(lineNumberAbs) ?: 0
            val end = document?.getLineEndOffset(lineNumberAbs) ?: 0
            val range = TextRange(start, end)
            logger.info("Lint error: #$lineNumberAbs: ${error.ruleDescription}")
            val description = "Markdownlint: ${error.ruleDescription!!}" // NOTE: not sure if including "markdownlint" in description is the best practice
            manager.createProblemDescriptor(
                file,
                range,
                description,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, // NOTE: not sure with this type
                isOnTheFly
            )
        }

        return problemDescriptors.toTypedArray()
    }


    /**
     * Inspired by vscode-markdownlint logic
     */
    private fun doWithMarkdownlintViaNode(
        file: PsiFile,
        customConfigPath: String = defaultCustomConfigPath
    ): List<MarkdownLintError> {

        // This is pure madness, but it's how "official" vscode-markdownlint does it
        val jsCode = """
            const root = require('child_process').execSync('npm root -g').toString().trim()
            const lib = require(root + '/markdownlint-cli2')
    
            const modeAsName = "markdownlint-cli2-config"; // 1st argv is config path 
            const name = "test.md";
            const argv = [
                "$customConfigPath",
                "${file.virtualFile.path}"
            ];
    
            let results = [];
            const parameters = {
              name: modeAsName,  
              // fs,
              // directory,
              argv,
              // ["fileContents"]: {
              //   [name]: document.getText()
              // },
              // "noErrors": true,
              // "noGlobs": true,
              // "noRequire": true, // getNoRequire(scheme)
              // "optionsDefault": {}, // await getOptionsDefault(fs, configuration, config)
              "optionsOverride": {
                "fix": false,
                "outputFormatters": [ [ (options) => results = options.results ] ]
              }
            };
    
            ;(async () => {
              await lib.main(parameters).then(() => results);
              console.log(results);
            })();
        """.trimIndent()

        val processBuilder = ProcessBuilder("node", "-e", jsCode)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val jsonArray = try {
            JSONArray(output)
        } catch (e: Exception) {
            if (e.message?.contains("text must start with '[' at 0") == true) {
                logger.warn("[doWithMarkdownlintViaNode] 💥 ERROR not JSON output: $output")
                JSONArray("[]")
            } else {
                throw e
            }
        }
        return parseJsonOutputToDataClasses(jsonArray)
    }

    // Unused - just for reference
    private fun doWithMarkdownlint(file: PsiFile): List<MarkdownLintError> {
        val commandLine = GeneralCommandLine()
            .withExePath("markdownlint")
            .withWorkDirectory(file.virtualFile.parent.path)

        commandLine.addParameter("--json")
        commandLine.addParameter(file.virtualFile.path)

        println("[🤖 CMD]")
        println(commandLine.commandLineString)

        // Run commandLine, wait for finish and print output
        val process = commandLine.createProcess()

        val errorsAsJsonString = process.errorStream.bufferedReader().readText()
        println("[😱 errors]")
        println(errorsAsJsonString)

        val jsonArray = JSONArray(errorsAsJsonString)
        return parseJsonOutputToDataClasses(jsonArray)
    }

    // Unused - just for reference
    private fun doWithMarkdownlintCli(
        file: PsiFile,
        customConfigPath: String = defaultCustomConfigPath
    ): List<MarkdownLintError>? {

        val commandLine = GeneralCommandLine()
            .withExePath("markdownlint-cli2-config")
            .withWorkDirectory(file.virtualFile.parent.path)

        commandLine.addParameter(customConfigPath) // FIXME
        return null
    }

    private fun getDocumentFromFile(file: PsiFile) = PsiDocumentManager.getInstance(file.project).getDocument(file)

    private fun getMainPsi(psiFile: PsiFile): PsiFile {
        val baseLanguage = psiFile.viewProvider.baseLanguage
        return psiFile.viewProvider.getPsi(baseLanguage)
    }

    private fun parseJsonOutputToDataClasses(jsonArray: JSONArray): List<MarkdownLintError> {
        val list = mutableListOf<MarkdownLintError>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val lineNumber = jsonObject.getInt("lineNumber")
            val ruleDescription = jsonObject.getString("ruleDescription")
            val error = MarkdownLintError(
                lineNumber = lineNumber,
                ruleDescription = ruleDescription,
            )
            list.add(error)
        }
        return list
    }

}

data class MarkdownLintError(
    val lineNumber: Int?, // e.g. 5
    val ruleDescription: String?, // e.g. `Unordered list style`
    // val errorContext: String?, // e.g. - `item 1`
    // val errorDetail: Any?, // e.g. `Expected: dash; Actual: asterisk`
    // val errorRange: Any?, // e.g. [1, 5]
    // val fileName: String?, // e.g. `/Users/.../.../test.md`
    // val fixInfo: FixInfo?, // e.g. { "editColumn": 1, "deleteCount": 1, "insertText": "-" }
    // val ruleInformation: String?, // e.g. https://github.com/DavidAnson/markdownlint/blob/v0.26.2/doc/Rules.md#md004
    // val ruleNames: List<String?>? // e.g. [ "MD004", "ul-style" ]
)

//data class FixInfo(
//    val insertText: String,
//    val lineNumber: Int
//)
