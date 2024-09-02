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

import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.util.TextUtil;

public class GoogleMapsPlugin implements Plugin {

    private static final Logger log = Logger.getLogger(GoogleMapsPlugin.class);

    public static final String PARAM_ATTACHMENT = "attachment";
    public static final String PARAM_URL = "url";
    public static final String PARAM_ALIGN = "align";
    public static final String PARAM_BORDER = "border";
    public static final String PARAM_WIDTH = "width";
    public static final String PARAM_HEIGHT = "height";
    public static final String PARAM_MARGINWIDTH = "marginwidth";
    public static final String PARAM_MARGINHEIGHT = "marginheight";
    public static final String PARAM_SCROLLING = "scrolling";

    private static final String DEFAULT_ATTACHMENT = null;
    private static final String DEFAULT_URL = null;
    private static final String DEFAULT_ALIGN = "center";
    private static final String DEFAULT_BORDER = "0";
    private static final String DEFAULT_WIDTH = "100%";
    private static final String DEFAULT_HEIGHT = "100%";
    private static final String DEFAULT_MARGINWIDTH = "10";
    private static final String DEFAULT_MARGINHEIGHT = "10";
    private static final String DEFAULT_SCROLLING = "auto";

    private String url = DEFAULT_URL;
    private String attachment = DEFAULT_ATTACHMENT;
    private String align = DEFAULT_ALIGN;
    private String border = DEFAULT_BORDER;
    private String width = DEFAULT_WIDTH;
    private String height = DEFAULT_HEIGHT;
    private String marginwidth = DEFAULT_MARGINWIDTH;
    private String marginheight = DEFAULT_MARGINHEIGHT;
    private String scrolling = DEFAULT_SCROLLING;

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        StringBuffer result = new StringBuffer();

        // Validate all parameters
        String attachment = getCleanParameter(params, PARAM_ATTACHMENT);
        String url = getCleanParameter(params, PARAM_URL);
        String align = getCleanParameter(params, PARAM_ALIGN, "center").toLowerCase();
        String border = getCleanParameter(params, PARAM_BORDER, DEFAULT_BORDER);
        String width = getCleanParameter(params, PARAM_WIDTH, DEFAULT_WIDTH);
        String height = getCleanParameter(params, PARAM_HEIGHT, DEFAULT_HEIGHT);
        String marginwidth = getCleanParameter(params, PARAM_MARGINWIDTH, DEFAULT_MARGINWIDTH);
        String marginheight = getCleanParameter(params, PARAM_MARGINHEIGHT, DEFAULT_MARGINHEIGHT);
        String scrolling = getCleanParameter(params, PARAM_SCROLLING, DEFAULT_SCROLLING);

        //log.info("url="+url+" autoPlay="+autoPlay+" items="+items+" suffix="+suffix+" prefix="+prefix);
        Page currentPage = wikiContext.getPage();
        if (attachment == null && url == null) {
            throw new PluginException("Parameter 'attachment' or 'url' is required for the MediaPlugin to work");
        }

        String src = null;

        if (attachment != null) {
            try {
                Engine engine = wikiContext.getEngine();

                AttachmentManager mgr = engine.getManager(AttachmentManager.class);

                Attachment att = mgr.getAttachmentInfo(wikiContext, attachment);

                src = wikiContext.getURL(WikiContext.ATTACH, att.getName());
            } catch (ProviderException ex) {
                throw new PluginException("Could not resolve the attachment: " + ex.getMessage());
            }
        } else {
            if (url.startsWith("http")) {
                try {
                    src = new java.net.URL(url).toExternalForm();
                } catch (java.net.MalformedURLException ex) {
                    throw new PluginException("Could not resolve the url: " + ex.getMessage());
                }
            } else {
                src = url;
            }
        }

        result.append("<iframe src=\"" + src + "\" align=\"" + align + "\" frameborder=\"" + border + "\" width=\"" + width + "\" height=\"" + height + "\" marginwidth=\"" + marginwidth + "\" marginheight=\"" + marginheight + "\" scrolling=\"" + scrolling + "\">\n");
        result.append("    Your browser does not support inline frames.\n");
        result.append("</iframe>\n");

        log.info("result=" + result.toString());
        log.info("DONE.");
        return result.toString();
    }

    private static final String getCleanParameter(Map params, String paramId, String defaultValue) {
        String value = getCleanParameter(params, paramId);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * This method is used to clean away things like quotation marks which a
     * malicious user could use to stop processing and insert javascript.
     */
    private static final String getCleanParameter(Map params, String paramId) {
        return TextUtil.replaceEntities((String) params.get(paramId));
    }
}
