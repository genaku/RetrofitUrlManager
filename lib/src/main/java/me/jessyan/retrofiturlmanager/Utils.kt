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
package me.jessyan.retrofiturlmanager

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * ================================================
 * 工具类
 *
 *
 * Created by JessYan on 2017/7/24.
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
object Utils {
    @JvmStatic
    fun checkUrl(url: String?): HttpUrl =
            url?.toHttpUrlOrNull() ?: throw InvalidUrlException(url)

    @JvmStatic
    fun <T> checkNotNull(obj: T?, message: String?): T =
            obj ?: throw NullPointerException(message)
}