/*
 * Copyright 2016-2019 新商态（北京）科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xst.golddata.utils

import com.google.common.escape.Escaper
import com.google.common.net.PercentEscaper
import com.google.common.net.UrlEscapers
import groovyx.net.http.UriBuilder

/*
 * Created by wdg100 on 18/4/21
 */
class URIUtils {

    public static Map<String, List<String>> deparam(String queryString) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
        final String[] pairs = queryString.split('&');
        for (String pair : pairs) {
            final int idx = pair.indexOf('=');
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, new LinkedList<String>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
        }
        return query_pairs;
    }
    public static String encodeURL(String url){
        int idx=url.indexOf('?');
        if(idx>-1){
            String requestUri=url.substring(0,idx);
            String queryStr=url.substring(idx+1);
            def x= deparam(queryStr);
            return requestUri+'?'+UriBuilder.basic(UriBuilder.root()).setQuery(x).toURI().rawQuery
        }

        return url;
    }

    static String GOOGLE_ESCAPE=UrlEscapers.URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS.replaceAll('[,\\:&;]','')

    private static final Escaper URL_PATH_SEGMENT_ESCAPER2 =
            new PercentEscaper(GOOGLE_ESCAPE , false);

    public static String encode_google(String data){
        URL_PATH_SEGMENT_ESCAPER2.escape(data);
    }
}
