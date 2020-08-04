/*
 * Copyright 2017 JessYan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.jessyan.retrofiturlmanager.parser

import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import me.jessyan.retrofiturlmanager.cache.Cache
import me.jessyan.retrofiturlmanager.cache.LruCache
import okhttp3.HttpUrl
import java.util.*

/**
 * ================================================
 * 高级解析器, 当 BaseUrl 中有除了域名以外的其他 Path 时, 可使用此解析器
 *
 *
 * 比如:
 * 1.
 * 旧 URL 地址为 https://www.github.com/wiki/part, 您在 App 初始化时传入 [RetrofitUrlManager.startAdvancedModel]
 * 的 BaseUrl 为 https://www.github.com/wiki
 * 您调用 [RetrofitUrlManager.putDomain] 方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/part
 *
 *
 * 2.
 * 旧 URL 地址为 https://www.github.com/wiki/part, 您在 App 初始化时传入 [RetrofitUrlManager.startAdvancedModel]
 * 的 BaseUrl 为 https://www.github.com/wiki
 * 您调用 [RetrofitUrlManager.putDomain] 方法传入的 URL 地址是 https://www.google.com
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/part
 *
 *
 * 3.
 * 旧 URL 地址为 https://www.github.com/wiki/part, 您在 App 初始化时传入 [RetrofitUrlManager.startAdvancedModel]
 * 的 BaseUrl 为 https://www.github.com
 * 您调用 [RetrofitUrlManager.putDomain] 方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/wiki/part
 *
 *
 * 解析器会将 BaseUrl 全部替换成您传入的 Url 地址
 *
 * @see UrlParser
 * Created by JessYan on 09/06/2018 16:00
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class AdvancedUrlParser(private val retrofitUrlManager: RetrofitUrlManager) : UrlParser {

    private var mCache: Cache<String, String?> = LruCache(100)

    override fun parseUrl(domainUrl: HttpUrl, url: HttpUrl): HttpUrl {
        val builder = url.newBuilder()
        val key = getKey(domainUrl, url)
        val cachedUrl = mCache[key]
        if (cachedUrl.isNullOrBlank()) {
            for (i in 0 until url.pathSize) {
                //当删除了上一个 index, PathSegment 的 item 会自动前进一位, 所以 remove(0) 就好
                builder.removePathSegment(0)
            }
            val newPathSegments: MutableList<String> = ArrayList()
            newPathSegments.addAll(domainUrl.encodedPathSegments)
            if (url.pathSize > retrofitUrlManager.pathSize) {
                val encodedPathSegments = url.encodedPathSegments
                for (i in retrofitUrlManager.pathSize until encodedPathSegments.size) {
                    newPathSegments.add(encodedPathSegments[i])
                }
            } else require(url.pathSize >= retrofitUrlManager.pathSize) {
                String.format("Your final path is %s, but the baseUrl of your RetrofitUrlManager#startAdvancedModel is %s",
                        url.scheme + "://" + url.host + url.encodedPath,
                        retrofitUrlManager.baseUrl?.scheme + "://"
                                + retrofitUrlManager.baseUrl?.host
                                + retrofitUrlManager.baseUrl?.encodedPath)
            }
            for (PathSegment in newPathSegments) {
                builder.addEncodedPathSegment(PathSegment)
            }
        } else {
            builder.encodedPath(cachedUrl)
        }
        val httpUrl = builder
                .scheme(domainUrl.scheme)
                .host(domainUrl.host)
                .port(domainUrl.port)
                .build()
        if (mCache[key].isNullOrBlank()) {
            mCache.put(key, httpUrl.encodedPath)
        }
        return httpUrl
    }

    private fun getKey(domainUrl: HttpUrl, url: HttpUrl): String =
            domainUrl.encodedPath + url.encodedPath + retrofitUrlManager.pathSize
}