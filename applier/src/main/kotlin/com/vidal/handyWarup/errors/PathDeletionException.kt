package com.vidal.handyWarup.errors

import java.io.IOException

class PathDeletionException(message: String, cause: IOException) : HandyWarupException(message, cause)
