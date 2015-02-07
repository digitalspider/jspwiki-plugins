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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoLinkHtmlFilter extends BasicPageFilter {

	private static final Logger log = Logger.getLogger(AutoLinkHtmlFilter.class);

    public static final String REGEX_HTML = "(https?|file)://[-a-zA-Z0-9+&@#/%?='~_|!:,.;]*[-a-zA-Z0-9+&@#/%='~_|]";
    public static final String REGEX_HTML_LINKED = "(\\||\\[)(https?|file)://[-a-zA-Z0-9+&@#/%?='~_|!:,.;]*[-a-zA-Z0-9+&@#/%='~_|]\\]";
    public static final String REGEX_HTML_PLUGIN_BODY= "\\[\\{(.|\n)*?\\}\\]";
    public static final String REGEX_HTML_PLUGIN_LINE = "\\[\\{[a-zA-Z0-9+&@#/%?='~_|!:,.; ]*\\}\\]";
    public static final String REGEX_HTML_NOFORMAT = "\\{\\{\\{(.|\n)*?\\}\\}\\}";

    @Override
    public String preSave(WikiContext wikiContext, String content) throws FilterException {
        content = super.preSave(wikiContext,content);
        log.info("content="+content);
        Collection<String> htmlStrings = findByRegex(content,REGEX_HTML);
        Collection<String> linkedHtmlStrings = findByRegex(content,REGEX_HTML_LINKED);
        linkedHtmlStrings = AutoLinkHtmlFilter.getUnlinkedCollection(linkedHtmlStrings);
        htmlStrings = AutoLinkHtmlFilter.removeAll(htmlStrings, linkedHtmlStrings);

        for (String link: htmlStrings) {
            content = content.replace(link,"["+link+"]");
        }
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

    public static Collection<String> getUnlinkedCollection(Collection<String> collection) {
        Collection<String> results = new ArrayList<String>();
        for (String link : collection) {
            link = link.substring(1, link.length() - 1);
            results.add(link);
        }
        return results;
    }

    public static Collection removeAll(final Collection a, final Collection b) {
        ArrayList list = new ArrayList( a );
        for (Iterator it = b.iterator(); it.hasNext();) {
            list.remove(it.next());
        }
        return list;
    }
}
