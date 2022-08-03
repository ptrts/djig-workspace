package sections

import java.io.File
import java.util.*
import java.util.regex.Pattern

abstract class FileWithSectionsWalker(protected val file: File) {

    // Section start patter
    // ## section <name> {
    private val sectionStartPattern = Pattern.compile("""^\s*##\s*section\s+(?<sectionName>[\p{Alnum}\-._]+)\s*\{\s*$""")

    // Section end pattern
    // ## }
    private val sectionEndPattern = Pattern.compile("""^\s*##\s*\}\s*$""")

    open fun walk() {
        Scanner(file).use(this::walkInternal)
    }

    private fun walkInternal(scanner: Scanner) {
        val sectionNamesStack = LinkedList<String>()
        var line: String
        var lineNumber = 0
        while (scanner.hasNextLine()) {
            line = scanner.nextLine()
            lineNumber++

            // Process section starts
            val newSectionName = tryParseSectionStart(line)
            if (newSectionName != null) {
                sectionNamesStack.push(newSectionName)
            }

            // Processs section endings
            val closedSectionName = tryParseSectionEnding(line, sectionNamesStack, file, lineNumber)

            onLine(line, newSectionName, closedSectionName)
        }
    }

    private fun tryParseSectionStart(line: String): String? {
        val sectionStartMatcher = sectionStartPattern.matcher(line)
        if (sectionStartMatcher.find()) {
            return sectionStartMatcher.group("sectionName")
        } else {
            return null
        }
    }

    private fun tryParseSectionEnding(
        line: String,
        sections: LinkedList<String>,
        file: File,
        lineNumber: Int
    ): String? {
        val sectionEndMatcher = sectionEndPattern.matcher(line)
        if (sectionEndMatcher.find()) {
            if (sections.isEmpty()) {
                throw RuntimeException(
                    "${file.canonicalPath}:${lineNumber}: " +
                            "end of section comment (${line}) " +
                            "without a corresponding start of section " +
                            "(##section <name> {) " +
                            "earlier in the file"
                )
            }
            return sections.pop()
        } else {
            return null
        }
    }

    protected abstract fun onLine(line: String, newSectionName: String?, closedSectionName: String?)
}