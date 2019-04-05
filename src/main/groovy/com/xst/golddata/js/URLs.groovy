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

package com.xst.golddata.js


import com.xst.golddata.Crawler

import java.util.function.BiFunction
import java.util.function.Function

/*
 * Created by wdg100 on 18/4/21
 */
class URLs {

    static class ToUrl implements Function<String, URL> {

        @Override
        URL apply(String s) {
            return new URL(s);
        }
    }

    static interface AjaxRequest{
        Map apply(String url,String data,Map headers,boolean encoding);
    }
    static class ajax implements BiFunction<String,Map,Map>{
        @Override
        public Map apply(String url,Map map){
            String method='GET'
            if(map.method){
               method=map.method
            }
            boolean encoding=true;
            if(map.get('encoding')!=null){
                encoding=Boolean.parseBoolean(map.get('encoding').toString());
            }
            return new Crawler().getContent([url:url,method: method],map.headers?:[:],encoding,map.proxy?:'',map.data?:'')
        }
    }
    static class ajax2 implements Function<String,com.xst.golddata.GoldSpider>{
        @Override
        public com.xst.golddata.GoldSpider apply(String url){
            return com.xst.golddata.GoldSpider.newSpider().setUrl(url)
        }
    }




}
