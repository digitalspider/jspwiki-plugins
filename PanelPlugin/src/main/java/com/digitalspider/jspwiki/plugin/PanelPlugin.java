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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.plugin.DefaultPluginManager;
import org.apache.wiki.ui.TemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PanelPlugin implements WikiPlugin {

	private final Logger log = Logger.getLogger(PanelPlugin.class);

    public static final String DEFAULT_ID = "1";
    public static final String DEFAULT_CLASSID = "default";
    public static final Boolean DEFAULT_SHOWEDIT = false;
    public static final String DEFAULT_HEADER = "";
    public static final String DEFAULT_FOOTER = "";
    public static final String DEFAULT_WIDTH = "";
    public static final String DEFAULT_MINWIDTH = "";
    public static final String DEFAULT_HEIGHT = "";
    public static final String DEFAULT_MINHEIGHT = "";
    public static final String DEFAULT_COLORPANELBG = "";
    public static final String DEFAULT_COLORHEADERBG = "";
    public static final String DEFAULT_COLORCONTENTBG = "";
    public static final String DEFAULT_COLORFOOTERBG = "";
    public static final String DEFAULT_COLORPANELTEXT = "";
    public static final String DEFAULT_COLORHEADERTEXT = "";
    public static final String DEFAULT_COLORCONTENTTEXT = "";
    public static final String DEFAULT_COLORFOOTERTEXT = "";
    public static final String DEFAULT_COLORBORDER = "";
    public static final String DEFAULT_MARGIN = "";
    public static final String DEFAULT_PADDING = "";
    public static final String DEFAULT_CORNERS = "";

    private static final String PARAM_ID = "id";
    private static final String PARAM_CLASSID = "classid";
    private static final String PARAM_SHOWEDIT = "showedit";
    private static final String PARAM_HEADER = "header";
    private static final String PARAM_FOOTER = "footer";
    private static final String PARAM_WIDTH = "width";
    private static final String PARAM_MINWIDTH = "minwidth";
    private static final String PARAM_HEIGHT = "height";
    private static final String PARAM_MINHEIGHT = "minheight";
    private static final String PARAM_COLORPANELBG = "colorpanelbg";
    private static final String PARAM_COLORHEADERBG = "colorheaderbg";
    private static final String PARAM_COLORCONTENTBG = "colorcontentbg";
    private static final String PARAM_COLORFOOTERBG = "colorfooterbg";
    private static final String PARAM_COLORPANELTEXT = "colorpaneltext";
    private static final String PARAM_COLORHEADERTEXT = "colorheadertext";
    private static final String PARAM_COLORCONTENTTEXT = "colorcontenttext";
    private static final String PARAM_COLORFOOTERTEXT = "colorfootertext";
    private static final String PARAM_COLORBORDER = "colorborder";
    private static final String PARAM_MARGIN = "margin";
    private static final String PARAM_PADDING = "padding";
    private static final String PARAM_CORNERS = "corners";

    private String id = DEFAULT_ID;
    private String classId = DEFAULT_CLASSID;
    private Boolean showEdit = DEFAULT_SHOWEDIT;
    private String header = DEFAULT_HEADER;
    private String footer = DEFAULT_FOOTER;
    private String width = DEFAULT_WIDTH;
    private String minwidth = DEFAULT_MINWIDTH;
    private String height = DEFAULT_HEIGHT;
    private String minheight = DEFAULT_MINHEIGHT;
    private String colorpanelbg = DEFAULT_COLORPANELBG;
    private String colorheaderbg = DEFAULT_COLORHEADERBG;
    private String colorcontentbg = DEFAULT_COLORCONTENTBG;
    private String colorfooterbg = DEFAULT_COLORFOOTERBG;
    private String colorpaneltext = DEFAULT_COLORPANELTEXT;
    private String colorheadertext = DEFAULT_COLORHEADERTEXT;
    private String colorcontenttext = DEFAULT_COLORCONTENTTEXT;
    private String colorfootertext = DEFAULT_COLORFOOTERTEXT;
    private String colorborder = DEFAULT_COLORBORDER;
    private String margin = DEFAULT_MARGIN;
    private String padding = DEFAULT_PADDING;
    private String corners = DEFAULT_CORNERS;


    private static final String RESOURCE_PANEL_JS = "panel/panel.js";
    private static final String RESOURCE_PANEL_CSS = "panel/panel.css";
    private List<String> pageResources = new ArrayList<String>();


	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
        setLogForDebug(params.get(PluginManager.PARAM_DEBUG));
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();
        WikiEngine engine = wikiContext.getEngine();
        Properties props = engine.getWikiProperties();

        addUniqueTemplateResourceRequest(wikiContext, TemplateManager.RESOURCE_SCRIPT, RESOURCE_PANEL_JS);
        addUniqueTemplateResourceRequest(wikiContext, TemplateManager.RESOURCE_STYLESHEET, RESOURCE_PANEL_CSS);

        // Validate all parameters
        validateParams(props, params);

        try {
            String htmlBody = "";
            String body = params.get(DefaultPluginManager.PARAM_BODY);
            if (StringUtils.isNotBlank(body)) {
                htmlBody = engine.textToHTML(wikiContext, body);
            }
            buffer.append("<div class='panel panel-"+classId+"' id='panel-"+id+"'>\n");
            String elementId = classId+"-"+id;
            /*
            if (showEdit) {
                buffer.append("<div class='editToggle' id='" + id + "' onclick='toggleEditMode(this.id,\"" + classId + "\")'>Edit</div>\n");
            }
            */
            if (StringUtils.isNotBlank(header)) {
                buffer.append("<div class='header header-" + classId + "' id='header-" + elementId + "'>" + header + "</div>\n");
            }
            buffer.append("<div class='content content-" + classId + "' id='content-" + elementId + "'>" + htmlBody + "</div>\n");
            if (StringUtils.isNotBlank(footer)) {
                buffer.append("<div class='footer footer-" + classId + "' id='footer-" + elementId + "'>" + footer + "</div>\n");
            }
            buffer.append("</div>\n");
            buffer.append("\n");

            result = buffer.toString();
        } catch (Exception e) {
            log.error(e,e);
            throw new PluginException(e.getMessage());
        }

		return result;
	}

    protected void validateParams(Properties props, Map<String, String> params) throws PluginException {
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
        paramName = PARAM_CLASSID;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            classId = param;
        }
        paramName = PARAM_HEADER;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            header = param;
        }
        paramName = PARAM_FOOTER;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            footer = param;
        }

        paramName = PARAM_SHOWEDIT;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!param.equalsIgnoreCase("true") && !param.equalsIgnoreCase("false")
                    && !param.equals("0") && !param.equals("1")) {
                throw new PluginException(paramName + " parameter is not a valid boolean");
            }
            showEdit = Boolean.parseBoolean(param);
        }

    }

    public void addUniqueTemplateResourceRequest(WikiContext wikiContext, String resourceType, String resourceName) {
        String pageName = wikiContext.getPage().getName();
        int pageVersion = wikiContext.getPage().getVersion();
        String pageResource = pageName+":"+pageVersion+":"+resourceType+":"+resourceName;
        if (!pageResources.contains(pageResource)) {
            TemplateManager.addResourceRequest(wikiContext, resourceType, resourceName);
            pageResources.add(pageResource);
        }
    }

    private void setLogForDebug(String value) {
        if (StringUtils.isNotBlank(value) && (value.equalsIgnoreCase("true") || value.equals("1"))) {
            log.setLevel(Level.INFO);
        }
    }
}
