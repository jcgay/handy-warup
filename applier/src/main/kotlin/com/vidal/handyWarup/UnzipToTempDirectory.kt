package com.vidal.handyWarup

import com.vidal.handyWarup.errors.UpdateUnzipException

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Function
import java.util.zip.ZipFile

class UnzipToTempDirectory : Function<File, Path> {

    override fun apply(file: File): Path {
        return toTemp(file)
    }

    private fun toTemp(zip: File): Path {
        val extractDir = tempDirectory()

        try {
            ZipFile(zip).use { zipFile ->
                zipFile.stream().filter({ zE -> !zE.isDirectory() }).forEachOrdered({ zipEntry ->
                    try {
                        zipFile.getInputStream(zipEntry).use({ `is` ->
                            val target = extractDir.resolve(zipEntry.getName())
                            val file = target.toFile()
                            makeFileTree(file.getParentFile())
                            Files.copy(`is`, target)
                        })
                    } catch (e: IOException) {
                        throw UpdateUnzipException(e)
                    }
                })
            }
        } catch (e: IOException) {
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
