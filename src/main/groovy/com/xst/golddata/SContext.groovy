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

/*
 * Created by wdg100 on 18/4/21
 */
public interface SContext {

    void init();

    def saveModels(List list)

    void saveModel(String url,int depth,Map ret,Map fields);

    boolean exists(String url);

    void addHref(SpiderRequest request);

    public void finish(String url,Map ret);


    public List getUrlDefs();

    public void onParseResume(String url,Map rule,String fields);

    public void onParseEmpty(String s,String content, Map rule,String fields)

    public void onRequestFail(String url, Map<String, Integer> content)

    public void onRequestResume(String s, LinkedHashMap<String, Integer> stringIntegerLinkedHashMap)

    public List getUrlDef(String s)

    def unAddedHrefList(List list)

    def addHrefs(List<SpiderRequest> list)


}
