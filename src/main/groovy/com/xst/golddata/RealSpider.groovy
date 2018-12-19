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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
 * Created by wdg100 on 18/4/21
 */
class RealSpider {
    Logger logger=LoggerFactory.getLogger(RealSpider)
    SpiderRequest currentRequest;
    int maxDepth=3;

    SContext ctx;

    String proxy;

    def doCrawl(){
        Crawler c=new Crawler();

        if(maxDepth!=-1 && currentRequest.depth>=maxDepth ){
            ctx.finish(currentRequest.url,[:]);
            return;
        }

        def urlDefs=getUrlDef(currentRequest.url.trim());

        if(!urlDefs){
            ctx.finish(currentRequest.url,[status:600])
            return null;
        }
        def urlContent=null;
        try{
            logger.info("start request url:{}",currentRequest.url);

            urlContent=c.getContent(currentRequest.asMap(),currentRequest.headers,true,proxy);
            if (urlContent.cookie) {
                currentRequest.headers['Cookie'] = urlContent.cookie
            }
            logger.info("end request url: status->{}",urlContent.status)
            if(new Integer(urlContent.status)>=400){
                ctx.onRequestFail(currentRequest.url,urlContent);
            }else{
                ctx.onRequestResume(currentRequest.url,urlContent);
            }
            ctx.finish(currentRequest.url,urlContent);
        }catch (Exception e){
            logger.warn("error to get url:{}",currentRequest.url,e);
            if(urlContent!=null && new Integer(urlContent.status)>=400){
                ctx.onRequestFail(currentRequest.url,urlContent);
            }
            ctx.finish(currentRequest.url, urlContent);
            return ;
        }

        urlDefs.each {urlDef->
            urlDef.each {key,value->
                if(key.startsWith('fields')){
                    try {

                        def ret=c.parse(urlContent,urlDef[key],currentRequest);
                        logger.info("parse:{} items,with key:{}",ret.size(),key)
                        if(ret ==null || ret.isEmpty()){
                            ctx.onParseEmpty(currentRequest.url,urlContent.content,urlDef,key);
                            return ;
                        }
                        ctx.onParseResume(currentRequest.url,urlDef,key);
                        parseResult(ret,urlDef,key)
                    }catch (Exception xx){
                        logger.warn("error to parse content at url:{},cause:{}",currentRequest.url,xx.message,xx);
                        ctx.onParseEmpty(currentRequest.url,urlContent.content,urlDef,key);

                    }
                }
            }
        }

    }

    def parseResult(def ret,def urlDef,def key){
        if(ret && !ret.isEmpty()){
            if(urlDef[key].__model){
                def mapRequests=[]
                ret.each { item->
                    def mapRequest=currentRequest.copy();
                    def xxx=[url:mapRequest.url,depth:mapRequest.depth+1,item:item,fields:urlDef[key]]
                    mapRequests.add(xxx)
                }
                ctx.saveModels(mapRequests);

            }else{
                def ret2=ctx.unAddedHrefList(ret)
                def mayRequests=[]
                ret2.each {
                    def item=it;
                    def childUrlDef=getUrlDef(item.href)
                    if(childUrlDef){//不为空列表.
                        def mapRequest=currentRequest.copy();

                        mapRequest.url=item.href;
                        if(!urlDef?.match.equals(childUrlDef?.match)){
                            mapRequest.depth=currentRequest.depth+1;
                        }else{
                            mapRequest.depth=currentRequest.depth;
                        }
                        mayRequests.add(mapRequest)
                    }
                }
                ctx.addHrefs(mayRequests);
            }

        }
    }

    def getUrlDef(String href){
        if(href==null){
            return null;
        }
        return ctx.getUrlDef(href);
    }

}
