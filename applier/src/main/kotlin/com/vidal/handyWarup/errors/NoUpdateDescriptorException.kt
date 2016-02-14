package com.vidal.handyWarup.errors

import java.io.IOException

class NoUpdateDescriptorException : HandyWarupException {

    constructor(message: String) : super(message) {
    }

    constructor(cause: IOException) : super(cause) {
    }
}
