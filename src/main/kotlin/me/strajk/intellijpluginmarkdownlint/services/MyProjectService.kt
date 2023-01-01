package me.strajk.intellijpluginmarkdownlint.services

import me.strajk.intellijpluginmarkdownlint.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }

    // TODO: Cleanup
    fun getRandomNumber() = 4
}
