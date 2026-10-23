package com.unirem.spikea

import com.unirem.spikea.logic.ResultRow
import com.unirem.spikea.logic.ResultsFormat
import java.io.File
import java.io.FileOutputStream

/** Append-only JSONL in app-private storage. Redaction happens before write. */
class Results(private val file: File) {

    @Synchronized
    fun append(row: ResultRow) {
        file.parentFile?.mkdirs()
        FileOutputStream(file, true).use { out ->
            out.write((ResultsFormat.line(row) + "\n").toByteArray(Charsets.UTF_8))
            out.flush()
        }
    }

    fun path(): String = file.absolutePath

    fun lineCount(): Int = if (file.exists()) {
        file.useLines { lines -> lines.count { it.isNotBlank() } }
    } else {
        0
    }

    fun readLines(): List<String> = if (file.exists()) file.readLines() else emptyList()
}
