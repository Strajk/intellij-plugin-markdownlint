package me.strajk.intellijpluginmarkdownlint

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Ignore

/*
* TODO:
*  Figure out how to launch the testing environment with markdown plugin installed
*  `Plugin 'markdownlint' requires plugin 'org.intellij.plugins.markdown' to be installed`
* */

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
@Ignore /* skipped for now, see TODO above */
class MyPluginTest : BasePlatformTestCase() {

    fun testFile() {
        val psiFileMarkdown = myFixture.configureByText(
            "foo.md", """
            # Title
            
            - list 1
            + list 2
            * list 3
            
            ## Subtitle
            
            > quote
            
            [link](https://www.google.com)
        """.trimIndent()
        )

        // test LocalInspectionTool
        myFixture.enableInspections(MarkdownLintInspection::class.java)
        myFixture.testHighlighting(true, false, false)
        val errors: MutableList<HighlightInfo> = myFixture.doHighlighting()
        println("!!!!! errors: $errors")
        assertEquals(3, errors.size)

        // assertTrue(PsiErrorElementUtil.hasErrors(project, psiFileMarkdown.virtualFile))
    }

    override fun getTestDataPath() = "src/test/testData/FIXME"
}
