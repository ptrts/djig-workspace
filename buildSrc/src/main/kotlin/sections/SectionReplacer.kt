package sections

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.stream.Stream

class SectionReplacer(
    file: File,
    sectionName: String,
    private val newContentsStream: Stream<String>
) : FileWithParticularSectionWalker(file, sectionName) {

    private val newVersionTempFile: File
    private val printWriter: PrintWriter

    private var copyLines = true

    init {
        newVersionTempFile = File.createTempFile(file.nameWithoutExtension, file.extension)
        printWriter = PrintWriter(newVersionTempFile)
    }

    override fun walk() {
        printWriter.use {
            super.walk()
        }

        Files.move(
            newVersionTempFile.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    override fun onLine(line: String, isOurSectionStart: Boolean, isOurSectionEnd: Boolean) {
        if (isOurSectionEnd) {
            // Put the inserted contents between the section bracket comments
            newContentsStream.forEach(printWriter::println)

            // We should copy to the new version of the file this line and all the following ones
            copyLines = true
        }

        if (copyLines) {
            printWriter.println(line)
        }

        if (isOurSectionStart) {
            // The next line won't go to the new version of the file
            copyLines = false
        }
    }
}
