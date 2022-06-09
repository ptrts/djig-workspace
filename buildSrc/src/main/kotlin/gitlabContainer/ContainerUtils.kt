package gitlabContainer

import org.apache.commons.lang3.StringUtils
import org.taruts.processUtils.ProcessRunner

class ContainerUtils {
    companion object {
        fun containerExists(): Boolean {
            // println("containerExists")
            val containerId: String = ProcessRunner.runProcess(
                null,
                "docker", "container", "ls",
                "-q",
                "-a",
                "-f", "name=gitlab.taruts.net"
            )
            // println("containerId=$containerId")
            return StringUtils.isNotBlank(containerId)
        }
    }
}
