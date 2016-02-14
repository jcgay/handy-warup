package com.vidal.handyWarup

import com.vidal.handyWarup.errors.UpdateUnzipException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class UnzipToTempDirectory : (File) -> Path {

    override fun invoke(file: File): Path {
        return toTemp(file)
    }

    private fun toTemp(zip: File): Path {
        val extractDir = tempDirectory()

        try {
            ZipFile(zip).use { zipFile ->
                zipFile.entries().toList()
                    .filter { !it.isDirectory }
                    .forEach {
                        try {
                            zipFile.getInputStream(it).use { entryStream ->
                                val target = extractDir.resolve(it.name)
                                val file = target.toFile()
                                makeFileTree(file.parentFile)
                                Files.copy(entryStream, target)
                            }
                        } catch(e: IOException) {
                            throw UpdateUnzipException(e)
                        }
                    }
            }
        } catch(e: IOException) {
            throw UpdateUnzipException("could not find diff file", e)
        }

        return extractDir
    }

    private fun makeFileTree(file: File) {
        if (file.exists()) {
            return
        }
        if (!file.mkdirs()) {
            throw UpdateUnzipException("Could not create parent directories")
        }
    }

    private fun tempDirectory(): Path {
        try {
            return Files.createTempDirectory("handy-warup")
        } catch (e: IOException) {
            throw UpdateUnzipException(e.message, e)
        }

    }
}
