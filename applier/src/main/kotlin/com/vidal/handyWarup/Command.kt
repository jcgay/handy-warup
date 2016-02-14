package com.vidal.handyWarup

import java.nio.file.Path

interface Command : (Path, Path) -> Unit
