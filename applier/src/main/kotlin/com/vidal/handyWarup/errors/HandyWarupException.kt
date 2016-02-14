package com.vidal.handyWarup.errors

open class HandyWarupException : RuntimeException {

    constructor(message: String?, cause: Exception) : super(message, cause) {
    }

    constructor(message: String?) : super(message) {
    }

    constructor(cause: Exception) : super(cause) {
    }
}
