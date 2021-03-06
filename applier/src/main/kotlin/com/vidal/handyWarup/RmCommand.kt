package com.vidal.handyWarup

import java.nio.file.Path

class RmCommand(private val relative: Path) : Command {
    override fun invoke(sourceRoot: Path, targetRoot: Path) {
        FsDeepRemove().accept(targetRoot.resolve(relative))
    }
}
