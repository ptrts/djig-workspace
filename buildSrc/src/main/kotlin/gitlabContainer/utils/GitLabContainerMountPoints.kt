package gitlabContainer.utils

import org.apache.commons.io.FileUtils
import org.gradle.api.Project

object GitLabContainerMountPoints {

    const val CONFIG = "/etc/gitlab"
    const val LOGS = "/var/log/gitlab"
    const val DATA = "/var/opt/gitlab"

    fun addBindMounts(command: MutableList<String>, project: Project) {
        command.addAll(
            getBindMounts(project)
        )
    }

    fun getBindMounts(project: Project): List<String> {
        val home: String = FileUtils.getFile(project.projectDir, "gitlab-container-home").canonicalPath
        return listOf(
            "--volume", "$home/config:$CONFIG",
            "--volume", "$home/logs:$LOGS",
            "--volume", "$home/data:$DATA",
        )
    }
}
