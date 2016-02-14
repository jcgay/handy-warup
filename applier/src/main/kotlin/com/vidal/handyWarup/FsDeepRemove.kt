package com.vidal.handyWarup

import com.vidal.handyWarup.errors.PathDeletionException

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

class FsDeepRemove : Consumer<Path> {
    override fun accept(path: Path) {
        val filePath = path.toFile()
        if (filePath.isDirectory) {
            for (p in filePath.listFiles()!!) {
                accept(p.toPath())
            }
        }
        delete(path)
    }

    private fun delete(path: Path) {
        try {
            Files.delete(path)
        } catch (e: IOException) {
            throw PathDeletionException("Could not delete " + path, e)
        }

    }
}
