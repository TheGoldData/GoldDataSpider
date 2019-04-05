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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.function.BiFunction

/*
 * Created by wdg100 on 2019/1/20
 */

class LogUtils {
    private static Logger logger= LoggerFactory.getLogger('js.log')

    static class LogFunc implements BiFunction<String,String,Void> {
        @Override
        Void apply(String level,String message) {
            switch (level){
                case 'ERROR':
                    logger.error(message);
                    break
                case 'WARN':
                    logger.warn(message)
                    break;
                case 'DEBUG':
                    logger.debug(message)
                    break;
                default:
                    logger.info(message);
            }

            return null;
        }
    }
}
