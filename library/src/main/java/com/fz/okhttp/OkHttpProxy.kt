package com.fz.okhttp

import android.content.Context
import com.fz.okhttp.params.OkRequestParams
import com.fz.okhttp.utils.Util
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.*

/**
 * OkHttp请求代理
 *
 * @author dingpeihua
 * @version 1.0
 * @date 2020/6/30 20:17
 */
class OkHttpProxy : IOkHttpProxy {
    private var mHttpClient: OkHttpClient?

    private constructor() : this(OkHttpClient())
    private constructor(mHttpClient: OkHttpClient) {
        this.mHttpClient = mHttpClient
    }

    constructor(context: Context) {
        mHttpClient = OkHttpWrapper.newBuilder(context.applicationContext).build()
    }

    private var httpClient: OkHttpClient
        get() = if (mHttpClient == null) OkHttpClient().also { mHttpClient = it } else mHttpClient!!
        set(okHttpClient) {
            mHttpClient = okHttpClient
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

    override fun <T> syncGet(url: String, params: Map<String, Any?>?, headers: HashMap<String, String>?, callback: OkCallback<T>?): Response {
        val builder = Request.Builder()
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl != null) {
            val httpBuilder = httpUrl.newBuilder()
            Util.buildGetParams(httpBuilder, params)
            builder.url(httpBuilder.build())
        }
        return syncRequest(builder.get(), headers, callback)
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

    override fun <T> syncPost(url: String, json: String, headers: HashMap<String, String>?, contentType: String?, callback: OkCallback<T>?): Response {
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

    override fun <T> syncPut(url: String, params: Map<String, Any?>, headers: HashMap<String, String>?, callback: OkCallback<T>?): Response {
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
        private var okHttpProxy: OkHttpProxy? = null
        private fun init(context: Context): OkHttpProxy? {
            if (okHttpProxy == null) {
                synchronized(OkHttpProxy::class.java) {
                    if (okHttpProxy == null) {
                        okHttpProxy = OkHttpProxy(context)
                    }
                }
            }
            return okHttpProxy
        }

        private fun init(): OkHttpProxy? {
            if (okHttpProxy == null) {
                synchronized(OkHttpProxy::class.java) {
                    if (okHttpProxy == null) {
                        okHttpProxy = OkHttpProxy()
                    }
                }
            }
            return okHttpProxy
        }

        val instance: OkHttpProxy?
            get() = if (okHttpProxy == null) init() else okHttpProxy

        fun getInstance(context: Context): OkHttpProxy? {
            return if (okHttpProxy == null) init(context) else okHttpProxy
        }
    }
}