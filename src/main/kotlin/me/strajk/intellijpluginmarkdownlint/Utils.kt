package me.strajk.intellijpluginmarkdownlint

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.json.JSONArray

/**
 * Executes markdownlint via node
 * Inspired by vscode-markdownlint logic
 */
fun execMarkdownlintViaNode(
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

    // TODO: Maybe use safeCliExec?
    val output = try {
        val processBuilder = ProcessBuilder("node", "-e", jsCode)
        val process = processBuilder.start()
        process.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        if (e.message?.contains("Cannot run program \"node\"") == true) {
            "[]" // TODO: Maybe directly returning empty list would be better?
        } else {
            throw e
        }
    }
    val jsonArray = try {
        JSONArray(output)
    } catch (e: Exception) {
        if (e.message?.contains("text must start with '[' at 0") == true) {
            JSONArray("[]")
        } else {
            throw e
        }
    }
    return parseJsonOutputToDataClasses(jsonArray)
}

fun parseJsonOutputToDataClasses(jsonArray: JSONArray): List<MarkdownLintError> {
    val list = mutableListOf<MarkdownLintError>()
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val lineNumber = jsonObject.getInt("lineNumber")
        val ruleDescription = jsonObject.getString("ruleDescription")
        @Suppress("UNCHECKED_CAST")
        val ruleNames = jsonObject.getJSONArray("ruleNames").toList() as List<String>

        val error = MarkdownLintError(
            lineNumber = lineNumber,
            ruleDescription = ruleDescription,
            ruleNames = ruleNames
        )
        list.add(error)
    }
    return list
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
     val ruleNames: List<String?>? // e.g. [ "MD004", "ul-style" ]
)

//data class FixInfo(
//    val insertText: String,
//    val lineNumber: Int
//)

// Unused - just for reference
fun execMarkdownlintViaCli(file: PsiFile): List<MarkdownLintError> {
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
fun execMarkdownlintCliViaCli(
    file: PsiFile,
    customConfigPath: String = defaultCustomConfigPath
): List<MarkdownLintError>? {

    val commandLine = GeneralCommandLine()
        .withExePath("markdownlint-cli2-config")
        .withWorkDirectory(file.virtualFile.parent.path)

    commandLine.addParameter(customConfigPath) // FIXME
    return null
}

fun getDocumentFromFile(psiFile: PsiFile): Document? {
    return psiFile.viewProvider.document // taken from Snyk plugin
    // return PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile) // taken from official plugins
}

fun getMainPsi(psiFile: PsiFile): PsiFile {
    val baseLanguage = psiFile.viewProvider.baseLanguage
    return psiFile.viewProvider.getPsi(baseLanguage)
}

fun safeCliExec(
    vararg commands: String,
): String {
    return try {
        ProcessBuilder(*commands)
            .start()
            .inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        if (e.message?.contains("Cannot run program") == true) {
            ""
        } else {
            throw e
        }
    }
}
