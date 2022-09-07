package com.fz.okhttp.params

import android.text.TextUtils
import com.fz.okhttp.interceptor.TimeoutInterceptor
import com.fz.okhttp.utils.Util
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author tp
 *
 *
 * A collection of string request parameters or files to send along with requests made from an
 * <pre>
 * RequestParams params = new RequestParams();
 * params.put("username", "james");
 * params.put("password", "123456");
 * params.put("email", "my&#064;email.com");
 * params.put("profile_picture", new File("pic.jpg")); // Upload a File
 * params.put("profile_picture2", someInputStream); // Upload an InputStream
 * params.put("profile_picture3", new ByteArrayInputStream(someBytes)); // Upload some bytes
 *
 * Map&lt;String, String&gt; map = new HashMap&lt;String, String&gt;();
 * map.put("first_name", "James");
 * map.put("last_name", "Smith");
 * params.put("user", map); // url params: "user[first_name]=James&amp;user[last_name]=Smith"
 *
 * Set&lt;String&gt; set = new HashSet&lt;String&gt;(); // unordered collection
 * set.add("music");
 * set.add("art");
 * params.put("like", set); // url params: "like=music&amp;like=art"
 *
 * List&lt;String&gt; list = new ArrayList&lt;String&gt;(); // Ordered collection
 * list.add("Java");
 * list.add("C");
 * params.put("languages", list); // url params: "languages[0]=Java&amp;languages[1]=C"
 *
</pre> *
 */
open class OkRequestParams() : Serializable {
    val urlParams = ConcurrentHashMap<String, Any?>()
    val fileParams = ConcurrentHashMap<String, FileWrapper?>()
    val headers = ConcurrentHashMap<String, String>()
    protected val mGson = Gson()
    private var jsonParams: String? = null
    private var isJsonParams = true

    /**
     * Adds a key/value string pair to the request.
     *
     * @param key   the key name for the new param.
     * @param value the value string for the new param.
     */
    fun addHeader(key: String?, value: String?): OkRequestParams {
        if (key != null && value != null) {
            headers[key] = value
        }
        return this
    }
    /**
     * Constructs a new RequestParams instance containing the key/value string params from the
     * specified map.
     *
     * @param source the source key/value string map to add.
     */
    /**
     * Constructs a new empty `RequestParams` instance.
     */
    constructor(source: Map<String, Any?>? = null) : this() {
        putAll(source)
    }

    /**
     * Constructs a new RequestParams instance and populate it with a single initial key/value
     * string param.
     *
     * @param key   the key name for the intial param.
     * @param value the value string for the initial param.
     */
    constructor(key: String?, value: Any?) : this(object : HashMap<String, Any?>() {
        init {
            if (key != null && value != null) {
                this[key] = value
            }
        }
    })

    /**
     * Constructs a new RequestParams instance and populate it with multiple initial key/value
     * string param.
     *
     * @param keysAndValues a sequence of keys and values. Objects are automatically converted to
     * Strings (including the value `null`).
     * @throws IllegalArgumentException if the number of arguments isn't even.
     */
    constructor(vararg keysAndValues: Any?) : this() {
        val len = keysAndValues.size
        require(len % 2 == 0) { "Supplied arguments must be even" }
        var i = 0
        while (i < len) {
            val key = keysAndValues[i].toString()
            val value = keysAndValues[i + 1].toString()
            put(key, value)
            i += 2
        }
    }

    private var connectTimeout: Long = 0
    private var readTimeout: Long = 0
    private var writeTimeout: Long = 0

    /**
     * 设置连接超时时间
     * 注意：使用该参数时，如果有自定义[RequestBody] ，需要使用[TimeoutRequestBody]，并调用
     * [TimeoutRequestBody.copyTimeout]
     *
     * @author dingpeihua
     * @date 2019/8/30 16:35
     * @version 1.0
     */
    fun connectTimeoutMillis(timeMillis: Long): OkRequestParams {
        connectTimeout = timeMillis
        return this
    }

    /**
     * 设置读取超时时间
     * 注意：使用该参数时，如果有自定义[RequestBody] ，需要使用[TimeoutRequestBody]，并调用
     * [TimeoutRequestBody.copyTimeout]
     *
     * @author dingpeihua
     * @date 2019/8/30 16:35
     * @version 1.0
     */
    fun readTimeoutMillis(timeMillis: Long): OkRequestParams {
        readTimeout = timeMillis
        return this
    }

    /**
     * 设置写超时时间
     * 注意：使用该参数时，如果有自定义[RequestBody] ，需要使用[TimeoutRequestBody]，并调用
     * [TimeoutRequestBody.copyTimeout]
     *
     * @author dingpeihua
     * @date 2019/8/30 16:35
     * @version 1.0
     */
    fun writeTimeoutMillis(timeMillis: Long): OkRequestParams {
        writeTimeout = timeMillis
        return this
    }

    /**
     * Adds a key/value string pair to the request.
     *
     * @param key   the key name for the new param.
     * @param value the value string for the new param.
     */
    fun put(key: String?, value: Any?): OkRequestParams {
        checkValue(value)
        if (key != null && value != null) {
            urlParams[key] = value
        }
        return this
    }

    open fun checkValue(value: Any?): Boolean {
        require(!(value is JSONArray || value is JSONObject)) { "Value can not be org.json.JSONArray or org.json.JSONObject" }
        return true
    }

    /**
     * Adds a file to the request.
     *
     * @param key  the key name for the new param.
     * @param file the file to add.
     * @throws FileNotFoundException throws if wrong File argument was passed
     */
    @Throws(FileNotFoundException::class)
    fun put(key: String?, file: File?): OkRequestParams {
        put(key, file, null, null)
        return this
    }

    /**
     * Copies all of the mappings from the specified map to this one.
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     */
    fun putAll(m: Map<String, Any?>?) {
        if (m != null && m.isNotEmpty()) {
            for ((key, value) in m) {
                put(key, value)
            }
        }
    }

    /**
     * Adds a file to the request with custom provided file name
     *
     * @param key            the key name for the new param.
     * @param file           the file to add.
     * @param customFileName file name to use instead of real file name
     * @throws FileNotFoundException throws if wrong File argument was passed
     */
    @Throws(FileNotFoundException::class)
    fun put(key: String?, customFileName: String?, file: File?): OkRequestParams {
        put(key, file, null, customFileName)
        return this
    }

    /**
     * Adds a file to the request with custom provided file content-type
     *
     * @param key         the key name for the new param.
     * @param file        the file to add.
     * @param contentType the content type of the file, eg. application/json
     * @throws FileNotFoundException throws if wrong File argument was passed
     */
    @Throws(FileNotFoundException::class)
    fun put(key: String?, file: File?, contentType: String?): OkRequestParams {
        put(key, file, contentType, null)
        return this
    }

    /**
     * Adds a file to the request with both custom provided file content-type and file name
     *
     * @param key            the key name for the new param.
     * @param file           the file to add.
     * @param contentType    the content type of the file, eg. application/json
     * @param customFileName file name to use instead of real file name
     * @throws FileNotFoundException throws if wrong File argument was passed
     */
    @Throws(FileNotFoundException::class)
    fun put(
        key: String?,
        file: File?,
        contentType: String?,
        customFileName: String?
    ): OkRequestParams {
        if (file == null || !file.exists()) {
            throw FileNotFoundException()
        }
        if (key != null) {
            fileParams[key] = FileWrapper(
                file, contentType ?: MEDIA_TYPE,
                customFileName ?: file.name
            )
        }
        return this
    }

    /**
     * Adds a int value to the request.
     *
     * @param key   the key name for the new param.
     * @param value the value int for the new param.
     */
    fun put(key: String?, value: Int): OkRequestParams {
        if (key != null) {
            urlParams[key] = value.toString()
        }
        return this
    }

    fun clearUrlParams(): OkRequestParams {
        urlParams.clear()
        return this
    }

    /**
     * Adds a long value to the request.
     *
     * @param key   the key name for the new param.
     * @param value the value long for the new param.
     */
    fun put(key: String?, value: Long): OkRequestParams {
        if (key != null) {
            urlParams[key] = value.toString()
        }
        return this
    }

    /**
     * Removes a parameter from the request.
     *
     * @param key the key name for the parameter to remove.
     */
    fun remove(key: String) {
        urlParams.remove(key)
        fileParams.remove(key)
    }

    /**
     * Check if a parameter is defined.
     *
     * @param key the key name for the parameter to check existence.
     * @return Boolean
     */
    fun has(key: String): Boolean {
        return urlParams[key] != null ||
                fileParams[key] != null
    }

    fun getJsonParams(): String? {
        return jsonParams
    }

    fun setJsonParams(jsonParams: String?): OkRequestParams {
        this.jsonParams = jsonParams
        return this
    }

    fun putJsonParams(params: String?): OkRequestParams {
        jsonParams = params
        return this
    }

    fun isJsonParams(): Boolean {
        return isJsonParams
    }

    fun setJsonParams(b: Boolean): OkRequestParams {
        isJsonParams = b
        return this
    }

    override fun toString(): String {
        val result = StringBuilder()
        append(result, "jsonParams", jsonParams)
        append(result, "isJsonParams", isJsonParams)
        append(result, "jsonParams", jsonParams)
        append(result, "connectTimeout", connectTimeout)
        append(result, "readTimeout", readTimeout)
        append(result, "writeTimeout", writeTimeout)
        for ((key, value) in urlParams) {
            append(result, key, value)
        }
        for ((key, file) in fileParams) {
            append(result, key, file?.absolutePath ?: "unknown")
        }
        for ((key, value) in headers) {
            append(result, key, value)
        }
        return result.toString()
    }

    private fun append(result: StringBuilder, key: Any, value: Any?) {
        if (result.isNotEmpty()) {
            result.append("&")
        }
        result.append(key)
        result.append("=")
        result.append(value)
    }

    class FileWrapper(
        val file: File,
        val contentType: String = "image/*",
        val customFileName: String = file.name
    ) : Serializable {
        val absolutePath: String
            get() = file.absolutePath
    }

    fun buildQueryParameter(url: HttpUrl): HttpUrl {
        val httpBuilder = url.newBuilder()
        Util.buildGetParams(httpBuilder, urlParams)
        return httpBuilder.build()
    }

    open fun createRequestBody(): RequestBody {
        return asRequestBody()
    }

    open fun createFileRequestBody(): MultipartBody {
        return asMultipartBody()
    }

    companion object {
        val MEDIA_TYPE = "application/json; charset=utf-8"

        @JvmStatic
        @JvmName("createRequestParams")
        fun OkRequestParams.asRequestParams(): String {
            val paramsMap: Map<String, Any?> = urlParams
            return if (isJsonParams) {
                if (TextUtils.isEmpty(jsonParams)) {
                    //json 参数
                    val mGson = mGson
                    return mGson.toJson(paramsMap)
                }
                jsonParams!!
            } else {
                val builder = StringBuilder()
                val it = paramsMap.keys.iterator()
                // add 参数
                while (it.hasNext()) {
                    val key = it.next()
                    val value = paramsMap[key]
                    if (builder.isNotEmpty()) {
                        builder.append("&")
                    }
                    builder.append(key)
                    builder.append("=")
                    if (value != null) {
                        builder.append(Util.toString(value))
                    }
                }
                builder.toString()
            }
        }

        @JvmStatic
        @JvmName("createRequestBody")
        fun OkRequestParams.asRequestBody(): RequestBody {
            val paramsMap: Map<String, Any?> = urlParams
            return if (isJsonParams) {
                val jsonParams: String = if (TextUtils.isEmpty(jsonParams)) {
                    //json 参数
                    val mGson = mGson
                    mGson.toJson(paramsMap)
                } else {
                    jsonParams!!
                }
                TimeoutRequestBody.toRequestBody(MEDIA_TYPE.toMediaType(), jsonParams)
                    .setHeaders(headers)
                    .connectTimeoutMillis(connectTimeout)
                    .readTimeoutMillis(readTimeout)
                    .writeTimeoutMillis(writeTimeout)
            } else {
                // Form表单
                val builder = FormBody.Builder()
                val it = paramsMap.keys.iterator()
                // add 参数
                while (it.hasNext()) {
                    val key = it.next()
                    val value = paramsMap[key]
                    if (value != null) {
                        builder.add(key, Util.toString(value))
                    }
                }
                builder.add(TimeoutInterceptor.CONNECT_TIMEOUT, connectTimeout.toString())
                    .add(TimeoutInterceptor.READ_TIMEOUT, readTimeout.toString())
                    .add(TimeoutInterceptor.WRITE_TIMEOUT, writeTimeout.toString())
                if (headers.size > 0) {
                    val headers: Map<String, String> = headers
                    val keys = headers.keys.iterator()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = headers[key]
                        if (value != null) {
                            builder.add(TimeoutInterceptor.HEAD_KEY + key, value)
                        }
                    }
                }
                builder.build()
            }
        }

        @JvmStatic
        @JvmName("createFileRequestBody")
        fun OkRequestParams.asMultipartBody(): MultipartBody {
            val builder = MultipartBody.Builder()
            for ((key, value) in urlParams.entries) {
                builder.addFormDataPart(key, Util.toString(value))
            }
            for ((key, value) in fileParams.entries) {
                val file = value?.file
                if (file != null) {
                    builder.addFormDataPart(
                        key, value.customFileName,
                        value.file.asRequestBody(
                            value.contentType.toMediaType(),
                        )
                    )
                }
            }
            builder.setType(MultipartBody.FORM)
            return builder.build()
        }

        @JvmStatic
        @JvmName("createFileRequestBody")
        fun Map<String, Any>.asMultipartBody(): MultipartBody {
            val builder = MultipartBody.Builder()
            builder.setType(MultipartBody.FORM)
            val keys: Set<String> = keys
            for (key in keys) {
                when (val value: Any? = get(key)) {
                    is File -> {
                        builder.addFormDataPart(
                            key,
                            value.name,
                            value.asRequestBody(MEDIA_TYPE.toMediaType())
                        )
                    }
                    else -> {
                        builder.addFormDataPart(key, value.toString())
                    }
                }
            }
            return builder.build()
        }
    }
}