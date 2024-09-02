/*
 * Copyright (C) 2014 David Vittor http://digitalspider.com.au
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
package com.jspwiki.plugin;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

public class HitCountPlugin implements Plugin {

    private final Logger log = Logger.getLogger(HitCountPlugin.class);
    private static final String KEY_PAGEHITCOUNT = "@pageHitCount";

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        int pageHitCount = 0;
        try {
            Page currentPage = wikiContext.getPage();
            log.info("currentPage=" + currentPage);
            Object pageHitCountAtt = currentPage.getAttribute(KEY_PAGEHITCOUNT);
            Integer hitCount = 0;
            if (pageHitCountAtt != null) {
                hitCount = Integer.parseInt(pageHitCountAtt.toString());
            }
            hitCount++;
            pageHitCount = hitCount;
            currentPage.setAttribute(KEY_PAGEHITCOUNT, hitCount);
        } catch (Exception e) {
            log.error(e, e);
        }

        log.info("DONE. pageHitCount=" + pageHitCount);
        return "" + pageHitCount;
    }

}
