package com.fz.okhttp

import androidx.annotation.MainThread
import com.fz.okhttp.params.OkRequestParams
import com.fz.okhttp.utils.Util
import okhttp3.Call
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException

/**
 * OkHttp请求代理
 *
 * @author dingpeihua
 * @version 1.0
 * @date 2020/6/30 20:17
 */
class OkHttpProxy : IOkHttpProxy {
    private var mHttpClient: OkHttpClient

    private constructor() : this(OkHttpClient())
    constructor(mHttpClient: OkHttpClient) {
        this.mHttpClient = mHttpClient
    }

    private var httpClient: OkHttpClient
        get() = mHttpClient
        set(okHttpClient) {
            mHttpClient = okHttpClient
        }

    @MainThread
    fun setOkHttpClient(okHttpClient: OkHttpClient) {
        this.mHttpClient = okHttpClient
    }

    fun <T> post(url: String, json: String, contentType: String, callback: OkCallback<T>) {
        post(url, json, null, contentType, callback)
    }

    private fun <T> request(
        builder: Request.Builder,
        headers: Map<String, String>?, callback: OkCallback<T>?,
    ) {
        addHeaders(headers, builder)
        httpClient.newCall(builder.build()).enqueue(OkHttpCallback(callback))
    }

    private fun <T> syncRequest(
        builder: Request.Builder,
        headers: Map<String, String>?, callback: OkCallback<T>?,
    ): Response {
        addHeaders(headers, builder)
        try {
            val call = httpClient.newCall(builder.build())
            val response = call.execute()
            if (callback != null) {
                val httpCallback = OkHttpCallback(callback)
                httpCallback.onResponse(call, response)
            }
            return response
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Response.Builder()
            .code(400)
            .build()
    }

    private fun addHeaders(headers: Map<String, String>?, builder: Request.Builder) {
        if (headers != null && headers.isNotEmpty()) {
            builder.headers(headers.toHeaders())
        }
    }

    protected fun buildRequestBody(map: Map<String, Any?>): RequestBody {
        return buildRequestBody("application/json; charset=utf-8".toMediaType(), map)
    }

    protected fun buildRequestBody(mediaType: MediaType?, map: Map<String, Any?>): RequestBody {
        //补全请求地址
        val builder = MultipartBody.Builder()
        //设置类型
        builder.setType(MultipartBody.FORM)
        val keys = map.keys
        //追加参数
        for (key in keys) {
            val value = map[key]
            if (value is File) {
                builder.addFormDataPart(key, value.name, value.asRequestBody(mediaType))
            } else if (value != null) {
                builder.addFormDataPart(key, value.toString())
            }
        }
        return builder.build()
    }

    override fun <T> get(url: String, callback: OkCallback<T>?) {
        get(url, null, null, callback)
    }

    override fun <T> syncGet(url: String): Response {
        return syncGet<Any>(url, null)
    }

    override fun <T> syncGet(url: String, callback: OkCallback<T>?): Response {
        return syncGet(url, null, null, callback)
    }

    override fun <T> get(url: String, headers: HashMap<String, String>?, callback: OkCallback<T>?) {
        get(url, null, headers, callback)
    }

    override fun <T> get(url: String, params: OkRequestParams?, callback: OkCallback<T>?) {
        val builder = Request.Builder()
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl != null && params != null) {
            builder.url(params.buildQueryParameter(httpUrl))
        }
        request(builder.get(), params?.headers, callback)
    }

    override fun <T> syncGet(url: String, params: OkRequestParams?, callback: OkCallback<T>?): Response {
        val builder = Request.Builder()
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl != null && params != null) {
            builder.url(params.buildQueryParameter(httpUrl))
        }
        return syncRequest(builder.get(), params?.headers, callback)
    }

    override fun <T> get(
        url: String, params: Map<String, Any?>?, headers: HashMap<String, String>?,
        callback: OkCallback<T>?,
    ) {
        val builder = Request.Builder()
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl != null) {
            val httpBuilder: HttpUrl.Builder = httpUrl.newBuilder()
            Util.buildGetParams(httpBuilder, params)
            builder.url(httpBuilder.build())
        }
        request(builder.get(), headers, callback)
    }

    override fun <T> syncGet(
        url: String,
        params: Map<String, Any?>?,
        headers: HashMap<String, String>?,
        callback: OkCallback<T>?
    ): Response {
        val builder = Request.Builder()
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl != null) {
            val httpBuilder = httpUrl.newBuilder()
            Util.buildGetParams(httpBuilder, params)
            builder.url(httpBuilder.build())
        }
        return syncRequest(builder.get(), headers, callback)
    }

    override fun newCall(request: Request): Call {
        return httpClient.newCall(request)
    }

    override fun <T> post(url: String, params: OkRequestParams, callback: OkCallback<T>?) {
        val builder = Request.Builder()
        builder.url(url)
        builder.post(params.createRequestBody())
        request(builder, params.headers, callback)
    }

    override fun <T> syncPost(url: String, params: OkRequestParams): Response {
        return syncPost<Any>(url, params, null)
    }

    override fun <T> syncPost(url: String, params: OkRequestParams, callback: OkCallback<T>?): Response {
        val builder = Request.Builder()
        builder.url(url)
        builder.post(params.createRequestBody())
        return syncRequest(builder, params.headers, callback)
    }

    override fun <T> postFile(url: String, params: OkRequestParams, callback: OkCallback<T>?) {
        val builder = Request.Builder()
        builder.url(url)
        builder.post(params.createFileRequestBody())
        request(builder, params.headers, callback)
    }

    override fun <T> syncPostFile(url: String, params: OkRequestParams, callback: OkCallback<T>?): Response {
        val builder = Request.Builder()
        builder.url(url)
        builder.post(params.createFileRequestBody())
        return syncRequest(builder, params.headers, callback)
    }

    override fun <T> post(
        url: String, params: Map<String, Any?>, headers: HashMap<String, String>?,
        callback: OkCallback<T>?,
    ) {
        val builder = Request.Builder()
        builder.url(url)
        builder.post(buildRequestBody(params))
        request(builder, headers, callback)
    }

    override fun <T> post(
        url: String, json: String, headers: HashMap<String, String>?,
        contentType: String?, callback: OkCallback<T>?,
    ) {
        val builder = Request.Builder()
        builder.url(url)
        builder.post(json.toRequestBody(contentType?.toMediaType()))
        request(builder, headers, callback)
    }

    override fun <T> syncPost(
        url: String,
        json: String,
        headers: HashMap<String, String>?,
        contentType: String?,
        callback: OkCallback<T>?
    ): Response {
        val builder = Request.Builder()
        builder.url(url)
        builder.post(json.toRequestBody(contentType?.toMediaType()))
        return syncRequest(builder, headers, callback)
    }

    override fun <T> delete(url: String, params: OkRequestParams?, callback: OkCallback<T>?) {
        val builder = Request.Builder()
        builder.url(url)
        if (params != null) {
            builder.delete(params.createRequestBody())
        }
        request(builder, params?.headers, callback)
    }

    override fun <T> syncDelete(url: String, params: OkRequestParams?, callback: OkCallback<T>?): Response {
        val builder = Request.Builder()
        builder.url(url)
        if (params != null) builder.delete(params.createRequestBody())
        return syncRequest(builder, params?.headers, callback)
    }

    override fun <T> delete(
        url: String, json: String?, headers: HashMap<String, String>?,
        contentType: String?, callback: OkCallback<T>?,
    ) {
        val builder = Request.Builder()
        builder.url(url)
        if (!json.isNullOrEmpty()) {
            builder.delete(json.toRequestBody(contentType?.toMediaType()))
        }
        request(builder, headers, callback)
    }

    override fun <T> delete(
        url: String, params: Map<String, Any?>?, headers: HashMap<String, String>?,
        callback: OkCallback<T>?,
    ) {
        val builder = Request.Builder()
        builder.url(url)
        builder.delete(OkRequestParams(params).createRequestBody())
        request(builder, headers, callback)
    }

    override fun <T> syncDelete(
        url: String, params: Map<String, Any?>?,
        headers: HashMap<String, String>?, callback: OkCallback<T>?,
    ): Response {
        val builder = Request.Builder()
        builder.url(url)
        builder.delete(OkRequestParams(params).createRequestBody())
        return syncRequest(builder, headers, callback)
    }

    override fun <T> put(url: String, params: OkRequestParams, callback: OkCallback<T>?) {
        val builder = Request.Builder()
        builder.url(url)
        builder.put(params.createRequestBody())
        request(builder, params.headers, callback)
    }

    override fun <T> syncPut(url: String, params: OkRequestParams, callback: OkCallback<T>?): Response {
        val builder = Request.Builder()
        builder.url(url)
        builder.put(params.createRequestBody())
        return syncRequest(builder, params.headers, callback)
    }

    override fun <T> put(
        url: String, json: String, headers: HashMap<String, String>?,
        contentType: String?, callback: OkCallback<T>?,
    ) {
        val builder = Request.Builder()
        builder.url(url)
        builder.put(json.toRequestBody(contentType?.toMediaType()))
        request(builder, headers, callback)
    }

    override fun <T> put(
        url: String, params: Map<String, Any?>, headers: HashMap<String, String>?,
        callback: OkCallback<T>?,
    ) {
        val builder = Request.Builder()
        builder.url(url)
        builder.put(buildRequestBody(params))
        request(builder, headers, callback)
    }

    override fun <T> syncPut(
        url: String,
        params: Map<String, Any?>,
        headers: HashMap<String, String>?,
        callback: OkCallback<T>?
    ): Response {
        val builder = Request.Builder()
        builder.url(url)
        builder.put(buildRequestBody(params))
        return syncRequest(builder, headers, callback)
    }

    /**
     * 取消请求
     *
     * @param tag
     * @author dingpeihua
     * @date 2020/6/30 20:18
     * @version 1.0
     */
    fun cancel(tag: Any) {
        val dispatcher = httpClient.dispatcher
        for (call in dispatcher.queuedCalls()) {
            if (tag == call.request().tag()) {
                call.cancel()
            }
        }
        for (call in dispatcher.runningCalls()) {
            if (tag == call.request().tag()) {
                call.cancel()
            }
        }
    }

    companion object {
        private var okHttpProxy: OkHttpProxy = OkHttpProxy()

        @JvmStatic
        val instance: OkHttpProxy
            get() = okHttpProxy

        @JvmStatic
        fun getInstance(okhttpClient: OkHttpClient): OkHttpProxy {
            okHttpProxy.setOkHttpClient(okhttpClient)
            return okHttpProxy
        }
    }
}

fun <T> String.postData(
    url: String,
    mediaType: MediaType = MediaTypeUtil.JSON_MEDIA_TYPE,
    headers: Headers? = null, callback: OkHttpCallback<T>
) {
    val builder = Request.Builder()
        .url(url)
        .post(this.toRequestBody(mediaType))
    if (headers != null && headers.size > 0) {
        builder.headers(headers)
    }
    val request: Request = builder.build() //创建Request 对象
    OkHttpProxy.instance.newCall(request).enqueue(callback)
}

fun String.postData(
    url: String,
    mediaType: MediaType = MediaTypeUtil.JSON_MEDIA_TYPE,
    headers: Headers? = null
): Response {
    val builder = Request.Builder()
        .url(url)
    if (headers != null && headers.size > 0) {
        builder.headers(headers)
    }

    val request: Request = builder.post(this.toRequestBody(mediaType)).build() //创建Request 对象
    return OkHttpProxy.instance.newCall(request).execute()
}


fun <T> HttpUrl.getData(headers: Headers? = null, callback: OkHttpCallback<T>) {
    val builder = Request.Builder()
    builder.url(this)
    if (headers != null && headers.size > 0) {
        builder.headers(headers)
    }
    val request: Request = builder.get().build() //创建Request 对象
    OkHttpProxy.instance.newCall(request).enqueue(callback)
}

fun HttpUrl.getData(headers: Headers? = null): Response {
    val builder = Request.Builder()
    builder.url(this)
    if (headers != null && headers.size > 0) {
        builder.headers(headers)
    }
    val request: Request = builder.get().build() //创建Request 对象
    return OkHttpProxy.instance.newCall(request).execute()
}

fun <T> String.getData(
    params: Map<String, String>? = null,
    headers: Headers? = null,
    callback: OkHttpCallback<T>
) {
    val httpUrl = toHttpUrl(params)
    if (httpUrl != null) {
        httpUrl.getData(headers, callback)
    } else {
        val builder = Request.Builder().url(this)
        if (headers != null && headers.size > 0) {
            builder.headers(headers)
        }
        val request: Request = builder.get().build() //创建Request 对象
        OkHttpProxy.instance.newCall(request).enqueue(callback)
    }
}

fun String.getData(params: Map<String, String>? = null, headers: Headers? = null): Response {
    val httpUrl = toHttpUrl(params)
    return if (httpUrl != null) {
        httpUrl.getData(headers)
    } else {
        val builder = Request.Builder()
        builder.url(this)
        if (headers != null && headers.size > 0) {
            builder.headers(headers)
        }
        val request: Request = builder.get().build() //创建Request 对象
        OkHttpProxy.instance.newCall(request).execute()
    }
}

private fun String.toHttpUrl(params: Map<String, String>?): HttpUrl? {
    val httpUrlBuilder = this.toHttpUrlOrNull()?.newBuilder()
    if (httpUrlBuilder != null) {
        if (!params.isNullOrEmpty()) {
            for (item in params) {
                httpUrlBuilder.addEncodedQueryParameter(item.key, item.value)
            }
        }
    }
    return httpUrlBuilder?.build()
}