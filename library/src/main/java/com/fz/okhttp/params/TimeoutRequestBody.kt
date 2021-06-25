package com.fz.okhttp.params

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ByteString
import java.io.File
import java.io.IOException

/**
 * 请求体处理超时时间
 *
 * @author dingpeihua
 * @version 1.0
 * @date 2019/8/30 17:02
 */
class TimeoutRequestBody(private val other: RequestBody) : RequestBody() {
    private var connectTimeout: Long = 0
    private var readTimeout: Long = 0
    private var writeTimeout: Long = 0
    var headers: Map<String, String>? = null

    fun connectTimeoutMillis(timeMillis: Long): TimeoutRequestBody {
        connectTimeout = timeMillis
        return this
    }

    fun readTimeoutMillis(timeMillis: Long): TimeoutRequestBody {
        readTimeout = timeMillis
        return this
    }

    fun writeTimeoutMillis(timeMillis: Long): TimeoutRequestBody {
        writeTimeout = timeMillis
        return this
    }

    fun getConnectTimeout(defaultTimeout: Long): Long {
        return if (connectTimeout > 0) connectTimeout else defaultTimeout
    }

    fun setHeaders(headers: Map<String, String>?): TimeoutRequestBody {
        this.headers = headers
        return this
    }

    fun getReadTimeout(defaultTimeout: Long): Long {
        return if (readTimeout > 0) readTimeout else defaultTimeout
    }

    fun getWriteTimeout(defaultTimeout: Long): Long {
        return if (writeTimeout > 0) writeTimeout else defaultTimeout
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return other.contentLength()
    }

    override fun contentType(): MediaType? {
        return other.contentType()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        other.writeTo(sink)
    }

    fun copyTimeout(body: TimeoutRequestBody?): TimeoutRequestBody {
        if (body == null) {
            return this
        }
        writeTimeoutMillis(body.writeTimeout)
        connectTimeoutMillis(body.connectTimeout)
        readTimeoutMillis(body.readTimeout)
        return this
    }

    companion object {
        /**
         * @see RequestBody.create
         */
        @JvmStatic
        fun toRequestBody(contentType: MediaType?, content: String): TimeoutRequestBody {
            return TimeoutRequestBody(content.toRequestBody(contentType))
        }

        /**
         * @see RequestBody.create
         */
        @JvmStatic
        fun toRequestBody(contentType: MediaType?, content: ByteString): TimeoutRequestBody {
            return TimeoutRequestBody(content.toRequestBody(contentType))
        }
        /**
         * @see RequestBody.create
         */
        /**
         * @see RequestBody.create
         */
        @JvmOverloads
        @JvmStatic
        fun toRequestBody(
                contentType: MediaType?, content: ByteArray,
                offset: Int = 0, byteCount: Int = content.size,
        ): TimeoutRequestBody {
            return TimeoutRequestBody(content.toRequestBody(contentType, offset, byteCount))
        }

        /**
         * @see RequestBody.create
         */
        @JvmStatic
        fun toRequestBody(contentType: MediaType?, file: File): TimeoutRequestBody {
            return TimeoutRequestBody(file.asRequestBody(contentType))
        }
    }
}