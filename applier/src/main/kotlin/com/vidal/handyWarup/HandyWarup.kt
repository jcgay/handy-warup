package com.vidal.handyWarup

import com.vidal.handyWarup.errors.CommandParsingException
import com.vidal.handyWarup.errors.HandyWarupException
import com.vidal.handyWarup.errors.NoUpdateDescriptorException
import com.vidal.handyWarup.errors.TargetDirectoryPermissionException
import com.vidal.handyWarup.errors.TemporaryCopyException

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.AbstractMap.SimpleEntry
import java.util.Arrays
import java.util.Date
import java.util.HashMap
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipFile

import java.nio.file.Files.createTempDirectory

class HandyWarup : BiFunction<File, File, File> {

    private val commandFactory: MutableMap<Pattern, Function<Matcher, Command>>
    private val deepCopy: FsDeepCopy
    private val deepRemove: FsDeepRemove

    init {
        deepCopy = FsDeepCopy()
        deepRemove = FsDeepRemove()
        commandFactory = HashMap<Pattern, Function<Matcher, Command>>()
        commandFactory.put(
                Pattern.compile("(?:add|replace) --from=/?(.*) --to=/?(.*)"),
                { matcher -> AddCommand(Paths.get(matcher.group(1)), Paths.get(matcher.group(2))) })
        commandFactory.put(
                Pattern.compile("rm --from=/?(.*)"),
                { matcher -> RmCommand(Paths.get(matcher.group(1))) })
    }

    /**
     * Tells whether a file is a valid Handy Warup update package.

     * @param file file to test
     * *
     * @return `true` if the file is valid, `false` otherwise
     */
    fun accepts(file: File): Boolean {
        try {
            ZipFile(file).use { zipFile -> return zipFile.getEntry("batch.warup") != null }
        } catch (e: IOException) {
            return false
        }

    }

    /**
     * Applies the specified update to the specified directory.

     * While not mandatory, calling [.accepts] is strongly recommended
     * in order to make sure `zippedDiff` is a valid update archive.

     * @param zippedDiff update archive to extract and apply
     * *
     * @param targetDirectory archive apply target
     * *
     * @return [File] instance that points to the modified specified installation path
     * *
     * @throws HandyWarupException if a problem occurs at any step
     */
    override fun apply(zippedDiff: File, targetDirectory: File): File {
        return applyPatch(zippedDiff, targetDirectory)
    }

    private fun applyPatch(zippedDiff: File, targetDirectory: File): File {
        assertTarget(targetDirectory)

        val targetPath = targetDirectory.toPath()
        val appliedDirectory = copyTarget(targetPath)
        val unzipped = UnzipToTempDirectory().apply(zippedDiff)

        val batchFile = Arrays.asList(*unzipped.toFile().listFiles()).stream().filter({ file -> file.getName() == "batch.warup" }).findFirst().orElseThrow({ NoUpdateDescriptorException("could not find patch file") })

        try {
            BufferedReader(FileReader(batchFile)).use { reader -> reader.lines().map(Function<String, Command> { this.parseCommandLine(it) }).forEach({ command -> command.accept(unzipped, appliedDirectory) }) }
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
            deepCopy.accept(targetDirectory, tempDirectory)
            return tempDirectory
        } catch (e: IOException) {
            throw TemporaryCopyException(e.message, e)
        }

    }

    private fun parseCommandLine(line: String): Command {
        return commandFactory.entries.stream().map({ entry ->
            val matcher = entry.key.matcher(line)
            SimpleEntry<Boolean, Supplier<Command>>(
                    matcher.matches(), // matches is called here, so subsequent group() calls will work
                    { entry.value.apply(matcher) })
        }).filter(Predicate<SimpleEntry<Boolean, Supplier<Command>>> { it.getKey() }).map({ entry -> entry.value.get() }).findFirst().orElseThrow({ CommandParsingException("Line could not be parsed: " + line) })
    }

    private fun move(appliedDirectory: Path, targetDirectory: Path): File {
        deepRemove.accept(targetDirectory)
        deepCopy.accept(appliedDirectory, targetDirectory)
        deepRemove.accept(appliedDirectory)
        return targetDirectory.toFile()
    }

    companion object {

        @JvmStatic fun main(args: Array<String>) {
            if (args.size != 2) {
                throw IllegalArgumentException(
                        "Expecting diff and target paths as arguments")
            }
            HandyWarup().apply(File(args[0]), File(args[1]))
        }
    }
}
