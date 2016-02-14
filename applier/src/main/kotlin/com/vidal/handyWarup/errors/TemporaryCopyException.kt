package com.vidal.handyWarup.errors

import java.io.IOException

class TemporaryCopyException(message: String?, cause: IOException) : HandyWarupException(message, cause)
