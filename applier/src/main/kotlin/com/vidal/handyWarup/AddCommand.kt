package com.vidal.handyWarup

import java.nio.file.Path

class AddCommand(private val relativeSource: Path, private val relativeTarget: Path) : Command {

    override fun accept(sourceRoot: Path, targetRoot: Path) {
        val source = sourceRoot.resolve(relativeSource)
        val target = targetRoot.resolve(relativeTarget)
        FsDeepCopy().accept(source, target)
    }
}
