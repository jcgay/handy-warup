package com.vidal.handyWarup

import java.nio.file.Path
import java.util.function.BiConsumer

interface Command : BiConsumer<Path, Path>
