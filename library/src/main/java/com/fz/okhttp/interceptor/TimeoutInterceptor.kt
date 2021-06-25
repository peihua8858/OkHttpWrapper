package com.fz.okhttp.interceptor

import com.fz.okhttp.params.TimeoutRequestBody
import com.socks.library.KLog
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 从header读取超时时间并设置
 *
 * @author dingpeihua
 * @version 1.0
 * @date 2019/6/14 14:22
 */
class TimeoutInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val requestBody = request.body
        var connectTimeout: Int = chain.connectTimeoutMillis()
        var readTimeout: Int = chain.readTimeoutMillis()
        var writeTimeout: Int = chain.writeTimeoutMillis()
        KLog.d("TimeoutInterceptor>>>requestBody:" + requestBody?.javaClass)
        when (requestBody) {
            is TimeoutRequestBody -> {
                connectTimeout = requestBody.getConnectTimeout(chain.connectTimeoutMillis().toLong()).toInt()
                readTimeout = requestBody.getReadTimeout(chain.readTimeoutMillis().toLong()).toInt()
                writeTimeout = requestBody.getWriteTimeout(chain.writeTimeoutMillis().toLong()).toInt()
                val builder: Request.Builder = addHeaders(request.newBuilder(), requestBody.headers)
                requestBody.headers = null
                request = builder.post(requestBody).build()
            }
            is FormBody -> {
                val size = requestBody.size
                val builder = FormBody.Builder()
                val headers: MutableMap<String, String> = HashMap()
                for (i in 0 until size) {
                    val name = requestBody.name(i)
                    when {
                        READ_TIMEOUT == name -> {
                            readTimeout = toInteger(requestBody.value(i), chain.readTimeoutMillis())
                        }
                        WRITE_TIMEOUT == name -> {
                            writeTimeout = toInteger(requestBody.value(i), chain.writeTimeoutMillis())
                        }
                        CONNECT_TIMEOUT == name -> {
                            connectTimeout = toInteger(requestBody.value(i), chain.connectTimeoutMillis())
                        }
                        name.startsWith(HEAD_KEY) -> {
                            headers[getHeaderKey(name)] = requestBody.value(i)
                        }
                        else -> {
                            builder.add(name, requestBody.value(i))
                        }
                    }
                }
                request = addHeaders(request.newBuilder(), headers)
                        .post(builder.build()).build()
            }
            else -> {
                val connectNew = request.header(CONNECT_TIMEOUT)
                val readNew = request.header(READ_TIMEOUT)
                val writeNew = request.header(WRITE_TIMEOUT)
                connectTimeout = toTimeout(connectNew, chain.connectTimeoutMillis())
                readTimeout = toTimeout(readNew, chain.readTimeoutMillis())
                writeTimeout = toTimeout(writeNew, chain.writeTimeoutMillis())
            }
        }
        KLog.d("""
    TimeoutInterceptor>>>url:${request.url}
    connectTimeout:$connectTimeout,readTimeout:$readTimeout,writeTimeout:$writeTimeout
    """.trimIndent())
        return chain
                .withConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .withReadTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .withWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .proceed(request)
    }

    private fun getHeaderKey(name: String): String {
        return name.substring(HEAD_KEY.length)
    }

    private fun toTimeout(value: String?, defaultValue: Int): Int {
        val result = toInteger(value, defaultValue)
        return if (result > 0) result else defaultValue
    }

    /**
     * 将Object对象转成Integer类型
     *
     * @param value
     * @return 如果value不能转成Integer，则默认defaultValue
     */
    private fun toInteger(value: String?, defaultValue: Int): Int {
        try {
            return value!!.toInt()
        } catch (ignored: Exception) {
        }
        return defaultValue
    }

    private fun addHeaders(builder: Request.Builder, headers: Map<String, String>?): Request.Builder {
        if (headers != null && headers.isNotEmpty()) {
            val keys = headers.keys.iterator()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = headers[key]
                KLog.d("TimeoutInterceptor>>>header:$key,$value")
                if (value != null) {
                    addHeader(builder, key, value)
                }
            }
        }
        return builder
    }

    private fun addHeader(builder: Request.Builder, name: String, value: String): Request.Builder {
        builder.addHeader(name, value)
        return builder
    }

    companion object {
        const val CONNECT_TIMEOUT = "connect_timeout"
        const val READ_TIMEOUT = "read_timeout"
        const val WRITE_TIMEOUT = "write_timeout"
        const val HEAD_KEY = "header_"
    }
}