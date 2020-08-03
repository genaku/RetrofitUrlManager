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

/**
 * ================================================
 * Url 监听器
 *
 *
 * Created by JessYan on 20/07/2017 14:18
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
interface onUrlChangeListener {
    /**
     * 此方法在框架使用 `domainName` 作为 key,从 [RetrofitUrlManager.mDomainNameHub]
     * 中取出对应的 BaseUrl 构建新的 Url 之前会被调用
     *
     *
     * 可以使用此回调确保 [RetrofitUrlManager.mDomainNameHub] 中是否已经存在自己期望的 BaseUrl
     * 如果不存在可以在此方法中进行 [RetrofitUrlManager.putDomain]
     *
     * @param oldUrl
     * @param domainName
     */
    fun onUrlChangeBefore(oldUrl: HttpUrl?, domainName: String?)

    /**
     * 当 Url 的 BaseUrl 被切换时回调
     * 调用时间是在接口请求服务器之前
     *
     * @param newUrl
     * @param oldUrl
     */
    fun onUrlChanged(newUrl: HttpUrl?, oldUrl: HttpUrl?)
}