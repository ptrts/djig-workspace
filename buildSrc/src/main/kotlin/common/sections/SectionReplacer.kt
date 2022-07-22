package common.sections

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
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.COPY_ATTRIBUTES
        )
    }

    override fun onLine(line: String, isOurSectionStart: Boolean, isOurSectionEnd: Boolean) {
        if (isOurSectionEnd) {
            newContentsStream.forEach(printWriter::println)

            // И эту саму строчку и следующую уже будем записывать в новую версию файла
            copyLines = true
        }

        if (copyLines) {
            printWriter.println(line)
        }

        if (isOurSectionStart) {
            // Следующую строчку уже не будем записывать в новую версию файла
            copyLines = false
        }
    }
}
