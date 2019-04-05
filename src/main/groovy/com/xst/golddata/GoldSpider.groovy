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

package com.xst.golddata

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.hjson.JsonValue

 class GoldSpider {
    String method='GET'
    String url
    Map headers=[:]
    String proxy='';
    String data;
    String rule;
    private Map res=null;
    static GoldSpider newSpider(){
        return new GoldSpider();
    }

    def setUrl(String url){
        this.url=url
        return this;
    }
    def setMethod(String method){
        this.method=method;
        return this;
    }
    def setProxy(String proxy){
        this.proxy=proxy
        return this
    }

    def setRule(String rule){
        this.rule=rule;
        return this;
    }

    def setData(String data){
        this.data=data;
        return this;
    }

    def setHeaders(Map headers){
        headers.putAll(headers)
        return this
    }

    def addHeader(String key,String val){
        headers.put(key,val)
        return this;
    }

    def request(){
        def res=new Crawler().getContent([url:url,method: method],headers,false,proxy,data);
        res.bytes=res.content
        this.res=res;
        return this;
    }

    def getBodyAsBytes(){
        return this.res.bytes;
    }

    String getBodyAsString(){
        String content=null
        String charset=headers['__charset'];
        if(res.bytes!=null){
            if(!charset){
                charset=new Crawler().guessCharset(res.bytes);
            }

            if(charset!=null){
                content=new String(res.bytes,charset);
            }else{
                content=new String(res.bytes,'gbk');
            }
        }

        return content;
    }

    public  <T> T getBodyAsType(Class<T> type){
        return new ObjectMapper().readValue(getBodyAsString(),type)
    }

    public List<Map> extractList(){
        res.content=getBodyAsString();
        SpiderRequest request=new SpiderRequest(url: url);
        return new Crawler().parse(res,parseRule(),request)
    }
    public <T> List<T> extractList(Class<T> clzz){
        List list= extractList()
        String ss= JsonOutput.toJson(list);
        ObjectMapper om=  new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JavaType javaType=om.typeFactory.constructCollectionType(List.class,clzz);
        return om.readValue(ss,javaType)
    }

    public Map extractFirst(){
        List list= extractList()
        if(!list.isEmpty()){
            return list.get(0)
        }
        return null;
    }

    public <T> T extractFirstAsType(Class<T> clzz){
        List list= extractList()
        if(!list.isEmpty()){
            String s=JsonOutput.toJson(list.get(0))
            ObjectMapper om=  new ObjectMapper();
            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return  om.readValue(s,clzz);
        }
        return null;
    }




    def parseRule(){
        String content=rule;
        def rule= JsonValue.readHjson(content).toString();
        rule=new ObjectMapper().readValue(rule,Map.class);
        Map copy=[:]
        copy.putAll(rule);
        copy.each { ruleKey,ruleValue->
            if(ruleKey.startsWith("fields")){
                if(ruleValue==null){
                    return;
                }
                String temp=null;
                if(ruleValue.__name==null || ''.equals(ruleValue.__name)){
                    temp=ruleKey.substring("fields".length());
                }else{
                    temp=ruleValue.__name.toString();
                }
                ruleValue.__name=temp;
            }
        }
        return rule;
    }

}
