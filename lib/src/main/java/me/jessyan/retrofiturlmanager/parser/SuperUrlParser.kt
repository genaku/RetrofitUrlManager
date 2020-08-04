/*
 * Copyright 2018 JessYan
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
 * 超级解析器
 * 超级模式属于高级模式的加强版, 优先级高于高级模式, 在高级模式中, 需要传入一个 BaseUrl (您传入 Retrofit 的 BaseUrl) 作为被替换的基准
 * 如这个传入的 BaseUrl 为 "https://www.github.com/wiki/part" (PathSize = 2), 那框架会将所有需要被替换的 Url 中的 域名 以及 域名 后面的前两个 pathSegments
 * 使用您传入 [RetrofitUrlManager.putDomain] 方法的 Url 替换掉
 * 但如果突然有一小部分的 Url 只想将 "https://www.github.com/wiki" (PathSize = 1) 替换掉, 后面的 pathSegment '/part' 想被保留下来
 * 这时项目中就出现了多个 PathSize 不同的需要被替换的 BaseUrl
 *
 *
 * 使用高级模式实现这种需求略显麻烦, 所以我创建了超级模式, 让每一个 Url 都可以随意指定不同的 BaseUrl (PathSize 自己定) 作为被替换的基准
 * 使 RetrofitUrlManager 可以从容应对各种复杂的需求
 *
 *
 * 超级模式也需要手动开启, 但与高级模式不同的是, 开启超级模式并不需要调用 API, 只需要在 Url 中加入 [RetrofitUrlManager.IDENTIFICATION_PATH_SIZE] + PathSize
 *
 *
 * 替换规则如下:
 * 1.
 * 旧 URL 地址为 https://www.github.com/wiki/part#baseurl_path_size=1, #baseurl_path_size=1 表示其中 BaseUrl 为 https://www.github.com/wiki
 * 您调用 [RetrofitUrlManager.putDomain]方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/part
 *
 *
 * 2.
 * 旧 URL 地址为 https://www.github.com/wiki/part#baseurl_path_size=1, #baseurl_path_size=1 表示其中 BaseUrl 为 https://www.github.com/wiki
 * 您调用 [RetrofitUrlManager.putDomain]方法传入的 URL 地址是 https://www.google.com
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/part
 *
 *
 * 3.
 * 旧 URL 地址为 https://www.github.com/wiki/part#baseurl_path_size=0, #baseurl_path_size=0 表示其中 BaseUrl 为 https://www.github.com
 * 您调用 [RetrofitUrlManager.putDomain]方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/wiki/part
 *
 *
 * 4.
 * 旧 URL 地址为 https://www.github.com/wiki/part/issues/1#baseurl_path_size=3, #baseurl_path_size=3 表示其中 BaseUrl 为 https://www.github.com/wiki/part/issues
 * 您调用 [RetrofitUrlManager.putDomain]方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/1
 *
 * @see UrlParser
 * Created by JessYan on 2018/6/21 16:41
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class SuperUrlParser : UrlParser {

    private var mCache: Cache<String, String?> = LruCache(100)

    override fun parseUrl(domainUrl: HttpUrl, url: HttpUrl): HttpUrl {
        val builder = url.newBuilder()
        val pathSize = resolvePathSize(url, builder)
        val key = getKey(domainUrl, url, pathSize)
        val cashedUrl = mCache[key]
        if (cashedUrl.isNullOrBlank()) {
            for (i in 0 until url.pathSize) {
                //当删除了上一个 index, PathSegment 的 item 会自动前进一位, 所以 remove(0) 就好
                builder.removePathSegment(0)
            }
            val newPathSegments: MutableList<String> = ArrayList()
            newPathSegments.addAll(domainUrl.encodedPathSegments)
            if (url.pathSize > pathSize) {
                val encodedPathSegments = url.encodedPathSegments
                for (i in pathSize until encodedPathSegments.size) {
                    newPathSegments.add(encodedPathSegments[i])
                }
            } else require(url.pathSize >= pathSize) {
                String.format(
                        "Your final path is %s, the pathSize = %d, but the #baseurl_path_size = %d, #baseurl_path_size must be less than or equal to pathSize of the final path",
                        url.scheme + "://" + url.host + url.encodedPath, url.pathSize, pathSize)
            }
            for (PathSegment in newPathSegments) {
                builder.addEncodedPathSegment(PathSegment)
            }
        } else {
            builder.encodedPath(cashedUrl)
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

    private fun getKey(domainUrl: HttpUrl, url: HttpUrl, pathSize: Int): String =
            domainUrl.encodedPath + url.encodedPath + pathSize

    private fun resolvePathSize(httpUrl: HttpUrl, builder: HttpUrl.Builder): Int {
        val fragment = httpUrl.fragment ?: ""
        var pathSize = 0
        val newFragment = StringBuffer()
        if (fragment.indexOf("#") == -1) {
            val split = fragment.split("=".toRegex()).toTypedArray()
            if (split.size > 1) {
                pathSize = split[1].toInt()
            }
        } else {
            if (fragment.indexOf(RetrofitUrlManager.IDENTIFICATION_PATH_SIZE) == -1) {
                val index = fragment.indexOf("#")
                newFragment.append(fragment.substring(index + 1, fragment.length))
                val split = fragment.substring(0, index).split("=".toRegex()).toTypedArray()
                if (split.size > 1) {
                    pathSize = split[1].toInt()
                }
            } else {
                val split = fragment.split(RetrofitUrlManager.IDENTIFICATION_PATH_SIZE.toRegex()).toTypedArray()
                newFragment.append(split[0])
                if (split.size > 1) {
                    val index = split[1].indexOf("#")
                    if (index != -1) {
                        newFragment.append(split[1].substring(index, split[1].length))
                        val substring = split[1].substring(0, index)
                        if (!substring.isBlank()) {
                            pathSize = substring.toInt()
                        }
                    } else {
                        pathSize = split[1].toInt()
                    }
                }
            }
        }
        if (newFragment.toString().isBlank()) {
            builder.fragment(null)
        } else {
            builder.fragment(newFragment.toString())
        }
        return pathSize
    }
}