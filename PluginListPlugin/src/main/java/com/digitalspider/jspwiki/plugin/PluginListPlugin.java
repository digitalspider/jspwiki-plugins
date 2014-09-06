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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.plugin.DefaultPluginManager;
import org.apache.wiki.render.XHTMLRenderer;

public class PluginListPlugin implements WikiPlugin {

	private final Logger log = Logger.getLogger(PluginListPlugin.class);

    public enum ModuleType {
        ALL, PLUGIN, FILTER, EDITOR
    }

    public static final String DEFAULT_CLASS = "plugin-list";
    public static final ModuleType DEFAULT_TYPE = ModuleType.ALL;
    public static final Boolean DEFAULT_SHOWSTYLE = false;

    private static final String PARAM_CLASS = "class";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_SHOWSTYLE = "showstyle";

    private String className = DEFAULT_CLASS;
    private ModuleType typeFilter = DEFAULT_TYPE;
    private Boolean showStyle = DEFAULT_SHOWSTYLE;

    private static final String DELIM = " | ";
	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();

        // Validate all parameters
        validateParams(wikiContext, params);

        WikiEngine engine = wikiContext.getEngine();
        Collection<DefaultPluginManager.WikiPluginInfo> pluginModules = engine.getPluginManager().modules();
        Collection<PageFilter> filterModules = engine.getFilterManager().modules();
        Collection<WikiModuleInfo> pageModules = engine.getPageManager().modules(); // null
        Collection<WikiModuleInfo> templateModules = engine.getTemplateManager().modules(); // empty list
        Collection<WikiModuleInfo> editorModules = engine.getEditorManager().modules();

        try {
            buffer.append("|| Module Type || Name (Alias) || Class || Author || Min-Max");
            if (showStyle) {
                buffer.append(" || Script/Stylesheet");
            }
            buffer.append("\n");
            String baseUrl = engine.getBaseURL();
            if (typeFilter == ModuleType.ALL || typeFilter == ModuleType.PLUGIN) {
                for (DefaultPluginManager.WikiPluginInfo info : pluginModules) {
                    buffer.append("| Plugin" + DELIM + info.getName() + getAlias(info.getAlias()) +
                            DELIM + getClassNameLinked(info.getClassName()) + DELIM + info.getAuthor() +
                            DELIM + info.getMinVersion() + "-" + info.getMaxVersion());
                    if (showStyle) {
                        buffer.append(
                                DELIM + getResourceLinked(baseUrl, info.getScriptLocation()) +
                                        getResourceLinked(baseUrl, info.getStylesheetLocation()));
                    }
                    buffer.append("\n");
                }
            }
            if (typeFilter == ModuleType.ALL || typeFilter == ModuleType.FILTER) {
                for (PageFilter filter : filterModules) {
                    buffer.append("| Filter" + DELIM + filter.getClass().getSimpleName() +
                            DELIM + getClassNameLinked(filter.getClass().getName()) + DELIM + "" +
                            DELIM + "" + "-" + "");
                    if (showStyle) {
                        buffer.append(
                                DELIM + "");
                    }

                    buffer.append("\n");
                }
            }

            if (typeFilter == ModuleType.ALL || typeFilter == ModuleType.EDITOR) {
                for (WikiModuleInfo info : editorModules) {
                    buffer.append("| Editor" + DELIM + info.getName() +
                            DELIM + getClassNameLinked(info.getClass().getName()) + DELIM + info.getAuthor() +
                            DELIM + info.getMinVersion() + "-" + info.getMaxVersion());
                    if (showStyle) {
                        buffer.append(
                                DELIM + getResourceLinked(baseUrl, info.getScriptLocation()) +
                                        getResourceLinked(baseUrl, info.getStylesheetLocation()));
                    }
                    buffer.append("\n");
                }
            }

            log.info("result="+buffer.toString());
            Reader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes())));
            JSPWikiMarkupParser parser = new JSPWikiMarkupParser(wikiContext, in);
            WikiDocument doc = parser.parse();
            log.debug("doc=" + doc);
            XHTMLRenderer renderer = new XHTMLRenderer(wikiContext, doc);
            result = renderer.getString();

            result = "<div class='"+className+"'>"+result+"</div>";
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
        paramName = PARAM_CLASS;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            className = param;
        }
        paramName = PARAM_TYPE;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            if (param.equalsIgnoreCase(ModuleType.PLUGIN.name())) {
                typeFilter = ModuleType.PLUGIN;
            }
            else if (param.equalsIgnoreCase(ModuleType.FILTER.name())) {
                typeFilter = ModuleType.FILTER;
            }
            else if (param.equalsIgnoreCase(ModuleType.EDITOR.name())) {
                typeFilter = ModuleType.EDITOR;
            } else {
                throw new PluginException(paramName + " parameter is not a valid type. " +
                        "Should be plugin,filter, or editor. ");
            }
        }
        paramName = PARAM_SHOWSTYLE;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!param.equalsIgnoreCase("true") && !param.equalsIgnoreCase("false")) {
                throw new PluginException(paramName + " parameter is not a valid boolean");
            }
            showStyle = Boolean.parseBoolean(param);
        }
    }

    private String getAlias(String alias) {
        String result = "";
        if (StringUtils.isNotBlank(alias)) {
            result = " (" + alias + ")";
        }
        return result;
    }

    private String getClassNameLinked(String className) {
        String result = className;
        if (StringUtils.isNotBlank(className) && className.startsWith("org.apache.wiki")) {
            String pathName = className.replace(".","/");
            if (pathName.contains("$")) {
                int index = pathName.indexOf("$");
                pathName = pathName.substring(0,index);
            }
            result = "["+className+"|http://jspwiki.apache.org/apidocs/2.10.1/"+pathName+".html]";
        }
        return result;
    }

    private String getResourceLinked(String baseUrl, String resourcePath) {
        String result = "";
        if (StringUtils.isNotBlank(resourcePath)) {
            result = "[" + resourcePath + "|"+baseUrl+"/"+resourcePath+"]";
        }
        return result;

    }


}