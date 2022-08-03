package sections

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream

object PropertiesFileSectionUtils {

    fun replaceSectionContents(file: File, sectionName: String, newContentsStream: Stream<String>) {
        SectionReplacer(file, sectionName, newContentsStream).walk()
    }
}