package com.vidal.handyWarup

import com.vidal.handyWarup.errors.TemporaryCopyException
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class FsDeepCopy : (Path, Path) -> Unit {

    override fun invoke(source: Path, target: Path) {
        try {
            Files.walkFileTree(source, DeepCopyVisitor(source, target))
        } catch (e: IOException) {
            throw TemporaryCopyException("Unable to deep copy $source to $target", e)
        }
    }

    private class DeepCopyVisitor(private val source: Path, private val target: Path) : FileVisitor<Path> {

        @Throws(IOException::class)
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            copy(dir, resolve(dir))
            return FileVisitResult.CONTINUE
        }

        @Throws(IOException::class)
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            copy(file, resolve(file))
            return FileVisitResult.CONTINUE
        }

        @Throws(IOException::class)
        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
            throw exc
        }

        @Throws(IOException::class)
        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            return FileVisitResult.CONTINUE
        }

        private fun resolve(path: Path): Path {
            return target.resolve(source.relativize(path))
        }

        @Throws(IOException::class)
        private fun copy(dir: Path, to: Path) {
            Files.copy(dir, to, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
