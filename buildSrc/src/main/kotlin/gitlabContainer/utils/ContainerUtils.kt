package gitlabContainer.utils

import org.apache.commons.lang3.StringUtils
import org.taruts.processUtils.ProcessRunner

object ContainerUtils {
    fun containerExists(): Boolean {
        // println("containerExists")
        val containerId: String = ProcessRunner.runProcess(
            null,
            "docker", "container", "ls",
            "-q",
            "-a",
            "-f", "name=gitlab.taruts.org"
        )
        // println("containerId=$containerId")
        return StringUtils.isNotBlank(containerId)
    }
}
