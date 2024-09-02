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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

public class VideoPlugin implements Plugin {

	private final Logger log = Logger.getLogger(VideoPlugin.class);

	// [{YouTube http://youtu.be/v0umeprpr34}]
	// [{YouTube https://www.youtube.com/watch?v=rYEDA3JcQqw}]

	private static final String BASEURL = "http://youtube.com/embed/";

	private static final String PARAM_URL = "url";
	private static final String PARAM_WIDTH = "width";
	private static final String PARAM_HEIGHT = "height";

	private static final String DEFAULT_URL = null;
	private static final int DEFAULT_WIDTH = 560;
	private static final int DEFAULT_HEIGHT = 315;

	private String url = DEFAULT_URL;
	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;

	public static final String REGEX_YOUTUBE="http(s)?://(?:www\\.)?(?:youtube\\.com|youtu\\.be)\\/(?:watch\\?v=)?(.+)";

	@Override
	public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
		log.info("STARTED");
		String result = "";

		// Validate all parameters
		validateParams(wikiContext,params);

		log.info("url="+url+" width="+width+" height="+height);

		try {
			result = "<iframe width='"+width+"' height='"+height+"' src='"+url+"' frameborder='0' allowfullscreen></iframe>";
		} catch (Exception e) {
			log.error(e,e);
			throw new PluginException("ERROR in ImageGallery: "+e.getMessage());
		}
		log.info("result="+result);
		log.info("DONE.");
		return result;
	}

	protected void validateParams(Context wikiContext, Map<String, String> params) throws PluginException {
		String paramName;
		String param;

		log.info("validateParams() START");
		paramName = PARAM_URL;
		param = params.get(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			param = findFirstByRegex(param, REGEX_YOUTUBE);
			if (StringUtils.isBlank(param)) {
				throw new PluginException(paramName+" parameter is not a valid youtube video. URL="+params.get(paramName));
			}
			url = BASEURL+getYouTubeId(param)+"?rel=0";
		}
		paramName = PARAM_WIDTH;
		param = params.get(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isNumeric(param)) {
				throw new PluginException(paramName+" parameter is not a numeric value");
			}
			width = Integer.parseInt(param);
		}
		paramName = PARAM_HEIGHT;
		param = params.get(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isNumeric(param)) {
				throw new PluginException(paramName+" parameter is not a numeric value");
			}
			height = Integer.parseInt(param);
		}
	}

	public static String findFirstByRegex(String data, String regex) {
		Collection<String> results = findByRegex(data, regex);
		if (!results.isEmpty()) {
			return results.iterator().next();
		}
		return null;
	}

	public static Collection<String> findByRegex(String data, String regex) {
		String patternString = regex;
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(data);
		Collection<String> results = new HashSet<String>();
		while (matcher.find()) {
			String result = matcher.group();
			results.add(result);
		}
		return results;
	}

	public static String getYouTubeId(String url) {
		String id = "";
		if (url.contains("v=")) {
			int index = url.indexOf("v=");
			if (index>0 ) {
				id = url.substring(index+2);
				if (id.contains("&")) {
					index = id.indexOf("&");
					if (index>0 ) {
						id = id.substring(0,index);
					}
				}
			}
		} else {
			int index = StringUtils.lastIndexOf(url, "/");
			if (index>0) {
				id = url.substring(index+1);
			}
		}
		return id;
	}

	public static void main(String[] args) {
		String url1 = "https://www.youtube.com/watch?v=rYEDA3JcQqw";
		String url2 = "http://youtu.be/v0umeprpr34";
		String url3 = "http://youtu.be/rYEDA3JcQqw";
		String url4 = "https://www.youtube.com/watch?v=v0umeprpr34&feature=youtu.be";
		String url5 = "https://www.youtube.com/watch?v=rYEDA3JcQqw";
		String url = url5;
		System.out.println("url="+url);
		url = findFirstByRegex(url, REGEX_YOUTUBE);
		System.out.println("url="+url);
		String id = getYouTubeId(url);
		System.out.println("id="+id);
	}
}
