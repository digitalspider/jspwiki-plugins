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

import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

public class GoComicsPlugin implements Plugin {

    private final Logger log = Logger.getLogger(GoComicsPlugin.class);

    private static final String PROP_BASEURL = "gocomicplugin.baseurl";
    private static final String PROP_SRCPATH = "gocomicplugin.srcpath";

    private static final String[] COMIC_LIST = ("9chickweedlane adamathome andycapp arloandjanis baldo "
            + "basicinstructions bc bignate brevity brewsterrockit broomhilda calvinandhobbes cathy closetohome "
            + "culdesac darksideofthehorse dicktracy dilbert-classics dinosaur-comics doonesbury drabble "
            + "duplex eric-the-circle fminus forbetterorforworse foxtrot frankandernest frazz garfield "
            + "getfuzzy heartofthecity heavenly-nostrils herman inthebleachers jimsjournal jumpstart "
            + "lacucaracha lastkiss lio luann marmaduke moderately-confused monty nancy nonsequitur offthemark "
            + "overboard peanuts pearlsbeforeswine pibgorn pickles poochcafe realitycheck reallifeadventures "
            + "redandrover richards-poor-almanac roseisrose savage-chickens scarygary shoe speedbump stonesoup "
            + "tankmcnamara thatababy that-is-priceless theargylesweater the-born-loser theflyingmccoys "
            + "thefuscobrothers thegrizzwells thekchronicles tomthedancingbug wizardofid working-daze wumo "
            + "ziggy").split(" ");
    private static final String PARAM_TIMEOUT = "timeout";
    private static final String PARAM_DATE = "date";
    private static final String PARAM_COMIC = "comic";
    private static final String PARAM_CLASS = "class";
    private static final String PARAM_WIDTH = "width";
    private static final String PARAM_HEIGHT = "height";

    private static final int DEFAULT_TIMEOUT = 10; // In seconds
    private static final int MAX_TIMEOUT = 120; // In seconds
    private static final String DEFAULT_SRCPATH = "http://assets.amuniversal.com";
    private static final String DEFAULT_BASEPATH = "http://www.gocomics.com/";
    private static final String DEFAULT_COMIC = "random";
    private static final String DEFAULT_CLASS = "comic";
    private static final int DEFAULT_WIDTH = 600;
    private static final int DEFAULT_HEIGHT = 200;

    private static final String REGEX_URL = "^(https?|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final String REGEX_PLAINTEXT = "^[a-zA-Z0-9_+-]*";

    private static final Date TODAY = new Date();
    private static final SimpleDateFormat DATEFORMAT_OUTPUT = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat DATEFORMAT_INPUT = new SimpleDateFormat("yyyyMMdd");

    private String url = DEFAULT_BASEPATH + DEFAULT_COMIC + "/" + DATEFORMAT_OUTPUT.format(TODAY);
    private String srcPath = DEFAULT_SRCPATH;
    private int timeout = DEFAULT_TIMEOUT;
    private String className = DEFAULT_CLASS;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");

        // Validate all parameters
        validateParams(wikiContext, params);

        log.info("url=" + url + " srcPath=" + srcPath + " timeout=" + timeout);

        String imgPath = "";
        try {
            String data = readConnection(url, timeout);
            log.trace("data = " + data);
            if (data != null) {
                imgPath = findFirstUrlBySrcPath(data, srcPath);
                log.info("imgPath=" + imgPath);
                if (StringUtils.isBlank(imgPath)) {
                    throw new Exception("No image could be found");
                }
            } else {
                throw new Exception("No data read from URL");
            }
        } catch (Exception e) {
            throw new PluginException("ERROR: GoComicsPlugin url=" + url + " ERROR: " + e.getMessage());
        }

        if (StringUtils.isNotBlank(imgPath)) {
            imgPath = "<div class='" + className + "'><img src='" + imgPath + "' alt='" + imgPath + "' width='" + width + "' height='" + height + "'/></div>";
        }
        log.info("imgPath=" + imgPath);
        log.info("DONE.");
        return imgPath;
    }

    protected void validateParams(Context wikiContext, Map<String, String> params) throws PluginException {
        String paramName;
        String param;

        log.info("validateParams() START");
        paramName = PARAM_TIMEOUT;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a numeric value");
            }
            timeout = Integer.parseInt(param);
            if (timeout > MAX_TIMEOUT) {
                timeout = MAX_TIMEOUT;
            }
        }
        paramName = PARAM_WIDTH;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a numeric value");
            }
            width = Integer.parseInt(param);
        }
        paramName = PARAM_HEIGHT;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a numeric value");
            }
            height = Integer.parseInt(param);
        }
        paramName = PARAM_CLASS;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter has invalid characters");
            }
            param = findFirstByRegex(param, REGEX_PLAINTEXT);
            if (param != null) {
                className = param;
            }
        }
        paramName = PARAM_COMIC;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter has invalid characters");
            }
            param = findFirstByRegex(param, REGEX_PLAINTEXT);
            if (param != null) {
                url = url.replace(DEFAULT_COMIC, param);
            }
        }
        if (url.contains(DEFAULT_COMIC)) {
            url = url.replace(DEFAULT_COMIC, getRandomComic());
        }
        paramName = PARAM_DATE;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                Date newDate = DATEFORMAT_INPUT.parse(param);
                url = url.replace(DATEFORMAT_OUTPUT.format(TODAY), DATEFORMAT_OUTPUT.format(newDate));
            } catch (Exception e) {
                throw new PluginException(paramName + " parameter is not of the format " + DATEFORMAT_INPUT.toPattern().toString());
            }
        }
        paramName = PROP_BASEURL;
        param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            param = findFirstByRegex(param, REGEX_URL);
            if (param != null) {
                url = url.replace(DEFAULT_BASEPATH, param);
            }
        }
        paramName = PROP_SRCPATH;
        param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            param = findFirstByRegex(param, REGEX_URL);
            if (param != null) {
                srcPath = param;
            }
        }
        log.info("validateParams() DONE");
    }

    public String findFirstUrlBySrcPath(String data, String srcPath) {
        Collection<String> results = findUrlBySrcPath(data, srcPath);
        if (!results.isEmpty()) {
            return results.iterator().next();
        }
        return null;
    }

    public Collection<String> findUrlBySrcPath(String data, String srcPath) {
        String regex = "\"" + srcPath + ".*?\"";
        Collection<String> results = findByRegex(data, regex);
        Collection<String> newResults = new ArrayList<String>();
        for (String result : results) {
            newResults.add(result.replace("\"", ""));
        }
        return newResults;
    }

    public String findFirstByRegex(String data, String regex) {
        Collection<String> results = findByRegex(data, regex);
        if (!results.isEmpty()) {
            return results.iterator().next();
        }
        return null;
    }

    public Collection<String> findByRegex(String data, String regex) {
        String patternString = regex;
        log.debug("patternString=" + patternString);
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(data);
        Collection<String> results = new HashSet<String>();
        while (matcher.find()) {
            String result = matcher.group();
            log.debug("Found=" + result);
            results.add(result);
        }
        return results;
    }

    public String readConnection(String httpUrl, int timeout) throws Exception {
        URLConnection conn;
        int TIMEOUT = timeout; // in seconds
        if (httpUrl != null && httpUrl.trim().length() > 0) {
            URL url = new URL(httpUrl);
            conn = url.openConnection();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection conn2 = ((HttpURLConnection) conn);
                conn2.setConnectTimeout(TIMEOUT * 1000);
                conn2.setRequestMethod("GET");
                conn2.setRequestProperty("Content-Type", "text/html");
                conn2.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                conn2.setRequestProperty("Accept", "text/html");
                int responseCode = conn2.getResponseCode();
                String responseMessage = conn2.getResponseMessage();
                String contentType = conn2.getContentType();
                log.debug("responseCode=" + responseCode + " responseMessage=" + responseMessage + " contentType=" + contentType);
                if (responseCode == 200 && contentType != null && responseMessage != null) {
                    String encoding = "UTF-8";
                    InputStream inStream = conn2.getInputStream();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(inStream, writer, encoding);
                    String data = writer.toString();
                    return data;
                }
            }
        }
        return null;
    }

    public String getRandomComic() {
        Random rand = new Random();
        int randomNumber = rand.nextInt(COMIC_LIST.length);
        String comic = COMIC_LIST[randomNumber];
        return comic;
    }

    public static void main(String[] args) {
        String url = "http://www.gocomics.com/garfield/2014/08/18";
        String srcPath = "http://assets.amuniversal.com";
        try {
            GoComicsPlugin plugin = new GoComicsPlugin();
            System.out.println(DATEFORMAT_INPUT.toPattern().toString());
            System.out.println("Testing regexes");
            url = plugin.findFirstByRegex(url, REGEX_URL);
            System.out.println("url=" + url);
            srcPath = plugin.findFirstByRegex(srcPath, REGEX_URL);
            System.out.println("srcPath=" + srcPath);
            String comic = plugin.findFirstByRegex("dilbert-classics", REGEX_PLAINTEXT);
            System.out.println("comic=" + comic);
            String data = plugin.readConnection(url, DEFAULT_TIMEOUT);
            // System.out.println("data = "+data);
            if (data != null) {
                Collection<String> results = plugin.findUrlBySrcPath(data, srcPath);
                System.out.println("results (" + results.size() + ") = " + results);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
