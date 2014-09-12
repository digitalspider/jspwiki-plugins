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
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.plugin.DefaultPluginManager;
import org.apache.wiki.render.XHTMLRenderer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
        setLogForDebug(params.get(PluginManager.PARAM_DEBUG));
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();
        WikiEngine engine = wikiContext.getEngine();
        Properties props = engine.getWikiProperties();

        // Validate all parameters
        validateParams(props, params);

        PageManager pageManager = engine.getPageManager();
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
                List<DefaultPluginManager.WikiPluginInfo> pluginModuleList = new ArrayList<DefaultPluginManager.WikiPluginInfo>();
                pluginModuleList.addAll(pluginModules);
                Collections.sort(pluginModuleList,new WikiPluginInfoComparator());
                for (DefaultPluginManager.WikiPluginInfo info : pluginModuleList) {
                    String name = info.getName();
                    String author = info.getAuthor();
                    buffer.append("| Plugin" + DELIM + getNameLinked(pageManager, name) + getAlias(info.getAlias()) +
                            DELIM + getClassNameLinked(info.getClassName()) + DELIM + getNameLinked(pageManager, author) +
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
                List<PageFilter> filterModuleList = new ArrayList<PageFilter>();
                filterModuleList.addAll(filterModules);
                Collections.sort(filterModuleList,new PageFilterComparator());
                for (PageFilter filter : filterModuleList) {
                    String name = filter.getClass().getSimpleName();
                    buffer.append("| Filter" + DELIM + getNameLinked(pageManager, name) +
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
                List<WikiModuleInfo> editorModuleList = new ArrayList<WikiModuleInfo>();
                editorModuleList.addAll(editorModules);
                Collections.sort(editorModuleList,new WikiModuleInfoComparator());
                for (WikiModuleInfo info : editorModuleList) {
                    String name = info.getName();
                    String author = info.getAuthor();
                    buffer.append("| Editor" + DELIM + getNameLinked(pageManager, name) +
                            DELIM + getClassNameLinked(info.getClass().getName()) + DELIM + getNameLinked(pageManager, author) +
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
            result = engine.textToHTML(wikiContext,buffer.toString());

            result = "<div class='"+className+"'>"+result+"</div>";
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
            }
            else if (param.equalsIgnoreCase(ModuleType.ALL.name())) {
                typeFilter = ModuleType.ALL;
            } else {
                throw new PluginException(paramName + " parameter is not a valid type. " +
                        "Should be all,plugin,filter, or editor. ");
            }
        }
        paramName = PARAM_SHOWSTYLE;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!param.equalsIgnoreCase("true") && !param.equalsIgnoreCase("false")
                    && !param.equals("0") && !param.equals("1")) {
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

    private String getNameLinked(PageManager pageManager, String name) {
        String result = name;
        if (StringUtils.isNotBlank(name)) {
            try {
                if (pageManager.pageExists(name)) {
                    result = "[" + name + "]";
                }
            } catch (Exception e) {
                log.error(e,e);
            }
        }
        return result;
    }

    public class WikiModuleInfoComparator implements Comparator<WikiModuleInfo> {
        public int compare(WikiModuleInfo m1, WikiModuleInfo m2) {
            return m1.getClass().getName().compareTo(m2.getClass().getName());
        }
    }

    public class WikiPluginInfoComparator implements Comparator<DefaultPluginManager.WikiPluginInfo> {
        public int compare(DefaultPluginManager.WikiPluginInfo m1, DefaultPluginManager.WikiPluginInfo m2) {
            return m1.getClassName().compareTo(m2.getClassName());
        }
    }

    public class PageFilterComparator implements Comparator<PageFilter> {
        public int compare(PageFilter pf1, PageFilter pf2) {
            return pf1.getClass().getSimpleName().compareTo(pf2.getClass().getSimpleName());
        }
    }


    private void setLogForDebug(String value) {
        if (StringUtils.isNotBlank(value) && (value.equalsIgnoreCase("true") || value.equals("1"))) {
            log.setLevel(Level.INFO);
        }
    }
}
