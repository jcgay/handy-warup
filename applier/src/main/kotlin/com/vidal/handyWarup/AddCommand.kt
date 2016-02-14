package com.vidal.handyWarup

import java.nio.file.Path

class AddCommand(private val relativeSource: Path, private val relativeTarget: Path) : Command {

    override fun invoke(sourceRoot: Path, targetRoot: Path) {
        val source = sourceRoot.resolve(relativeSource)
        val target = targetRoot.resolve(relativeTarget)
        FsDeepCopy()(source, target)
    }
}
