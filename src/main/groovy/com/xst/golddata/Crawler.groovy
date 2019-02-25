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


import com.ibm.icu.text.CharsetDetector
import com.xst.golddata.utils.URIUtils
import groovyx.net.http.HttpBuilder
import groovyx.net.http.util.IoUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.xst.golddata.utils.CookieUtils

import javax.script.SimpleBindings
/*
 * Created by wdg100 on 18/4/21
 */
public class Crawler {

   static Logger logger = LoggerFactory.getLogger(Crawler)

    static {
        logger.info('文档参考: https://golddata.100shouhou.com/front/docs')
    }

    public Crawler() {


    }


    def getContent(Map map=[:],Map incHeaders=[:],boolean encoding=true,String proxy='',String requestData=''){
        def ret=[status:500]

        if('fake'.equals(incHeaders.__url)){//如果有__url参数表示该网站不是靠 url抓取的，url只是个数据代理。
            return [
                    status:200,
                    content: ''
            ]
        }

        def method=map.method?map.method:(incHeaders.__method?incHeaders.__method:'GET');
        if(requestData){//如果有请求内容，则也将请求方法设置为POST
            method='POST';
        }
        String  contentType;
        String accept=incHeaders['Accept'];
        if(accept){
            if(map.contentType){
                contentType=map.contentType;
            }else{
                if(!"GET".equalsIgnoreCase(method)) {
                    if(incHeaders['Content-Type']!=null){
                        contentType=incHeaders['Content-Type'];
                    }
                }
            }

        }

        if(!"GET".equalsIgnoreCase(method) && !contentType) {
            contentType='application/x-www-form-urlencoded'
        }

        String siteProxy=incHeaders['__proxy'];
        if(siteProxy){
            proxy=siteProxy
        }

        def defaultHeaders=[
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36' +
                        'Name',
                'Accept':'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8',
                'Accept-Encoding':'gzip, deflate, br'

        ];

        defaultHeaders.putAll(incHeaders);

        String headerCookieStr=incHeaders['Cookie'];
        def closeable={

            request.uri = URIUtils.encodeURL(map.url)

            if(contentType!=null && !''.equalsIgnoreCase(contentType)){
                request.contentType = contentType
            }

            defaultHeaders.each {key,val->
                if(!key.startsWith('__') && val!=null && !''.equals(val.trim())){
                    request.headers[key]=val
                }
            }

            switch (method){
                case 'POST':
                    request.body=requestData

            }

            response.parser("*/*") {cc, resp->
                ret.headers=[:]
                resp.headers.each{
                    ret.headers[it.key]=it.value;
                }

                ret.status=resp.statusCode
                if(resp.cookies.size()>0){
                    def cookieMap2=[:]
                    if(headerCookieStr!=null){
                        cookieMap2.putAll(CookieUtils.map(headerCookieStr));
                    }
                    resp.cookies.each { cookie->
                        cookieMap2[cookie.name]=cookie.value;
                    }

                    ret.cookie = CookieUtils.str(cookieMap2);
                }

                return IoUtils.streamToBytes(resp.inputStream);
            }

        }
        HttpBuilder httpConfig=null;
        if(proxy && !"".equals(proxy)){
            httpConfig=HttpConfig.get(proxy)
        }else{
             httpConfig=HttpConfig.get()
        }
        def content=null;
        Closure retry={
            switch (method){
                case "GET":
                    content = httpConfig.get(byte[], closeable)
                    break;
                case "POST":
                    content= httpConfig.post(byte[],closeable)
                    break;
                case "HEAD":
                    content= httpConfig.head(byte[],closeable)
                    break;
                case "PUT":
                    content=httpConfig.put(byte[],closeable);
                    break;
            }
        }

        try{

            retry()
        }catch (Exception e){
            if(e.getCause() instanceof  IOException && ret.status==200){
                logger.warn('retry once by crawler:{}',e.message)
                try{
                    retry()
                }catch (Exception e2){
                    logger.warn(" retry failed again :{}",e2.message)
                    return ret;
                }
            }else{
                content=e.body;
//                return ret;
            }
        }
        byte[] bytes=content;
        if(bytes!=null && encoding){
            String charset=incHeaders['__charset'];
            if(!charset){
                charset=guessCharset(bytes);
            }

            if(charset!=null){
                ret.content=new String(bytes,charset);
            }else{
                ret.content=new String(bytes,'gbk');
            }
        }else{
            ret.content=bytes;
        }


        return ret;

    }

    public static String guessCharset(byte[] is) throws IOException {
        CharsetDetector charDect = new CharsetDetector();
        charDect.setText(is)
        return charDect.detect()?.getName()
    }

    public static String guessCharsetByInputStream(InputStream is) throws IOException {
        return guessCharset(is.bytes);
    }

    def parse(Map res,Map fields,SpiderRequest request,StringBuffer errors=null){
        String baseUri=request.url
        def doc=null;
        if('js'.equals(fields.'__node')){
           doc= [content:res.content,baseUri: baseUri] ;
        }else{
            doc=Jsoup.parse(res.content,baseUri);
        }
        def ret=[];
        List<Element> eleList=[];

        if(fields.'__node'){
            if('js'.equals(fields.'__node')){
                Map condition=[
                        request:request,
                        baseUri:doc.baseUri,
                        html:doc.content,
                        headers:res.headers,
                        cookie:res.cookie,
                        status: res.status,
                        fields:fields,
                        out:ret,
                        newMap:{return [:]},
                        newUrl:{obj-> return new URL(obj)}
                ]

                if(fields.__js){
                    SimpleBindings simple=   new SimpleBindings(condition)
                    try{
                        JSScriptManager.newSingleEngine().eval(fields.__js.trim(),simple)
                    }catch (Exception e){
                        String cause=ExceptionUtils.getRootCauseMessage(e);
                        if(errors!=null){
                            errors.append(cause);
                        }

                        logger.warn("error exec js:{},cause:{}",fields.__js.trim(),cause)
                    }
                }
                if(ret && !ret.isEmpty()) {
                    def __limit = fields.__limit;
                    if (__limit) {//如果有限制条数,则进行剪切！
                        ret = ret.subList(0, Math.min(ret.size(), __limit))
                    }
                }
                return ret;
            }else{
                Elements eles=doc.select(fields.'__node'.trim());
                if(!eles.isEmpty()){
                    eleList.addAll(eles)
                }
            }

        }else{
            eleList.add(doc);
        }
        String html= doc.outerHtml();
        eleList.eachWithIndex {it,eleIdx->
            Element ele=it;
            def item=[:]
            def subFields=fields.findAll ( {key,val->
                if( key.startsWith('__')){
                    return false;
                }
                if(val.expr){
                    return true
                } else {
                    return false;
                }
            })
            subFields.each {key,val->

                Elements eleRets=ele.select(val.expr.trim())
                String s=null;
                if(!eleRets.isEmpty()){
                    Element first=eleRets.first()

                    if((val.attr)){
                        s=first.attr(val.attr.trim());
                    }else{
                        s=first.html()?.trim()
                    }

                    Map condition=[
                            request:request,
                            baseUri:doc.baseUri(),
                            source:s,
                            html:html,
                            headers: res.headers,
                            cookie:res.cookie,
                            status: res.status,
                            eleIdx:eleIdx,
                            newUrl:{obj-> return new URL(obj)}
                    ]

                    if((val.js)){
                        SimpleBindings simple=   new SimpleBindings(condition)
                        try {
                            s= (String)JSScriptManager.newSingleEngine().eval(val.js.trim(),simple)
                        } catch (Exception e) {
                            String cause=ExceptionUtils.getRootCauseMessage(e);
                            if(errors!=null){
                                errors.append(cause);
                            }
                            logger.warn("error exec js:{},cause:{}", val.js, cause)
                        }
                    }else{

                        if(condition.source){
                            s=condition.source;
                        }
                    }
                    item[key]=s;
                }else {
                    Map condition = [
                            request:request,
                            baseUri:doc.baseUri(),
                            source: null,
                            html   : html,
                            headers: res.headers,
                            cookie:res.cookie,
                            status: res.status,
                            eleIdx:eleIdx,
                            newUrl : { obj -> return new URL(obj) }
                    ]

                    if (val.js) {
                        SimpleBindings simple = new SimpleBindings(condition)
                        try {
                            s = (String) JSScriptManager.newSingleEngine().eval(val.js.trim(), simple)
                        } catch (Exception e) {
                            String cause=ExceptionUtils.getRootCauseMessage(e);
                            if(errors!=null){
                                errors.append(cause);
                            }
                            logger.warn("error exec js:{},cause:{}", val.js, cause)
                        }
                    }

                    item[key] = s;
                }
            }
            subFields=fields.findAll ( {key,val->
                if( key.startsWith('__')){
                    return false;
                }
                if(val.expr){
                    return false
                } else {
                    return true;
                }
            })
            subFields.each { key, val ->
                if( key.startsWith('__')){
                    return;
                }
                Map condition=[
                        request:request,
                        baseUri:doc.baseUri(),
                        item:item,
                        html:html,
                        headers: res.headers,
                        cookie:res.cookie,
                        status: res.status,
                        eleIdx:eleIdx,
                        newUrl:{obj-> return new URL(obj)}
                ]

                if(val.js){
                    SimpleBindings simple=   new SimpleBindings(condition)
                    try{
                        item[key]= (String)JSScriptManager.newSingleEngine().eval(val.js.trim(),simple)
                    }catch (Exception e){
                        String cause=ExceptionUtils.getRootCauseMessage(e);
                        if(errors!=null){
                            errors.append(cause);
                        }
                        logger.warn("error exec js:{},cause:{}",val.js.trim(),cause)
                    }
                }
            }

            if(item){
                ret<<item;
            }
        }
        if(ret && !ret.isEmpty()) {
            def __limit = fields.__limit;
            if (__limit) {//如果有限制条数,则进行剪切！
                ret = ret.subList(0, Math.min(ret.size(), __limit))
            }
        }
        return ret;
    }


}

