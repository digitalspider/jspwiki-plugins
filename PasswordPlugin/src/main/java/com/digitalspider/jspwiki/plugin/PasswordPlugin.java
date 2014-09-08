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
package com.digitalspider.jspwiki.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

import java.util.Map;

public class PasswordPlugin implements WikiPlugin {

	private final Logger log = Logger.getLogger(PasswordPlugin.class);

    private static final String DEFAULT_ID = null;

    private static final String PARAM_ID = "id";

    private String id = DEFAULT_ID;

	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();

        // Validate all parameters
        validateParams(wikiContext, params);

        WikiEngine engine = wikiContext.getEngine();
        PageManager pageManager = engine.getPageManager();
        String baseUrl = engine.getBaseURL();

        try {

        } catch (Exception e) {
            log.error(e,e);
            throw new PluginException(e.getMessage());
        }

		return result;
	}

    protected void validateParams(WikiContext wikiContext, Map<String, String> params) throws PluginException {
        String paramName;
        String param;

        log.info("validateParams() START");
        paramName = PARAM_ID;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            id = param;
        }
    }

    private String getPropKey(String currentKey, String source) {
        String result = currentKey;
        if (StringUtils.isNotBlank(source)) {
            result+="."+source;
        }
        return result;
    }
}
