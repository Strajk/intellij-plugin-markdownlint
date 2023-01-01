package me.strajk.intellijpluginmarkdownlint
// TODO: Proper logging

import me.strajk.intellijpluginmarkdownlint.settings.MarkdownlintSettingsConfigurable
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.json.JSONArray

// TODO: Check UnfairLocalInspectionTool
@Suppress("unused")
class MarkdownLintInspection : LocalInspectionTool() {
    private val log: Logger = Logger.getInstance(MarkdownLintInspection::class.java)

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val project = file.project

        // "Return early" if not relevant
        if (!MarkdownlintSettingsConfigurable(project).settings.pluginEnabled) return null
        if (file != getMainPsi(file)) return null
        if (file.fileType.defaultExtension != "md") return null
        if (!file.isWritable) return null
        // TODO: Check isEnabled
        // TODO: Check isIgnored
        // TODO: Check charsetData

        val document = getDocumentFromFile(file)
        val lintErrors = doWithMarkdownlintViaNode(
            file,
            MarkdownlintSettingsConfigurable(project).settings.customConfigPath,
        )

        val problemDescriptors = lintErrors.map { error ->
            val lineNumberAbs = (error.lineNumber!! - 1).coerceAtLeast(0)
            val start = document?.getLineStartOffset(lineNumberAbs)
            val end = document?.getLineEndOffset(lineNumberAbs)
            val range = TextRange(
                start ?: 0,
                end ?: 0
            )
            manager.createProblemDescriptor(
                file,
                range,
                error.ruleDescription!!, // non-null assertion
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
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
        customConfigPath: String? = null
    ): List<MarkdownLintError> {
        if (customConfigPath != null) println("!!! Using custom config path: $customConfigPath")

        // This is pure madness, but it's how "official" vscode-markdownlint does it
        val jsCode = """
            const root = require('child_process').execSync('npm root -g').toString().trim()
            const lib = require(root + '/markdownlint-cli2')
    
            const modeAsName = "markdownlint-cli2-config"; // 1st argv is config path 
            const name = "test.md";
            const argv = [
                "${customConfigPath ?: "/Users/strajk/Projects/setup/.markdownlint-cli2.yaml"}",
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
                println("[doWithMarkdownlintViaNode] 💥 ERROR not JSON output: $output")
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
    private fun doWithMarkdownlintCli(file: PsiFile): List<MarkdownLintError>? {

        val commandLine = GeneralCommandLine()
            .withExePath("markdownlint-cli2-config")
            .withWorkDirectory(file.virtualFile.parent.path)

        commandLine.addParameter("/Users/strajk/Projects/setup/.markdownlint-cli2.yaml") // FIXME
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
    val lineNumber: Int?,
    val ruleDescription: String?,
    // TODO: Handle other fields?
    // val errorContext: String?,
    // val errorDetail: Any?,
    // val errorRange: Any?,
    // val fileName: String?,
    // val fixInfo: FixInfo?,
    // val ruleInformation: String?,
    // val ruleNames: List<String?>?
)

//data class FixInfo(
//    val insertText: String,
//    val lineNumber: Int
//)
