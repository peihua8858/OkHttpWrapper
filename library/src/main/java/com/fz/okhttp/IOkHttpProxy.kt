package com.fz.okhttp

import com.fz.okhttp.params.OkRequestParams
import okhttp3.Response
import java.util.*

/**
 * Http 请求代理
 *
 * @author dingpeihua
 * @version 1.0
 * @date 2020/7/1 15:20
 */
interface IOkHttpProxy {
    operator fun <T> get(url: String, callback: OkCallback<T>?)
    fun <T> syncGet(url: String): Response?
    fun <T> syncGet(url: String, callback: OkCallback<T>?): Response?
    operator fun <T> get(url: String, headers: HashMap<String, String>?, callback: OkCallback<T>?)
    operator fun <T> get(url: String, params: OkRequestParams?, callback: OkCallback<T>?)
    fun <T> syncGet(url: String, params: OkRequestParams?, callback: OkCallback<T>?): Response?
    operator fun <T> get(
            url: String, params: Map<String, Any?>?, headers: HashMap<String, String>?,
            callback: OkCallback<T>?
    )

    fun <T> syncGet(
            url: String, params: Map<String, Any?>?, headers: HashMap<String, String>?,
            callback: OkCallback<T>?
    ): Response?

    fun <T> post(url: String, params: OkRequestParams, callback: OkCallback<T>?)
    fun <T> syncPost(url: String, params: OkRequestParams): Response?
    fun <T> syncPost(url: String, params: OkRequestParams, callback: OkCallback<T>?): Response?
    fun <T> postFile(url: String, params: OkRequestParams, callback: OkCallback<T>?)
    fun <T> syncPostFile(url: String, params: OkRequestParams, callback: OkCallback<T>?): Response?

    //    <T> Response syncPostFile(String url, Map<String, Object> params, OkCallback<T> callback);
    fun <T> post(
            url: String, params: Map<String, Any?>, headers: HashMap<String, String>?,
            callback: OkCallback<T>?
    )

    fun <T> post(
            url: String, json: String, headers: HashMap<String, String>?, contentType: String?,
            callback: OkCallback<T>?
    )

    fun <T> syncPost(
            url: String, json: String, headers: HashMap<String, String>?, contentType: String?,
            callback: OkCallback<T>?
    ): Response?

    fun <T> delete(url: String, params: OkRequestParams?, callback: OkCallback<T>?)
    fun <T> syncDelete(url: String, params: OkRequestParams?, callback: OkCallback<T>?): Response?
    fun <T> delete(
            url: String, json: String?, headers: HashMap<String, String>?, contentType: String?,
            callback: OkCallback<T>?
    )

    fun <T> delete(
            url: String, params: Map<String, Any?>?, headers: HashMap<String, String>?,
            callback: OkCallback<T>?
    )

    fun <T> syncDelete(
            url: String, params: Map<String, Any?>?, headers: HashMap<String, String>?,
            callback: OkCallback<T>?
    ): Response?

    fun <T> put(url: String, params: OkRequestParams, callback: OkCallback<T>?)
    fun <T> syncPut(url: String, params: OkRequestParams, callback: OkCallback<T>?): Response?
    fun <T> put(
            url: String, json: String, headers: HashMap<String, String>?, contentType: String?,
            callback: OkCallback<T>?
    )

    fun <T> put(
            url: String, params: Map<String, Any?>, headers: HashMap<String, String>?,
            callback: OkCallback<T>?
    )

    fun <T> syncPut(
            url: String, params: Map<String, Any?>, headers: HashMap<String, String>?,
            callback: OkCallback<T>?
    ): Response?
}