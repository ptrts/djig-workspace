package common.sections

import java.io.File

abstract class FileWithParticularSectionWalker(file: File, private val sectionName: String) : FileWithSectionsWalker(file) {

    override final fun onLine(line: String, newSectionName: String?, closedSectionName: String?) {
        val isOurSectionStart = sectionName.equals(newSectionName)
        val isOurSectionEnd = sectionName.equals(closedSectionName)
        onLine(line, isOurSectionStart, isOurSectionEnd)
    }

    abstract fun onLine(line: String, isOurSectionStart: Boolean, isOurSectionEnd: Boolean)
}
