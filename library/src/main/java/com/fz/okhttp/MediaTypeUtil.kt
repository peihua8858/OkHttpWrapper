package com.fz.okhttp

import okhttp3.MediaType.Companion.toMediaType

object MediaTypeUtil {
    val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    val FORM_TYPE = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
}