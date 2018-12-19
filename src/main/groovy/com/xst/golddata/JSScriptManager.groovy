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
 *
 */

package com.xst.golddata

import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import com.xst.golddata.js.Dates
import com.xst.golddata.js.JSUtils
import com.xst.golddata.js.Soups
import com.xst.golddata.js.URLs

import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine

/*
 * Created by wdg100 on 18/4/21
 */
class JSScriptManager {

    private static SecureJSFilter secureJSFilter=new SecureJSFilter();

    static NashornScriptEngineFactory jsfactory = new NashornScriptEngineFactory();

    private static ScriptEngine singleEngine=null

    public static ScriptEngine newSingleEngine(){
        if(singleEngine==null){
            singleEngine=newEngine();
        }
        return singleEngine;
    }


    public static ScriptEngine newEngine(){
        def engine= jsfactory.getScriptEngine(secureJSFilter);
        Bindings bindings=engine.createBindings();
        bindings.put('formatDate',new Dates.FormatDate() )
        bindings.put('parseDate',new Dates.ParseDate())
        bindings.put('url',new URLs.ToUrl())
        bindings.put('md5',new JSUtils.ToMd5())

        bindings.put('$',new Soups.newSoup())
        bindings.put('$ajax',new URLs.ajax());
        bindings.put('$ajax2',new URLs.ajax2());
        bindings.put('$cookie',new JSUtils.CookieOp())

        //缩短写法
        bindings.put('Map',engine.eval('Java.type("java.util.HashMap")'));
        bindings.put('JDate',engine.eval('Java.type("java.util.Date")'));
        bindings.put('Xml',engine.eval('Java.type("groovy.util.XmlSlurper")'));
        engine.setBindings(bindings,ScriptContext.GLOBAL_SCOPE);

        return engine;
    }


}
