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

import com.xst.golddata.utils.CookieUtils
import com.xst.golddata.utils.Md5Utils

import java.util.function.Function
/*
 * Created by wdg100 on 18/4/21
 */
class JSUtils {


    static class ToMd5 implements Function<String, String> {

        @Override
        String apply(String s) {
            return  Md5Utils.md5(s);
        }
    }

    static class CookieOp implements Function<Map,String>{
        @Override
        String apply(Map s) {
            return new Cookie()
        }
    }


}