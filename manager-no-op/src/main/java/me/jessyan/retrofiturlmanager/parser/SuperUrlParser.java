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
package me.jessyan.retrofiturlmanager.parser;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import me.jessyan.retrofiturlmanager.RetrofitUrlManager;
import me.jessyan.retrofiturlmanager.cache.Cache;
import me.jessyan.retrofiturlmanager.cache.LruCache;
import okhttp3.HttpUrl;

import static me.jessyan.retrofiturlmanager.RetrofitUrlManager.IDENTIFICATION_PATH_SIZE;

/**
 * ================================================
 * 超级解析器
 * 超级模式属于高级模式的加强版, 优先级高于高级模式, 在高级模式中, 需要传入一个 BaseUrl (您传入 Retrofit 的 BaseUrl) 作为被替换的基准
 * 如这个传入的 BaseUrl 为 "https://www.github.com/wiki/part" (PathSize = 2), 那框架会将所有需要被替换的 Url 中的 域名 以及 域名 后面的前两个 pathSegments
 * 使用您传入 {@link RetrofitUrlManager#putDomain(String, String)} 方法的 Url 替换掉
 * 但如果突然有一小部分的 Url 只想将 "https://www.github.com/wiki" (PathSize = 1) 替换掉, 后面的 pathSegment '/part' 想被保留下来
 * 这时项目中就出现了多个 PathSize 不同的需要被替换的 BaseUrl
 * <p>
 * 使用高级模式实现这种需求略显麻烦, 所以我创建了超级模式, 让每一个 Url 都可以随意指定不同的 BaseUrl (PathSize 自己定) 作为被替换的基准
 * 使 RetrofitUrlManager 可以从容应对各种复杂的需求
 * <p>
 * 超级模式也需要手动开启, 但与高级模式不同的是, 开启超级模式并不需要调用 API, 只需要在 Url 中加入 {@link RetrofitUrlManager#IDENTIFICATION_PATH_SIZE} + PathSize
 * <p>
 * 替换规则如下:
 * 1.
 * 旧 URL 地址为 https://www.github.com/wiki/part#baseurl_path_size=1, #baseurl_path_size=1 表示其中 BaseUrl 为 https://www.github.com/wiki
 * 您调用 {@link RetrofitUrlManager#putDomain(String, String)}方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/part
 * <p>
 * 2.
 * 旧 URL 地址为 https://www.github.com/wiki/part#baseurl_path_size=1, #baseurl_path_size=1 表示其中 BaseUrl 为 https://www.github.com/wiki
 * 您调用 {@link RetrofitUrlManager#putDomain(String, String)}方法传入的 URL 地址是 https://www.google.com
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/part
 * <p>
 * 3.
 * 旧 URL 地址为 https://www.github.com/wiki/part#baseurl_path_size=0, #baseurl_path_size=0 表示其中 BaseUrl 为 https://www.github.com
 * 您调用 {@link RetrofitUrlManager#putDomain(String, String)}方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/wiki/part
 * <p>
 * 4.
 * 旧 URL 地址为 https://www.github.com/wiki/part/issues/1#baseurl_path_size=3, #baseurl_path_size=3 表示其中 BaseUrl 为 https://www.github.com/wiki/part/issues
 * 您调用 {@link RetrofitUrlManager#putDomain(String, String)}方法传入的 URL 地址是 https://www.google.com/api
 * 经过本解析器解析后生成的新 URL 地址为 http://www.google.com/api/1
 *
 * @see UrlParser
 * Created by JessYan on 2018/6/21 16:41
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * ================================================
 */
public class SuperUrlParser implements UrlParser {
    private RetrofitUrlManager mRetrofitUrlManager;
    private Cache<String, String> mCache;

    @Override
    public void init(RetrofitUrlManager retrofitUrlManager) {
        // no-op
    }

    @Override
    public HttpUrl parseUrl(HttpUrl domainUrl, HttpUrl url) {
        // no-op
        return url;
    }

    private String getKey(HttpUrl domainUrl, HttpUrl url, int PathSize) {
        return "no-op";
    }

    private int resolvePathSize(HttpUrl httpUrl, HttpUrl.Builder builder) {
        // no-op
        return -1;
    }
}
