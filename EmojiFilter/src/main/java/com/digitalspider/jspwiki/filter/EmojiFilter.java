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
package com.digitalspider.jspwiki.filter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.filters.BasicPageFilter;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.render.XHTMLRenderer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiFilter extends BasicPageFilter {

	private static final Logger log = Logger.getLogger(EmojiFilter.class);

	public static final String REGEX_HTML = "(https?|file)://[-a-zA-Z0-9+&@#/%?='~_|!:,.;]*[-a-zA-Z0-9+&@#/%='~_|]";
	public static final String REGEX_EMOJI = ":::[a-zA-Z0-9+-_!@#$%^&*()]*:::";
	public static final String REGEX_HTML_LINKED = "(\\||\\[)(https?|file)://[-a-zA-Z0-9+&@#/%?='~_|!:,.;]*[-a-zA-Z0-9+&@#/%='~_|]\\]";
	public static final String REGEX_HTML_PLUGIN_BODY= "\\[\\{(.)*\\}\\]";
	public static final String REGEX_HTML_PLUGIN_LINE = "\\[\\{[a-zA-Z0-9+&@#/%?='~_|!:,.; ]*\\}\\]";
	public static final String REGEX_HTML_NOFORMAT = "\\{\\{\\{(.)*\\}\\}\\}";

	public static final String PARAM_CSSCLASS = "cssclass";
	public static final String PARAM_ICONSIZE = "iconsize";
	public static final String PARAM_BASEURL = "baseurl";
	public static final String PARAM_PREFIX = "prefix";
	public static final String PARAM_SUFFIX = "suffix";

	public static final String DEFAULT_CSSCLASS = "emoji";
	public static final int DEFAULT_ICONSIZE = 20;
	public static final String DEFAULT_BASEURL = "http://www.emoji-cheat-sheet.com/graphics/emojis/";
	public static final String DEFAULT_PREFIX = "";
	public static final String DEFAULT_SUFFIX = "png";

	private static String cssclass = DEFAULT_CSSCLASS;
	private static int iconsize = DEFAULT_ICONSIZE;
	private static String baseurl = DEFAULT_BASEURL;
	private static String suffix = DEFAULT_SUFFIX;
	private static String prefix = DEFAULT_PREFIX;

    @Override
    public void initialize(WikiEngine wikiEngine, Properties properties) throws FilterException {
	log.info("initialize");
        super.initialize(wikiEngine,properties);
	if (properties.containsKey(PARAM_ICONSIZE)) {
	    iconsize = Integer.parseInt(properties.getProperty(PARAM_ICONSIZE));
	}
	if (properties.containsKey(PARAM_CSSCLASS)) {
	    cssclass = properties.getProperty(PARAM_CSSCLASS);
	}
	if (properties.containsKey(PARAM_BASEURL)) {
	    baseurl = properties.getProperty(PARAM_BASEURL);
	}
	if (properties.containsKey(PARAM_PREFIX)) {
	    prefix = properties.getProperty(PARAM_PREFIX);
	}
	if (properties.containsKey(PARAM_SUFFIX)) {
	    suffix = properties.getProperty(PARAM_SUFFIX);
	}
    }

    @Override
    public String postTranslate(WikiContext wikiContext, String content) throws FilterException {
	log.info("postTranslate");
        content = super.postTranslate(wikiContext,content);
        //log.info("content="+content);
        Collection<String> htmlStrings = findByRegex(content,REGEX_EMOJI);
        content = EmojiFilter.replaceEmoji(content,htmlStrings);
        return content;
    }

    public static Collection<String> findByRegex(String data, String regex) {
        String patternString = regex;
        log.debug("patternString="+patternString);
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(removePageSpecialContent(data));
        Collection<String> results = new HashSet<String>();
        while (matcher.find()) {
            String result = matcher.group();
            log.debug("Found="+result);
            results.add(result);
        }
        return results;
    }

    public static String removePageSpecialContent(String data) {
	List<String> regexes = new ArrayList<String>();
	regexes.add(REGEX_HTML_NOFORMAT);
	boolean includeBody = true;
	String regex = (includeBody) ? REGEX_HTML_PLUGIN_BODY : REGEX_HTML_PLUGIN_LINE;
	regexes.add(regex);
	return removeRegexContent(data,regexes);
    }

    public static String removeRegexContent(String data, List<String> regexes) {
    	String newData = data;
        for( String regex : regexes ) {
	    Matcher matcher = Pattern.compile(regex).matcher(data);
            while (matcher.find()) {
                String result = matcher.group();
                log.debug("Using regex="+regex+" found="+result);
	        newData = newData.replace(result,"");
            }
	}
	return newData;
    }

    public static String replaceEmoji(String content, Collection<String> emojiStrings) {
        for (String emoji: emojiStrings) {
            content = content.replace(emoji,"<span class='"+cssclass+"'><img src='"+baseurl+prefix+emoji.substring(3,emoji.length()-3)+"."+suffix+"' height="+iconsize+" weigth="+iconsize+" /></span>");
        }
	return content;
    }
}
