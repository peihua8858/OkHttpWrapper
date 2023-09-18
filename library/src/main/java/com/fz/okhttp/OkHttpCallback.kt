package com.fz.okhttp

import android.os.Handler
import android.os.Looper
import com.fz.gson.GsonFactory
import com.fz.okhttp.utils.Util
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.socks.library.KLog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.*

/**
 * OkHttp 回调
 *
 * @author dingpeihua
 * @version 1.0
 * @date 2020/6/30 19:36
 */
class OkHttpCallback<T> @JvmOverloads constructor(
    callback: OkCallback<T>?,
    private val mGson: Gson = GsonFactory.createDefaultBuild()
        .setLenient().create(),
) : Callback {
    private val callback: OkCallback<T>? = callback
    private var adapter: TypeAdapter<T>? = null
    private val mHandler = Handler(Looper.getMainLooper())
    fun setAdapter(adapter: TypeAdapter<*>?) {
        this.adapter = adapter as TypeAdapter<T>?
    }

    override fun onFailure(call: Call, e: IOException) {
        onFailure(e)
    }

    override fun onResponse(call: Call, response: Response) {
        if (response.isSuccessful) {
            val body = response.body
            if (body != null) {
                try {
                    val bis = ByteArrayInputStream(body.bytes())
                    val reader: Reader = BufferedReader(InputStreamReader(bis))
                    val jsonReader = mGson.newJsonReader(reader)
                    val result = adapter!!.read(jsonReader)
                    reader.close()
                    bis.close()
                    onSuccess(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onFailure(UnsupportedOperationException("not support," + e.message, e))
                } finally {
                    body.close()
                }
            } else {
                onSuccess(null)
            }
        } else {
            onFailure(Exception(response.message))
        }
    }

    private fun onFailure(e: Exception) {
        mHandler.post { callback?.onFailure(e) }
    }

    private fun onSuccess(result: T?) {
        mHandler.post { callback?.onSuccess(result) }
    }

    init {
        if (callback != null) {
            try {
                val type = Util.getSuperclassTypeParameter(callback)
                setAdapter(mGson.getAdapter(TypeToken.get(type)))
            } catch (e: Exception) {
                e.printStackTrace()
                KLog.d("jetlore>>" + e.message)
            }
        }
    }
}