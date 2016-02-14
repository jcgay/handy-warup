package com.vidal.handyWarup.errors

import java.io.IOException

class UpdateUnzipException : HandyWarupException {

    constructor(cause: Exception) : super(cause) {
    }

    constructor(message: String?, cause: IOException) : super(message, cause) {
    }

    constructor(message: String?) : super(message) {
    }
}
