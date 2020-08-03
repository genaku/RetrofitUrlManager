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
package me.jessyan.retrofiturlmanager.demo

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.fetchDomain
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.globalDomain
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.putDomain
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.registerUrlChangeListener
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.removeGlobalDomain
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.setGlobalDomain
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.startAdvancedModel
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.unregisterUrlChangeListener
import me.jessyan.retrofiturlmanager.demo.api.Api
import me.jessyan.retrofiturlmanager.onUrlChangeListener
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import java.io.IOException

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
 * 高级模式只能替换 [RetrofitUrlManager.startAdvancedModel] 中传入的 BaseUrl, 但可以替换拥有多个 pathSegments 的 BaseUrl
 * 如 "https:www.google.com/api", 需要手动开启高级模式 [RetrofitUrlManager.startAdvancedModel]
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
 * 超级模式也需要手动开启, 但与高级模式不同的是, 开启超级模式并不需要调用 API, 只需要在 Url 中加入 [RetrofitUrlManager.IDENTIFICATION_PATH_SIZE] + PathSize
 *
 *
 * 至此三种模式替换 BaseUrl 的自由程度 (可扩展性) 排名, 从小到大依次是:
 * 普通模式 (只能替换域名) < 高级模式 (只能替换 [RetrofitUrlManager.startAdvancedModel] 中传入的 BaseUrl) < 超级模式 (每个 Url 都可以随意指定可被替换的 BaseUrl, pathSize 随意变换)
 *
 *
 * 三种模式在使用上的复杂程度排名, 从小到大依次是:
 * 普通模式 (无需做过多配置) < 高级模式 (App 初始化时调用一次 [RetrofitUrlManager.startAdvancedModel] 即可) < 超级模式 (每个需要被替换 BaseUrl 的 Url 中都需要加入 [RetrofitUrlManager.IDENTIFICATION_PATH_SIZE] + PathSize)
 *
 *
 * 由此可见，自由度越强, 操作也越复杂, 所以可以根据自己的需求选择不同的模式, 并且也可以在需求变化时随意升级或降级这三种模式
 *
 *
 * Created by JessYan on 18/07/2017 17:03
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class MainActivity : AppCompatActivity() {
    private var mUrl1: EditText? = null
    private var mUrl2: EditText? = null
    private var mUrl3: EditText? = null
    private var mGlobalUrl: EditText? = null
    private var mProgressDialog: ProgressDialog? = null
    private lateinit var mListener: ChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initListener()
    }

    private fun initView() {
        setContentView(R.layout.activity_main)
        mUrl1 = findViewById<View>(R.id.et_url1) as EditText
        mUrl2 = findViewById<View>(R.id.et_url2) as EditText
        mUrl3 = findViewById<View>(R.id.et_url3) as EditText
        mGlobalUrl = findViewById<View>(R.id.et_global_url) as EditText
        mProgressDialog = ProgressDialog(this)
        mUrl1!!.setSelection(mUrl1!!.text.toString().length)
    }

    private fun initListener() {
        mListener = ChangeListener()
        //如果有需要可以注册监听器,当一个 Url 的 BaseUrl 被新的 Url 替代,则会回调这个监听器,调用时间是在接口请求服务器之前
        registerUrlChangeListener(mListener)
        //如果您已经确定了最终的 BaseUrl ,不需要再动态切换 BaseUrl, 请 RetrofitUrlManager.setRun(false);
        findViewById<View>(R.id.bt_request1).setOnClickListener {
            val httpUrl = fetchDomain(Api.GITHUB_DOMAIN_NAME)
            if (httpUrl == null || httpUrl.toString() != mUrl1!!.text.toString()) { //可以在 App 运行时随意切换某个接口的 BaseUrl
                putDomain(Api.GITHUB_DOMAIN_NAME, mUrl1!!.text.toString())
            }
            NetWorkManager
                    .getInstance()
                    .oneApiService
                    .getUsers(1, 10)
                    .compose(getDefaultTransformer())
                    .subscribe(defaultObserver)
        }
        findViewById<View>(R.id.bt_request2).setOnClickListener {
            val httpUrl = fetchDomain(Api.GANK_DOMAIN_NAME)
            if (httpUrl == null || httpUrl.toString() != mUrl2!!.text.toString()) { //可以在 App 运行时随意切换某个接口的 BaseUrl
                putDomain(Api.GANK_DOMAIN_NAME, mUrl2!!.text.toString())
            }
            /**
             * 使用超级模式请求, 详细注释请查看 [me.jessyan.retrofiturlmanager.demo.api.TwoApiService.getData]
             */
            /**
             * 使用超级模式请求, 详细注释请查看 [me.jessyan.retrofiturlmanager.demo.api.TwoApiService.getData]
             */
            NetWorkManager
                    .getInstance()
                    .twoApiService
                    .getData(10, 1)
                    .compose(getDefaultTransformer())
                    .subscribe(defaultObserver)
        }
        findViewById<View>(R.id.bt_request3).setOnClickListener {
            val httpUrl = fetchDomain(Api.DOUBAN_DOMAIN_NAME)
            if (httpUrl == null || httpUrl.toString() != mUrl3!!.text.toString()) { //可以在 App 运行时随意切换某个接口的 BaseUrl
                putDomain(Api.DOUBAN_DOMAIN_NAME, mUrl3!!.text.toString())
            }
            NetWorkManager
                    .getInstance()
                    .threeApiService
                    .getBook(1220562)
                    .compose(getDefaultTransformer())
                    .subscribe(defaultObserver)
        }
    }

    private fun showResult(result: String?) {
        AlertDialog.Builder(this)
                .setMessage(result)
                .setCancelable(true)
                .setPositiveButton("ok") { dialog, which -> dialog.dismiss() }
                .setNegativeButton("cancel") { dialog, which -> dialog.dismiss() }
                .create()
                .show()
    }

    /**
     * 什么是高级模式? 什么是 pathSegment? 建议先看看最上面的类注释! [MainActivity]
     */
    fun startAdvancedModel(view: View?) {
        val editText = EditText(this@MainActivity)
        editText.setBackgroundDrawable(null)
        editText.setText("http://jessyan.me/1")
        editText.setPadding(80, 30, 0, 0)
        AlertDialog.Builder(this)
                .setTitle("增加或减少下面的 pathSegment, 看看替换后的 Url 有什么不同?")
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton("ok") { dialog, which ->
                    dialog.dismiss()
                    startAdvancedModel(editText.text.toString())
                }
                .setNegativeButton("cancel") { dialog, which -> dialog.dismiss() }
                .create()
                .show()
    }

    fun urlNotChange(view: View?) {
        showResult("使用本框架的全局 BaseUrl 后, 默认整个项目的所有 Url 都会被全局 BaseUrl 替换, 但是在实际开发中又需要某些 Url 保持原样不被全局 BaseUrl 替换掉, 比如请求某些固定的图片下载地址时, 这时在这个 Url 地址尾部加上 RetrofitUrlManager.IDENTIFICATION_IGNORE 即可避免被全局 BaseUrl 替换, 可以使用 RetrofitUrlManager.setUrlNotChange(url) 方法, 该方法返回的 Url 已帮您自动在 Url 尾部加上该标志!")
    }

    // 请求默认 BaseUrl，请求的接口没有配置 DomainHeader，所以只受全局 BaseUrl的影响
    fun btnRequestDefault(view: View?) {
        NetWorkManager
                .getInstance()
                .oneApiService
                .requestDefault()
                .compose(getDefaultTransformer())
                .subscribe(defaultObserver)
    }

    // 设置全局替换的 BaseUrl
    fun btnSetGlobalUrl(view: View?) {
        //当您项目中只有一个 BaseUrl, 但需要动态切换 BaseUrl 时, 全局 BaseUrl 显得非常方便
        //使用 RetrofitUrlManager.setUrlNotChange(url); 方法处理过的 url 地址进行网络请求
        //则可以使此 url 地址忽略掉本框架的所有更改效果
        val httpUrl = globalDomain
        if (null == httpUrl || httpUrl.toString() != mGlobalUrl!!.text.toString().trim { it <= ' ' }) setGlobalDomain(mGlobalUrl!!.text.toString().trim { it <= ' ' })
        Toast.makeText(applicationContext, "全局替换baseUrl成功", Toast.LENGTH_SHORT).show()
    }

    // 移除全局的 BaseUrl
    fun btnRmoveGlobalUrl(view: View?) {
        //不想再使用全局 BaseUrl ,想用之前传入 Retrofit 的默认 BaseUrl ,就Remove
        removeGlobalDomain()
        Toast.makeText(applicationContext, "移除了全局baseUrl", Toast.LENGTH_SHORT).show()
    }

    private fun <T> getDefaultTransformer(): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream ->
            upstream.subscribeOn(Schedulers.io())
                    .doOnSubscribe { mProgressDialog!!.show() }
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { mProgressDialog!!.dismiss() }
        }
    }

    private val defaultObserver: Observer<ResponseBody>
        get() = object : Observer<ResponseBody> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(response: ResponseBody) {
                try {
                    val string = response.string()
                    Log.d("test", string)
                    showResult(string)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun onError(throwable: Throwable) {
                throwable.printStackTrace()
                showResult(throwable.message)
            }

            override fun onComplete() {}
        }

    override fun onDestroy() {
        super.onDestroy()
        unregisterUrlChangeListener(mListener) //记住注销监听器
    }

    private inner class ChangeListener : onUrlChangeListener {
        override fun onUrlChangeBefore(oldUrl: HttpUrl?, domainName: String?) {
            Log.d("MainActivity", String.format("The oldUrl is <%s>, ready fetch <%s> from DomainNameHub",
                    oldUrl.toString(),
                    domainName))
        }

        override fun onUrlChanged(newUrl: HttpUrl?, oldUrl: HttpUrl?) {
            Observable.just(1)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ Toast.makeText(applicationContext, "The newUrl is { " + newUrl.toString() + " }", Toast.LENGTH_LONG).show() }) { throwable -> throwable.printStackTrace() }
        }
    }
}