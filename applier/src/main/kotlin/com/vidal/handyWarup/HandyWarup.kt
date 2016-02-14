package com.vidal.handyWarup

import com.vidal.handyWarup.errors.*
import java.io.*
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.AbstractMap.SimpleEntry
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipFile

class HandyWarup : (File, File) -> File {

    private val commandFactory: MutableMap<Pattern, (Matcher) -> Command>
    private val deepCopy: FsDeepCopy
    private val deepRemove: FsDeepRemove

    init {
        deepCopy = FsDeepCopy()
        deepRemove = FsDeepRemove()
        commandFactory = HashMap<Pattern, (Matcher) -> Command>()
        commandFactory.put(
                Pattern.compile("(?:add|replace) --from=/?(.*) --to=/?(.*)"),
                { matcher -> AddCommand(Paths.get(matcher.group(1)), Paths.get(matcher.group(2))) })
        commandFactory.put(
                Pattern.compile("rm --from=/?(.*)"),
                { matcher -> RmCommand(Paths.get(matcher.group(1))) })
    }

    /**
     * Applies the specified update to the specified directory.

     * While not mandatory, calling [.accepts] is strongly recommended
     * in order to make sure `zippedDiff` is a valid update archive.

     * @param zippedDiff update archive to extract and apply
     * @param targetDirectory archive apply target
     *
     * @return [File] instance that points to the modified specified installation path
     *
     * @throws HandyWarupException if a problem occurs at any step
     */
    override fun invoke(zippedDiff: File, targetDirectory: File): File {
        return applyPatch(zippedDiff, targetDirectory)
    }

    /**
     * Tells whether a file is a valid Handy Warup update package.

     * @param file file to test
     * *
     * @return `true` if the file is valid, `false` otherwise
     */
    fun accepts(file: File): Boolean {
        try {
            return ZipFile(file).use { zipFile: ZipFile -> zipFile.getEntry("batch.warup") } != null
        } catch(e: IOException) {
            return false
        }
    }

    private fun applyPatch(zippedDiff: File, targetDirectory: File): File {
        assertTarget(targetDirectory)

        val targetPath = targetDirectory.toPath()
        val appliedDirectory = copyTarget(targetPath)
        val unzipped = UnzipToTempDirectory()(zippedDiff)

        val batchFile = unzipped.toFile().listFiles()
                .filter { it.name == "batch.warup" }
                .firstOrNull() ?: throw NoUpdateDescriptorException("could not find patch file")

        try {
            BufferedReader(FileReader(batchFile)).use { reader -> reader.lines()
                    .map { parseCommandLine(it) }
                    .forEach { it(unzipped, appliedDirectory) }
            }
        } catch (e: FileNotFoundException) {
            throw NoUpdateDescriptorException(e)
        } catch (e: IOException) {
            throw HandyWarupException(e)
        }

        return move(appliedDirectory, targetPath)
    }

    private fun assertTarget(targetDirectory: File) {
        if (!targetDirectory.exists()) {
            throw TargetDirectoryPermissionException("could not find target to apply to")
        }
        if (!targetDirectory.canWrite()) {
            throw TargetDirectoryPermissionException("target must be writable")
        }
    }

    private fun copyTarget(targetDirectory: Path): Path {
        try {
            val tempDirectory = createTempDirectory("handy-warup-" + Date().time)
            deepCopy(targetDirectory, tempDirectory)
            return tempDirectory
        } catch (e: IOException) {
            throw TemporaryCopyException(e.message, e)
        }

    }

    private fun parseCommandLine(line: String): Command {
        return commandFactory
                .map {
                    val matcher = it.key.matcher(line)
                    SimpleEntry(matcher.matches(), { it.value(matcher) })
                }
                .filter { it.key == true }
                .map { it.value() }
                .firstOrNull() ?: throw CommandParsingException("Line could not be parsed: " + line)
    }

    private fun move(appliedDirectory: Path, targetDirectory: Path): File {
        deepRemove.accept(targetDirectory)
        deepCopy(appliedDirectory, targetDirectory)
        deepRemove.accept(appliedDirectory)
        return targetDirectory.toFile()
    }

    companion object {

        @JvmStatic fun main(args: Array<String>) {
            if (args.size != 2) {
                throw IllegalArgumentException(
                        "Expecting diff and target paths as arguments")
            }
            HandyWarup()(File(args[0]), File(args[1]))
        }
    }
}
