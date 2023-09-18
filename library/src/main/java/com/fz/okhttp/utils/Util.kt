package com.fz.okhttp.utils

import com.google.gson.internal.`$Gson$Types`
import okhttp3.HttpUrl
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object Util {
    /**
     * 根据指定对象获取其泛型的实际类型
     * 返回的类型为[java.io.Serializable]。
     *
     * @param obj
     * @return
     * @see {@link $Gson$Types.canonicalize
     */
    @JvmStatic
    fun getSuperclassTypeParameter(obj: Any): Type {
        return getSuperclassTypeParameter(obj.javaClass)
    }

    /**
     * 根据指定类型获取其泛型的实际类型
     * 返回的类型为[java.io.Serializable]。
     *
     * @param subclass
     * @return
     * @see {@link $Gson$Types.canonicalize
     */
    @JvmStatic
    fun getSuperclassTypeParameter(subclass: Class<*>): Type {
        val superclass = subclass.genericSuperclass
        if (superclass !is ParameterizedType) {
            throw RuntimeException("Missing type parameter.")
        }
        return getParameterUpperBound(0, superclass)
    }

    /**
     * 根据传入的参数化类型获取实际类型
     * 返回在功能上相等但不一定相等的类型。
     * 返回的类型为[java.io.Serializable]。
     *
     * @param index
     * @param type
     * @return
     * @see {@link $Gson$Types.canonicalize
     */
    @JvmStatic
    fun getParameterUpperBound(index: Int, type: ParameterizedType): Type {
        val types = type.actualTypeArguments
        require(!(index < 0 || index >= types.size)) { "Index " + index + " not in range [0," + types.size + ") for " + type }
        return `$Gson$Types`.canonicalize(types[index])
    }

    /**
     * 将Object对象转成String类型
     *
     * @param value
     * @return 如果value不能转成String，则默认""
     */
    @JvmOverloads
    @JvmStatic
    fun toString(value: Any?, defaultValue: String = ""): String {
        if (value is String) {
            return value
        } else if (value != null) {
            return value.toString()
        }
        return defaultValue
    }
    @JvmStatic
    fun buildGetParams(httpBuilder: HttpUrl.Builder, params: Map<String, Any?>?) {
        if (!params.isNullOrEmpty()) {
            val iterator = params.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                httpBuilder.addEncodedQueryParameter(entry.key, toString(entry.value))
            }
        }
    }
}