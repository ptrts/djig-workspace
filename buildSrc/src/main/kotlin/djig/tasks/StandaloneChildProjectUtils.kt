package djig.tasks

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import java.io.File

// todo Что за "standalone child project"? Объяснить это понятие здесь в KDoc
object StandaloneChildProjectUtils {

    fun getResourceFile(
        parentProject: Project,
        childProjectRelativePath: String,
        resourceRelativePath: String
    ): File {
        return FileUtils.getFile(
            parentProject.projectDir,
            "${childProjectRelativePath}/src/main/resources/${resourceRelativePath}"
        )
    }
}
