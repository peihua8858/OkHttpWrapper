package com.fz.okhttp

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.CookieCache
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.fz.okhttp.interceptor.TimeoutInterceptor
import okhttp3.*
import okhttp3.EventListener
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.Proxy
import java.net.ProxySelector
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.*

/**
 * @author dingpeihua
 * @version 1.0
 * @date 2019/11/22 15:11
 */
class OkHttpWrapper {
    internal class CookieSave(var isAddCookie: Boolean, var cookie: Cookie)
    internal class CookieMap(
        var isAddCookie: Boolean,
        hostOnly: Boolean,
        secure: Boolean,
        val mCookies: Array<out String>?
    ) {
        var hostOnly = false
        var secure = false
        fun hasCookies(): Boolean {
            return mCookies != null && mCookies.isNotEmpty()
        }

        constructor(isAddCookie: Boolean, cookies: Array<String>?) : this(
            isAddCookie,
            false,
            false,
            cookies
        ) {
        }

        init {
            this.hostOnly = hostOnly
            this.secure = secure
        }
    }

    var context: Context?
    private val builder: OkHttpClient.Builder
    private var mCookies: MutableList<CookieSave> = ArrayList()
    val networkInterceptors: MutableList<Interceptor> = ArrayList()
    private var interceptors: MutableList<Interceptor>? = ArrayList()
    private var certsData: List<ByteArray>? = null
    private var securityInterceptor: Interceptor? = null
    private var responseCacheInterceptor: Interceptor? = null
    private var cachePath: String? = null
    private var cookieMaps: MutableList<CookieMap>? = null
    private var maxRequests = 128
    private var maxRequestsPerHost = 64

    constructor(context: Context?, builder: OkHttpClient.Builder) {
        this.context = context
        this.builder = builder
    }

    internal constructor(other: OkHttpWrapper) {
        context = other.context
        builder = other.builder
        mCookies = other.mCookies
        interceptors = other.interceptors
        certsData = other.certsData
        securityInterceptor = other.securityInterceptor
        responseCacheInterceptor = other.responseCacheInterceptor
        cachePath = other.cachePath
        cookieMaps = other.cookieMaps
    }

    fun addCookie(isAddCookie: Boolean, cookie: Cookie?): OkHttpWrapper {
        if (cookie != null) {
            mCookies.add(CookieSave(isAddCookie, cookie))
        }
        return this
    }

    fun addCookie(cookie: Cookie?): OkHttpWrapper {
        return addCookie(true, cookie)
    }

    fun addCookie(vararg cookies: Cookie?): OkHttpWrapper {
        return addCookie(true, *cookies)
    }

    fun addCookie(isAddCookie: Boolean, vararg cookies: Cookie?): OkHttpWrapper {
        if (cookies.isNotEmpty()) {
            for (cookie in cookies) {
                addCookie(isAddCookie, cookie)
            }
        }
        return this
    }

    fun addCookie(cookies: List<Cookie?>?): OkHttpWrapper {
        return addCookie(true, cookies)
    }

    fun addCookie(isAddCookie: Boolean, cookies: List<Cookie?>?): OkHttpWrapper {
        if (cookies != null && cookies.isNotEmpty()) {
            for (cookie in cookies) {
                addCookie(isAddCookie, cookie)
            }
        }
        return this
    }

    /**
     * 设置cookie，默认secure为true，即默认必须是https
     * 如果非https 可使用[.addCookie]或者[.addCookie]
     *
     * @param isAddCookie true则添加cookie，否则不添加
     * @param cookies     长度必须是4的倍数：
     * cookies[i] :domain
     * cookies[i + 1] :path
     * cookies[i + 2] :name
     * cookies[i + 3]  :value
     * @author dingpeihua
     * @date 2020/1/3 11:15
     * @version 1.0
     */
    fun addCookie(isAddCookie: Boolean, vararg cookies: String): OkHttpWrapper {
        return addCookie(isAddCookie, false, *cookies)
    }

    /**
     * 设置cookie，默认secure为true，即默认必须是https
     * 如果非https 可使用[.addCookie]或者[.addCookie]
     *
     * @param isAddCookie true则添加cookie，否则不添加
     * @param hostOnly    true 只匹配host
     * @param cookies     长度必须是4的倍数：
     * cookies[i] :domain
     * cookies[i + 1] :path
     * cookies[i + 2] :name
     * cookies[i + 3]  :value
     * @author dingpeihua
     * @date 2020/1/3 11:15
     * @version 1.0
     */
    fun addCookie(isAddCookie: Boolean, hostOnly: Boolean, vararg cookies: String): OkHttpWrapper {
        return addCookie(isAddCookie, false, false, *cookies)
    }

    fun addCookie(
        isAddCookie: Boolean,
        hostOnly: Boolean,
        secure: Boolean,
        vararg cookies: String
    ): OkHttpWrapper {
        if (cookies != null && cookies.size % 4 == 0) {
            if (cookieMaps == null) {
                cookieMaps = ArrayList()
            }
            cookieMaps!!.add(CookieMap(isAddCookie, hostOnly, secure, cookies))
        }
        return this
    }

    fun setCertsData(certs_data: List<ByteArray>?): OkHttpWrapper {
        certsData = certs_data
        return this
    }

    fun addInterceptor(vararg interceptor: Interceptor?): OkHttpWrapper {
        if (interceptor != null) {
            interceptors!!.addAll(ArrayList(Arrays.asList(*interceptor)))
        }
        return this
    }

    fun addInterceptor(interceptor: Interceptor?): OkHttpWrapper {
        if (interceptor != null) {
            interceptors!!.add(interceptor)
        }
        return this
    }

    fun addNetworkInterceptor(interceptor: Interceptor?): OkHttpWrapper {
        if (interceptor != null) {
            networkInterceptors.add(interceptor)
        }
        return this
    }

    fun securityInterceptor(interceptor: Interceptor?): OkHttpWrapper {
        securityInterceptor = interceptor
        return this
    }

    fun responseCacheInterceptor(interceptor: Interceptor?): OkHttpWrapper {
        responseCacheInterceptor = interceptor
        return this
    }
    /**
     * 设置自定义超时时间拦截器
     * 如：[TimeoutInterceptor]
     *
     * @author dingpeihua
     * @date 2019/8/30 17:10
     * @version 1.0
     */
    /**
     * 设置自定义超时时间拦截器
     * [TimeoutInterceptor]
     *
     * @author dingpeihua
     * @date 2019/8/30 17:10
     * @version 1.0
     */
    @JvmOverloads
    fun timeoutInterceptor(interceptor: Interceptor = TimeoutInterceptor()): OkHttpWrapper {
        interceptors!!.add(interceptor)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.connectTimeout]
     */
    fun connectTimeout(timeout: Long, unit: TimeUnit): OkHttpWrapper {
        builder.connectTimeout(if (timeout > 0) timeout else defaultTimeout, unit)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.readTimeout]
     */
    fun readTimeout(timeout: Long, unit: TimeUnit): OkHttpWrapper {
        builder.readTimeout(if (timeout > 0) timeout else defaultTimeout, unit)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.writeTimeout]
     */
    fun writeTimeout(timeout: Long, unit: TimeUnit): OkHttpWrapper {
        builder.writeTimeout(if (timeout > 0) timeout else defaultTimeout, unit)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.pingInterval]
     */
    fun pingInterval(interval: Long, unit: TimeUnit): OkHttpWrapper {
        builder.pingInterval(interval, unit)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.proxy]
     */
    fun proxy(proxy: Proxy?): OkHttpWrapper {
        if (proxy != null) {
            builder.proxy(proxy)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.proxySelector]
     */
    fun proxySelector(proxySelector: ProxySelector?): OkHttpWrapper {
        if (proxySelector != null) {
            builder.proxySelector(proxySelector)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.cookieJar]
     */
    fun cookieJar(cookieJar: CookieJar?): OkHttpWrapper {
        if (cookieJar != null) {
            builder.cookieJar(cookieJar)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.cache]
     */
    fun cache(cache: Cache?): OkHttpWrapper {
        if (cache != null) {
            builder.cache(cache)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.dns]
     */
    fun dns(dns: Dns?): OkHttpWrapper {
        if (dns != null) {
            builder.dns(dns)
        }
        return this
    }

    /**
     * @see[okhttp3.OkHttpClient.Builder.socketFactory]
     */
    fun socketFactory(socketFactory: SocketFactory?): OkHttpWrapper {
        if (socketFactory != null) {
            builder.socketFactory(socketFactory)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.sslSocketFactory]
     */
    fun sslSocketFactory(
        sslSocketFactory: SSLSocketFactory?,
        trustManager: X509TrustManager?
    ): OkHttpWrapper {
        if (sslSocketFactory != null && trustManager != null) {
            builder.sslSocketFactory(sslSocketFactory, trustManager)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.hostnameVerifier]
     */
    fun hostnameVerifier(hostnameVerifier: HostnameVerifier?): OkHttpWrapper {
        if (hostnameVerifier != null) {
            builder.hostnameVerifier(hostnameVerifier)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.certificatePinner]
     */
    fun certificatePinner(certificatePinner: CertificatePinner?): OkHttpWrapper {
        if (certificatePinner != null) {
            builder.certificatePinner(certificatePinner)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.authenticator]
     */
    fun authenticator(authenticator: Authenticator?): OkHttpWrapper {
        if (authenticator != null) {
            builder.authenticator(authenticator)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.proxyAuthenticator]
     */
    fun proxyAuthenticator(proxyAuthenticator: Authenticator?): OkHttpWrapper {
        if (proxyAuthenticator != null) {
            builder.proxyAuthenticator(proxyAuthenticator)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.connectionPool]
     */
    fun connectionPool(connectionPool: ConnectionPool?): OkHttpWrapper {
        if (connectionPool != null) {
            builder.connectionPool(connectionPool)
        }
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.followRedirects]
     */
    fun followSslRedirects(followProtocolRedirects: Boolean): OkHttpWrapper {
        builder.followSslRedirects(followProtocolRedirects)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.followRedirects]
     */
    fun followRedirects(followRedirects: Boolean): OkHttpWrapper {
        builder.followRedirects(followRedirects)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.retryOnConnectionFailure]
     */
    fun retryOnConnectionFailure(retryOnConnectionFailure: Boolean): OkHttpWrapper {
        builder.retryOnConnectionFailure(retryOnConnectionFailure)
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.dispatcher]
     */
    fun dispatcher(dispatcher: Dispatcher?): OkHttpWrapper {
        if (dispatcher != null) {
            builder.dispatcher(dispatcher)
        }
        return this
    }

    /**
     * @see [Dispatcher.setMaxRequests]
     */
    fun setMaxRequests(maxRequests: Int): OkHttpWrapper {
        require(maxRequests >= 1) { "max < 1: $maxRequests" }
        this.maxRequests = maxRequests
        return this
    }

    /**
     * @see  [Dispatcher.setMaxRequestsPerHost]
     */
    fun setMaxRequestsPerHost(maxRequestsPerHost: Int): OkHttpWrapper {
        require(maxRequestsPerHost >= 1) { "max < 1: $maxRequestsPerHost" }
        this.maxRequestsPerHost = maxRequestsPerHost
        return this
    }

    /**
     * @see [okhttp3.OkHttpClient.Builder.protocols]
     */
    fun protocols(protocols: List<Protocol>?): OkHttpWrapper {
        if (protocols != null) {
            builder.protocols(protocols)
        }
        return this
    }

    /**
     * @param connectionSpecs
     * @return
     * @see [okhttp3.OkHttpClient.Builder.connectionSpecs]
     */
    fun connectionSpecs(connectionSpecs: List<ConnectionSpec>?): OkHttpWrapper {
        if (connectionSpecs != null) {
            builder.connectionSpecs(connectionSpecs)
        }
        return this
    }

    /**
     * @param eventListener
     * @return
     * @see [okhttp3.OkHttpClient.Builder.eventListener]
     */
    fun eventListener(eventListener: EventListener?): OkHttpWrapper {
        if (eventListener != null) {
            builder.eventListener(eventListener)
        }
        return this
    }

    /**
     * @param eventListenerFactory
     * @return
     * @see [okhttp3.OkHttpClient.Builder.eventListenerFactory]
     */
    fun eventListenerFactory(eventListenerFactory: EventListener.Factory?): OkHttpWrapper {
        if (eventListenerFactory != null) {
            builder.eventListenerFactory(eventListenerFactory)
        }
        return this
    }

    /**
     * 设置缓存目录
     * 默认缓存路径：
     *  * 1、如果存在外部sdcard则缓存在[Context.getExternalCacheDir]/diskCache/dataCache,
     *  *    即路径为sdcard/Android/data/packageName/cache/diskCache/dataCache
     *  * 2、如果不存在外部sdcard 则缓存在[Context.getCacheDir]/diskCache/dataCache,
     *  *    即路径为/data/data/packageName/cache/diskCache/dataCache
     *
     * @param cachePath
     * @author dingpeihua
     * @date 2019/10/26 9:53
     * @version 1.0
     */
    fun setCachePath(cachePath: String?): OkHttpWrapper {
        this.cachePath = cachePath
        return this
    }

    fun hasCookie(): Boolean {
        return mCookies.isNotEmpty() || !cookieMaps.isNullOrEmpty()
    }

    fun build(): OkHttpClient {
        builder.interceptors().clear()
        builder.networkInterceptors().clear()
        if (context != null && hasCookie()) {
            val sharedPrefsCookiePersistor = SharedPrefsCookiePersistor(context)
            val cookieCache: CookieCache = SetCookieCache()
            saveCookie(cookieCache)
            val cookieJar: CookieJar = PersistentCookieJar(cookieCache, sharedPrefsCookiePersistor)
            builder.cookieJar(cookieJar)
        }
        if (certsData != null && !certsData!!.isEmpty()) {
            try {
                val certificates: MutableList<InputStream> = ArrayList()
                for (bytes in certsData!!) {
                    certificates.add(ByteArrayInputStream(bytes))
                }
                setSSlSocketFactory(certificates)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (securityInterceptor != null) {
            builder.addInterceptor(securityInterceptor!!)
        }
        if (interceptors != null && interceptors!!.size > 0) {
            for (interceptor in interceptors!!) {
                builder.addInterceptor(interceptor)
            }
        }
        //设置缓存
        if (responseCacheInterceptor != null) {
            builder.addInterceptor(responseCacheInterceptor!!)
        }
        if (networkInterceptors.size > 0) {
            for (networkInterceptor in networkInterceptors) {
                builder.addNetworkInterceptor(networkInterceptor)
            }
        }
        //错误重连
        builder.retryOnConnectionFailure(true)
        val okHttpClient: OkHttpClient = builder.build()
        if (maxRequests > 1) {
            okHttpClient.dispatcher.maxRequests = maxRequests
        }
        if (maxRequestsPerHost > 1) {
            okHttpClient.dispatcher.maxRequestsPerHost = maxRequestsPerHost
        }
        return okHttpClient
    }

    /**
     * 添加证书
     *
     * @param certificates
     */
    private fun setSSlSocketFactory(certificates: List<InputStream>) {
        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null)
            try {
                var i = 0
                val size = certificates.size
                while (i < size) {
                    val certificate = certificates[i]
                    val certificateAlias = (i++).toString()
                    keyStore.setCertificateEntry(
                        certificateAlias,
                        certificateFactory.generateCertificate(certificate)
                    )
                    if (certificate != null) {
                        certificate.close()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val sslContext = SSLContext.getInstance("TLS")
            val trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            val trustManagers = trustManagerFactory.trustManagers
            sslContext.init(null, trustManagers, SecureRandom())
            val x509TrustManager = trustManagers[0] as X509TrustManager
            sslSocketFactory(sslContext.socketFactory, x509TrustManager)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated(
        "use OkHttpWrapper.clone()",
        ReplaceWith("OkHttpWrapper.clone()", "com.fz.okhttp.OkHttpWrapper")
    )
    fun clone(): OkHttpWrapper {
        return OkHttpWrapper(this)
    }

    fun saveCookie(cookieCache: CookieCache) {
        val cookies: MutableList<Cookie> = ArrayList()
        if (mCookies.isNotEmpty()) {
            for (cookieSave in mCookies) {
                val cookie = cookieSave.cookie
                if (cookieSave.isAddCookie) {
                    cookies.add(cookie)
                }
            }
        }
        if (cookieMaps != null && cookieMaps!!.size > 0) {
            for (cookieMap in cookieMaps!!) {
                if (cookieMap.hasCookies()) {
                    val values = cookieMap.mCookies
                    val length = values!!.size
                    var i = 0
                    while (i < length) {
                        val domain = values[i]
                        val path = values[i + 1]
                        val name = values[i + 2]
                        val value = values[i + 3]
                        val builder = Cookie.Builder()
                            .path(path)
                            .name(name)
                            .value(value)
                        if (cookieMap.hostOnly) {
                            builder.hostOnlyDomain(domain)
                        } else {
                            builder.domain(domain)
                        }
                        if (cookieMap.secure) {
                            builder.secure()
                        }
                        if (cookieMap.isAddCookie) {
                            cookies.add(builder.build())
                        }
                        i += 4
                    }
                }
            }
        }
        cookieCache.addAll(cookies)
    }

    companion object {
        private const val defaultTimeout: Long = 20000

        @JvmStatic
        fun newBuilder(context: Context?): OkHttpWrapper {
            if (context == null) {
                throw NullPointerException("context == null")
            }
            return OkHttpWrapper(context.applicationContext, OkHttpClient().newBuilder())
        }

        fun OkHttpWrapper.clone(): OkHttpWrapper {
            return OkHttpWrapper(this)
        }
    }
}