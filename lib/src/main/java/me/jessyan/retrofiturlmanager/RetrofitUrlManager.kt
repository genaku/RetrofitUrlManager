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

import me.jessyan.retrofiturlmanager.Utils.checkUrl
import me.jessyan.retrofiturlmanager.parser.DefaultUrlParser
import me.jessyan.retrofiturlmanager.parser.UrlParser
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

/**
 * ================================================
 * RetrofitUrlManager 以简洁的 Api, 让 Retrofit 不仅支持多 BaseUrl
 * 还可以在 App 运行时动态切换任意 BaseUrl, 在多 BaseUrl 场景下也不会影响到其他不需要切换的 BaseUrl
 *
 *
 * 想要更深入的使用本框架必须要了解2个术语 pathSegments 和 PathSize
 * "https://www.github.com/wiki/part?name=jess" 其中的 "/wiki" 和 "/part" 就是 pathSegment, PathSize 就是 pathSegment 的 Size
 * 这个 Url 的 PathSize 就是 2, 可以粗略理解为域名后面跟了几个 "/" PathSize 就是几
 *
 *
 * 本框架分为三种模式, 普通模式 (默认)、高级模式 (需要手动开启)、超级模式 (需要手动开启)
 *
 *
 * 普通模式:
 * 普通模式只能替换域名, 比如使用 "https:www.google.com" 作为 Retrofit 的 BaseUrl 可以被替换
 * 但是以 "https:www.google.com/api" 作为 BaseUrl 还是只能替换其中的域名 "https:www.google.com"
 * 详细替换规则可以查看 [DomainUrlParser]
 *
 *
 * 高级模式:
 * 高级模式只能替换 [.startAdvancedModel] 中传入的 BaseUrl, 但可以替换拥有多个 pathSegments 的 BaseUrl
 * 如 "https:www.google.com/api", 需要手动开启高级模式 [.startAdvancedModel]
 * 详细替换规则可以查看 [AdvancedUrlParser]
 *
 *
 * 超级模式:
 * 详细替换规则可以查看 [SuperUrlParser]
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
 * 超级模式也需要手动开启, 但与高级模式不同的是, 开启超级模式并不需要调用 API, 只需要在 Url 中加入 [.IDENTIFICATION_PATH_SIZE] + PathSize
 *
 *
 * 至此三种模式替换 BaseUrl 的自由程度 (可扩展性) 排名, 从小到大依次是:
 * 普通模式 (只能替换域名) < 高级模式 (只能替换 [.startAdvancedModel] 中传入的 BaseUrl) < 超级模式 (每个 Url 都可以随意指定可被替换的 BaseUrl, pathSize 随意变换)
 *
 *
 * 三种模式在使用上的复杂程度排名, 从小到大依次是:
 * 普通模式 (无需做过多配置) < 高级模式 (App 初始化时调用一次 [.startAdvancedModel] 即可) < 超级模式 (每个需要被替换 BaseUrl 的 Url 中都需要加入 [.IDENTIFICATION_PATH_SIZE] + PathSize)
 *
 *
 * 由此可见，自由度越强, 操作也越复杂, 所以可以根据自己的需求选择不同的模式, 并且也可以在需求变化时随意升级或降级这三种模式
 *
 *
 * Created by JessYan on 17/07/2017 14:29
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
object RetrofitUrlManager {

    private const val DOMAIN_NAME = "Domain-Name"
    private const val GLOBAL_DOMAIN_NAME = "me.jessyan.retrofiturlmanager.globalDomainName"

    const val DOMAIN_NAME_HEADER = "$DOMAIN_NAME: "

    /**
     * 如果在 Url 地址中加入此标识符, 框架将不会对此 Url 进行任何切换 BaseUrl 的操作
     */
    const val IDENTIFICATION_IGNORE = "#url_ignore"

    /**
     * 如果在 Url 地址中加入此标识符, 意味着您想对此 Url 开启超级模式, 框架会将 '=' 后面的数字作为 PathSize, 来确认最终需要被超级模式替换的 BaseUrl
     */
    const val IDENTIFICATION_PATH_SIZE = "#baseurl_path_size="

    /**
     * 获取 BaseUrl
     *
     * @return [.baseUrl]
     */
    @JvmStatic
    var baseUrl: HttpUrl? = null
        private set

    /**
     * 获取 PathSegments 的总大小
     *
     * @return PathSegments 的 size
     */
    @JvmStatic
    var pathSize = 0
        private set
    /**
     * 框架是否在运行
     *
     * @return `true` 为正在运行, `false` 为未运行
     */
    /**
     * 控制框架是否运行, 在每个域名地址都已经确定, 不需要再动态更改时可设置为 `false`
     *
     * @param run `true` 为正在运行, `false` 为未运行
     */
    @JvmStatic
    var isRun = true //默认开始运行, 可以随时停止运行, 比如您在 App 启动后已经不需要再动态切换 BaseUrl 了

    private val mDomainNameHub: MutableMap<String, HttpUrl?> = HashMap()
    private val mInterceptor: Interceptor
    private val mListeners: MutableList<onUrlChangeListener> = ArrayList()
    private var mUrlParser: UrlParser = DefaultUrlParser(this)

    init {
        val hasDependency: Boolean = try {
            Class.forName("okhttp3.OkHttpClient")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        check(hasDependency) {
            //使用本框架必须依赖 Okhttp
            "Must be dependency Okhttp"
        }
        mInterceptor = object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                return if (!isRun) chain.proceed(chain.request()) else chain.proceed(processRequest(chain.request())!!)
            }
        }
    }

    /**
     * 将 [OkHttpClient.Builder] 传入, 配置一些本框架需要的参数
     *
     * @param builder [OkHttpClient.Builder]
     * @return [OkHttpClient.Builder]
     */
    @JvmStatic
    fun with(builder: OkHttpClient.Builder): OkHttpClient.Builder =
            builder.addInterceptor(mInterceptor)

    /**
     * 对 [Request] 进行一些必要的加工, 执行切换 BaseUrl 的相关逻辑
     *
     * @param request [Request]
     * @return [Request]
     */
    @JvmStatic
    fun processRequest(request: Request?): Request? {
        request ?: return null
        val newBuilder = request.newBuilder()
        val url = request.url.toString()
        //如果 Url 地址中包含 IDENTIFICATION_IGNORE 标识符, 框架将不会对此 Url 进行任何切换 BaseUrl 的操作
        if (url.contains(IDENTIFICATION_IGNORE)) {
            return pruneIdentification(newBuilder, url)
        }
        val domainName = obtainDomainNameFromHeaders(request)
        val httpUrl: HttpUrl?
        val listeners = getListeners()

        // 如果有 header,获取 header 中 domainName 所映射的 url,若没有,则检查全局的 BaseUrl,未找到则为null
        if (!domainName.isNullOrBlank()) {
            notifyListeners(request, domainName, listeners)
            httpUrl = fetchDomain(domainName)
            newBuilder.removeHeader(DOMAIN_NAME)
        } else {
            notifyListeners(request, GLOBAL_DOMAIN_NAME, listeners)
            httpUrl = globalDomain
        }
        if (null != httpUrl) {
            val newUrl = mUrlParser.parseUrl(httpUrl, request.url)
            listeners.forEach {
                it.onUrlChanged(newUrl, request.url) // 通知监听器此 Url 的 BaseUrl 已被切换
            }
            return newBuilder
                    .url(newUrl)
                    .build()
        }
        return newBuilder.build()
    }

    /**
     * 将 `IDENTIFICATION_IGNORE` 从 Url 地址中修剪掉
     *
     * @param newBuilder [Request.Builder]
     * @param url        原始 Url 地址
     * @return 被修剪过 Url 地址的 [Request]
     */
    private fun pruneIdentification(newBuilder: Request.Builder, url: String): Request {
        val split = url.split(IDENTIFICATION_IGNORE.toRegex()).toTypedArray()
        val buffer = StringBuffer()
        for (s in split) {
            buffer.append(s)
        }
        return newBuilder
                .url(buffer.toString())
                .build()
    }

    /**
     * 通知所有监听器的 [onUrlChangeListener.onUrlChangeBefore] 方法
     *
     * @param request    [Request]
     * @param domainName 域名的别名
     * @param listeners  监听器列表
     */
    private fun notifyListeners(request: Request, domainName: String?, listeners: List<onUrlChangeListener>) {
        listeners.forEach {
            it.onUrlChangeBefore(request.url, domainName)
        }
    }

    /**
     * 开启高级模式, 高级模式可以替换拥有多个 pathSegments 的 BaseUrl, 如: https://www.github.com/wiki/part
     * 高级模式的解析规则, 请看 [me.jessyan.retrofiturlmanager.parser.AdvancedUrlParser]
     * 注意, 如果没有开启高级模式, 默认为普通默认, 只能替换域名, 如: https://www.github.com
     *
     *
     * 注意, 遇到这个坑, 请别怪框架!!! Retrofit 的 BaseUrl 含有可被覆盖 pathSegment 的规则:
     * 举例: 您设置给 Retrofit 的 BaseUrl 是 "http://www.github.com/a/b/"
     * 然后您在接口方法上给的注解是 `@GET("/path")`, 这时 Retrofit 生成的最终路径是 "http://www.github.com/path"
     * "/a/b/" 被剪切掉了, 为什么? 因为您在 "path" 前面加上了 "/", "/" 会让 Retrofit 认为把您只想保留 BaseUrl 中的域名
     * 如果去掉 "/", `@GET("path")` 得到的最终路径才是 "http://www.github.com/a/b/path"
     *
     *
     * 所以如果在最终路径中, BaseUrl 的 "/a/b/" 因为您不熟悉规则而被剪切, 这时您应该在 [.startAdvancedModel]
     * 中传入被剪切的实际 BaseUrl "http://www.github.com", 而不是 http://www.github.com/a/b/, 否则框架会理解错误!
     *
     * @param baseUrl 您当时传入 Retrofit 的 BaseUrl
     * @see me.jessyan.retrofiturlmanager.parser.AdvancedUrlParser
     */
    @JvmStatic
    fun startAdvancedModel(baseUrl: String) {
        startAdvancedModel(checkUrl(baseUrl))
    }

    /**
     * 开启高级模式, 高级模式可以替换拥有多个 pathSegments 的 BaseUrl, 如: https://www.github.com/wiki/part
     * 高级模式的解析规则, 请看 [me.jessyan.retrofiturlmanager.parser.AdvancedUrlParser]
     * 注意, 如果没有开启高级模式, 默认为普通默认, 只能替换域名, 如: https://www.github.com
     *
     *
     * 注意, 遇到这个坑, 请别怪框架!!! Retrofit 的 BaseUrl 含有可被覆盖 pathSegment 的规则:
     * 举例: 您设置给 Retrofit 的 BaseUrl 是 "http://www.github.com/a/b/"
     * 然后您在接口方法上给的注解是 `@GET("/path")`, 这时 Retrofit 生成的最终路径是 "http://www.github.com/path"
     * "/a/b/" 被剪切掉了, 为什么? 因为您在 "path" 前面加上了 "/", "/" 会让 Retrofit 认为把您只想保留 BaseUrl 中的域名
     * 如果去掉 "/", `@GET("path")` 得到的最终路径才是 "http://www.github.com/a/b/path"
     *
     *
     * 所以如果在最终路径中, BaseUrl 的 "/a/b/" 因为您不熟悉规则而被剪切, 这时您应该在 [.startAdvancedModel]
     * 中传入被剪切的实际 BaseUrl "http://www.github.com", 而不是 http://www.github.com/a/b/, 否则框架会理解错误!
     *
     * @param baseUrl 您当时传入 Retrofit 的 BaseUrl
     * @see me.jessyan.retrofiturlmanager.parser.AdvancedUrlParser
     */
    @Synchronized
    @JvmStatic
    fun startAdvancedModel(baseUrl: HttpUrl) {
        RetrofitUrlManager.baseUrl = baseUrl
        pathSize = baseUrl.pathSize
        val baseUrlPathSegments = baseUrl.pathSegments
        if (baseUrlPathSegments[baseUrlPathSegments.size - 1].isEmpty()) {
            pathSize--
        }
    }

    /**
     * 是否开启高级模式
     *
     * @return `true` 为开启, `false` 为未开启
     */
    @JvmStatic
    val isAdvancedModel: Boolean
        get() = baseUrl != null

    /**
     * 将 url 地址作为参数传入此方法, 并使用此方法返回的 Url 地址进行网络请求, 则会使此 Url 地址忽略掉本框架的所有更改效果
     *
     *
     * 使用场景:
     * 比如当您使用了 [.setGlobalDomain] 配置了全局 BaseUrl 后, 想请求一个与全局 BaseUrl
     * 不同的第三方服务商地址获取图片
     *
     * @param url Url 地址
     * @return 处理后的 Url 地址
     */
    @JvmStatic
    fun setUrlNotChange(url: String): String {
        return url + IDENTIFICATION_IGNORE
    }

    /**
     * 将 url 地址和 pathSize 作为参数传入此方法, 并使用此方法返回的 Url 地址进行网络请求, 则会使此 Url 地址使用超级模式
     *
     *
     * 什么是超级模式? 请看 [RetrofitUrlManager] 上面的注释
     *
     * @param url      Url 地址
     * @param pathSize pathSize
     * @return 处理后的 Url 地址
     */
    @JvmStatic
    fun setPathSizeOfUrl(url: String, pathSize: Int): String {
        require(pathSize >= 0) { "pathSize must be >= 0" }
        return url + IDENTIFICATION_PATH_SIZE + pathSize
    }

    /**
     * 全局动态替换 BaseUrl, 优先级: Header中配置的 BaseUrl > 全局配置的 BaseUrl
     * 除了作为备用的 BaseUrl, 当您项目中只有一个 BaseUrl, 但需要动态切换
     * 这种方式不用在每个接口方法上加入 Header, 就能实现动态切换 BaseUrl
     *
     * @param globalDomain 全局 BaseUrl
     */
    @JvmStatic
    fun setGlobalDomain(globalDomain: String) {
        val url = checkUrl(globalDomain)
        synchronized(mDomainNameHub) { mDomainNameHub.put(GLOBAL_DOMAIN_NAME, url) }
    }

    /**
     * 获取全局 BaseUrl
     */
    @get:Synchronized
    @JvmStatic
    val globalDomain: HttpUrl?
        get() = mDomainNameHub[GLOBAL_DOMAIN_NAME]

    /**
     * 移除全局 BaseUrl
     */
    @JvmStatic
    fun removeGlobalDomain() {
        synchronized(mDomainNameHub) { mDomainNameHub.remove(GLOBAL_DOMAIN_NAME) }
    }

    /**
     * 存放 Domain(BaseUrl) 的映射关系
     *
     * @param domainName
     * @param domainUrl
     */
    @JvmStatic
    fun putDomain(domainName: String, domainUrl: String) {
        val url = checkUrl(domainUrl)
        synchronized(mDomainNameHub) { mDomainNameHub.put(domainName, url) }
    }

    /**
     * 取出对应 `domainName` 的 Url(BaseUrl)
     *
     * @param domainName
     * @return
     */
    @Synchronized
    @JvmStatic
    fun fetchDomain(domainName: String): HttpUrl? {
        return mDomainNameHub[domainName]
    }

    /**
     * 移除某个 `domainName`
     *
     * @param domainName `domainName`
     */
    @JvmStatic
    fun removeDomain(domainName: String) {
        synchronized(mDomainNameHub) { mDomainNameHub.remove(domainName) }
    }

    /**
     * 清理所有 Domain(BaseUrl)
     */
    @JvmStatic
    fun clearAllDomain() {
        mDomainNameHub.clear()
    }

    /**
     * 存放 Domain(BaseUrl) 的容器中是否存在这个 `domainName`
     *
     * @param domainName `domainName`
     * @return `true` 为存在, `false` 为不存在
     */
    @Synchronized
    @JvmStatic
    fun haveDomain(domainName: String): Boolean {
        return mDomainNameHub.containsKey(domainName)
    }

    /**
     * 存放 Domain(BaseUrl) 的容器, 当前的大小
     *
     * @return 容量大小
     */
    @Synchronized
    @JvmStatic
    fun domainSize(): Int {
        return mDomainNameHub.size
    }

    /**
     * 可自行实现 [UrlParser] 动态切换 Url 解析策略
     *
     * @param parser [UrlParser]
     */
    @JvmStatic
    fun setUrlParser(parser: UrlParser) {
        mUrlParser = parser
    }

    /**
     * 注册监听器(当 Url 的 BaseUrl 被切换时会被回调的监听器)
     *
     * @param listener 监听器列表
     */
    @JvmStatic
    fun registerUrlChangeListener(listener: onUrlChangeListener) {
        synchronized(mListeners) { mListeners.add(listener) }
    }

    /**
     * 注销监听器(当 Url 的 BaseUrl 被切换时会被回调的监听器)
     *
     * @param listener 监听器列表
     */
    @JvmStatic
    fun unregisterUrlChangeListener(listener: onUrlChangeListener) {
        synchronized(mListeners) { mListeners.remove(listener) }
    }

    private fun getListeners(): List<onUrlChangeListener> {
        val listeners: List<onUrlChangeListener>
        synchronized(mListeners) {
            listeners = mListeners.toList()
        }
        return listeners
    }

    /**
     * 从 [Request.header] 中取出 DomainName
     *
     * @param request [Request]
     * @return DomainName
     */
    private fun obtainDomainNameFromHeaders(request: Request): String? {
        val headers = request.headers(DOMAIN_NAME)
        if (headers.isEmpty()) return null
        require(headers.size <= 1) { "Only one Domain-Name in the headers" }
        return request.header(DOMAIN_NAME)
    }
}