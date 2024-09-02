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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.plugin.DefaultPluginManager;
import org.apache.wiki.ui.TemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.render.RenderingManager;

public class PanelPluginPlus implements Plugin {

    private final Logger log = Logger.getLogger(PanelPluginPlus.class);

    public static final String DEFAULT_ID = "1234";
    public static final String DEFAULT_CLASSID = "class";
    public static final Boolean DEFAULT_SHOWEDIT = false;
    public static final String DEFAULT_HEADER = "";
    public static final String DEFAULT_FOOTER = "";

    private static final String PARAM_ID = "id";
    private static final String PARAM_CLASSID = "classid";
    private static final String PARAM_SHOWEDIT = "showedit";
    private static final String PARAM_HEADER = "header";
    private static final String PARAM_FOOTER = "footer";

    private String id = DEFAULT_ID;
    private String classId = DEFAULT_CLASSID;
    private Boolean showEdit = DEFAULT_SHOWEDIT;
    private String header = DEFAULT_HEADER;
    private String footer = DEFAULT_FOOTER;

    private static final String RESOURCE_JSCOLOR_JS = "jscolor/jscolor.js";
    private static final String RESOURCE_PANEL_JS = "panel/panel.js";
    private static final String RESOURCE_PANEL_CSS = "panel/panel.css";
    private List<String> pageResources = new ArrayList<String>();

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        setLogForDebug(params.get(PluginManager.PARAM_DEBUG));
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();
        Engine engine = wikiContext.getEngine();
        Properties props = engine.getWikiProperties();

        addUniqueTemplateResourceRequest(wikiContext, TemplateManager.RESOURCE_SCRIPT, RESOURCE_JSCOLOR_JS);
        addUniqueTemplateResourceRequest(wikiContext, TemplateManager.RESOURCE_SCRIPT, RESOURCE_PANEL_JS);
        addUniqueTemplateResourceRequest(wikiContext, TemplateManager.RESOURCE_STYLESHEET, RESOURCE_PANEL_CSS);

        // Validate all parameters
        validateParams(props, params);

        try {
            String htmlBody = "";
            String body = params.get(DefaultPluginManager.PARAM_BODY);
            if (StringUtils.isNotBlank(body)) {
                htmlBody = engine.getManager(RenderingManager.class).textToHTML(wikiContext, body);
            }
            buffer.append("<div class='panel panel-" + classId + "' id='panel-" + id + "'>\n");
            if (showEdit) {
                buffer.append("<div class='editToggle' id='" + id + "' onclick='toggleEditMode(this.id,\"" + classId + "\")'>Edit</div>\n");
            }
            if (StringUtils.isNotBlank(header)) {
                buffer.append("<div class='header header-" + classId + "' id='header-" + id + "'>" + header + "</div>\n");
            }
            buffer.append("<div class='content content-" + classId + "' id='content-" + id + "'>" + htmlBody + "</div>\n");
            if (StringUtils.isNotBlank(footer)) {
                buffer.append("<div class='footer footer-" + classId + "' id='footer-" + id + "'>" + footer + "</div>\n");
            }
            buffer.append("</div>\n");
            buffer.append("\n");
            buffer.append("<div id='colorSelectDiv' style='display:none;position:relative;width:150px;height:150px;'>\n"
                    + "<div id='elementSelector'>\n"
                    + "Select&nbsp;Element:&nbsp;<span class='code' id='selectId' onclick='selectChangeElement(this.id)'>ID</span>&nbsp;|&nbsp;<span class='code' id='selectClass' onclick='selectChangeElement(this.id)'>CLASS</span>&nbsp;|&nbsp;<span class='code' id='selectBody' onclick='selectChangeElement(this.id)'>BODY</span>&nbsp;|&nbsp;<span class='code' onclick='closeColorMap()'>CLOSE</span>\n"
                    + "</div>\n"
                    + "<div id='styleSelectorColor' style='display:none'>\n"
                    + "Select&nbsp;Color:&nbsp;<span class='code' id='selectTextColor' onclick='selectStyleColor(this.id)'>Text</span>&nbsp;|&nbsp;<span class='code' id='selectBackgroundColor' onclick='selectStyleColor(this.id)'>Background</span>&nbsp;|&nbsp;<span class='code' id='selectBorderColor' onclick='selectStyleColor(this.id)'>Border</span>\n"
                    + "</div>\n"
                    + "<div id='styleSelector' style='display:none'>\n"
                    + "Select&nbsp;Style:&nbsp;<span class='code' id='selectFont' onclick='selectStyle(this.id)'>Font</span>&nbsp;|&nbsp;<span class='code' id='selectFontSize' onclick='selectStyle(this.id)'>Font Size</span>&nbsp;|&nbsp;<span class='code' id='selectBorder' onclick='selectStyle(this.id)'>Border</span>&nbsp;|&nbsp;<span class='code' id='selectCorners' onclick='selectStyle(this.id)'>Corners</span><br/>\n"
                    + "<span class='code' id='selectPadding' onclick='selectStyle(this.id)'>Padding</span>&nbsp;|&nbsp;<span class='code' id='selectMargin' onclick='selectStyle(this.id)'>Margin</span>|&nbsp;<span class='code' id='selectMinWidth' onclick='selectStyle(this.id)'>MinWidth</span>&nbsp;|&nbsp;<span class='code' id='selectMinHeight' onclick='selectStyle(this.id)'>MinHeight</span>&nbsp;|&nbsp;<span class='code' id='selectScroll' onclick='selectStyle(this.id)'>Scroll</span>&nbsp;|&nbsp;<span class='code' id='selectCustom' onclick='selectStyle(this.id)'>Custom</span>\n"
                    + "</div>\n"
                    + "<input class='color' id='colorInput' onchange='alterColor(\"#\"+this.color)' style='display:none'></input>\n"
                    + "<input class='styleInput' id='styleInput' onchange='alterStyle(this.value)' style='display:none'></input>\n"
                    + "<textarea class='customInput' id='customInput' onchange='alterStyle(this.value)' style='display:none'></textarea>\n"
                    + "</div>\n");
            buffer.append("\n");

            result = buffer.toString();
        } catch (Exception e) {
            log.error(e, e);
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

    public void addUniqueTemplateResourceRequest(Context wikiContext, String resourceType, String resourceName) {
        String pageName = wikiContext.getPage().getName();
        int pageVersion = wikiContext.getPage().getVersion();
        String pageResource = pageName + ":" + pageVersion + ":" + resourceType + ":" + resourceName;
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
