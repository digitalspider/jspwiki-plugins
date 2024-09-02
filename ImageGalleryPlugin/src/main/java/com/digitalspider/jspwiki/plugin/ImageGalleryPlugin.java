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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.ui.TemplateManager;
import org.apache.wiki.url.URLConstructor;

public class ImageGalleryPlugin implements Plugin {

    private static final Logger log = Logger.getLogger(ImageGalleryPlugin.class);

    // [{ImageGallery url=http://web timeout=1 class=imgGal autoPlay=3000 items=4 lazy=true nav=true suffix=jpg prefix=owl sortby=date sortdesc=true}]
    public static final String REGEX_PLAINTEXT = "^[a-zA-Z0-9_+-]*";
    public static final String REGEX_IMAGE = "src=(\"|')(https?://.*?\\.(?:png|jpg))(\"|')";
    public static final String REGEX_URL = "^(https?|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    private static final String RESOURCE_JSSOR_JS = "jssor/js/jssor.slider.min.js";
    private static final String RESOURCE_JSSOR_CSS = "jssor/css/jssor.slider.css";

    private static final String PARAM_WIDTH = "width";
    private static final String PARAM_HEIGHT = "height";
    private static final String PARAM_STEPS = "steps";
    private static final String PARAM_SPEED = "speed";
    private static final String PARAM_URL = "url";
    private static final String PARAM_TIMEOUT = "timeout";
    private static final String PARAM_CLASS = "class";
    private static final String PARAM_AUTOPLAY = "autoplay";
    private static final String PARAM_ITEMS = "items";
    private static final String PARAM_LAZYLOAD = "lazy";
    private static final String PARAM_NAVIGATION = "nav";
    private static final String PARAM_ARROWS = "arrows";
    private static final String PARAM_SORTBY = "sortby";
    private static final String PARAM_SORTDESC = "sortdesc";
    private static final String PARAM_SUFFIX = "suffix";
    private static final String PARAM_PREFIX = "prefix";

    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 150;
    private static final int DEFAULT_STEPS = 2;
    private static final int DEFAULT_SPEED = 160;
    private static final String DEFAULT_URL = null;
    private static final int DEFAULT_TIMEOUT = 10; // In seconds
    private static final int MAX_TIMEOUT = 120; // In seconds
    private static final String DEFAULT_CLASS = "image-gallery";
    private static final int DEFAULT_AUTOPLAY = 0;
    private static final int DEFAULT_ITEMS = 4;
    private static final boolean DEFAULT_LAZY = true;
    private static final boolean DEFAULT_NAV = true;
    private static final boolean DEFAULT_ARROWS = true;
    private static final String DEFAULT_SUFFIX = null;
    private static final String DEFAULT_PREFIX = null;
    private static final String DEFAULT_SORTBY = null;
    private static final boolean DEFAULT_SORTDESC = false;

    private List<String> pageResources = new ArrayList<String>();

    private String sliderId = "slider" + System.identityHashCode(this);
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private int steps = DEFAULT_STEPS;
    private int speed = DEFAULT_SPEED;
    private String url = DEFAULT_URL;
    private int timeout = DEFAULT_TIMEOUT;
    private String className = DEFAULT_CLASS;
    private int autoPlay = DEFAULT_AUTOPLAY;
    private int items = DEFAULT_ITEMS;
    private boolean lazyLaod = DEFAULT_LAZY;
    private boolean navigation = DEFAULT_NAV;
    private boolean arrows = DEFAULT_ARROWS;
    private String suffix = DEFAULT_SUFFIX;
    private String prefix = DEFAULT_PREFIX;
    private String sortBy = DEFAULT_SORTBY;
    private boolean sortDesc = DEFAULT_SORTDESC;

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        String result = "";

        // Validate all parameters
        validateParams(wikiContext, params);

        log.info("url=" + url + " autoPlay=" + autoPlay + " items=" + items + " suffix=" + suffix + " prefix=" + prefix);

        List<String> imageUrls = new ArrayList<String>();
        try {
            addUniqueTemplateResourceRequest(wikiContext, TemplateManager.RESOURCE_SCRIPT, RESOURCE_JSSOR_JS);
            addUniqueTemplateResourceRequest(wikiContext, TemplateManager.RESOURCE_STYLESHEET, RESOURCE_JSSOR_CSS);
            Page currentPage = wikiContext.getPage();
            if (StringUtils.isNotBlank(url)) {
                String data = readConnection(url, timeout);
                log.trace("data = " + data);
                if (data != null) {
                    imageUrls.addAll(findImages(data));
                    if (imageUrls.isEmpty()) {
                        throw new Exception("No images could be found at " + url);
                    }
                } else {
                    throw new Exception("No data read from URL");
                }
            } else {
                // Load from attachments
                AttachmentManager attachmentManager = wikiContext.getEngine().getManager(AttachmentManager.class);
                List<Attachment> attachments = attachmentManager.listAttachments(currentPage);
                for (Attachment attachment : attachments) {
                    log.info("attachment=" + attachment.getName());

                    String url = wikiContext.getEngine().getManager(URLConstructor.class).makeURL(WikiContext.ATTACH, attachment.getName(), null);
                    log.info("url=" + url);
                    if (url != null && url.endsWith("png") || url.endsWith("jpg")) {
                        imageUrls.add(url);
                    }
                }
            }
            log.info("imageUrls (" + imageUrls.size() + ")=" + imageUrls);
            if (!imageUrls.isEmpty()) {
                imageUrls = applyPrefixSuffix(imageUrls);
                if (sortBy != null && sortBy.equals("name")) {
                    if (sortDesc) {
                        Collections.sort(imageUrls, new ReverseComparator());
                    } else {
                        Collections.sort(imageUrls, String.CASE_INSENSITIVE_ORDER);
                    }
                } else if (sortBy != null && sortBy.equals("date")) {
                    // TODO: Not implemented
                }
                result = getText(wikiContext, imageUrls);
            }
        } catch (Exception e) {
            log.error(e, e);
            throw new PluginException("ERROR in ImageGallery: " + e.getMessage());
        }
        log.info("result=" + result);
        log.info("DONE.");
        return result;
    }

    protected void validateParams(Context wikiContext, Map<String, String> params) throws PluginException {
        String paramName;
        String param;

        log.info("validateParams() START");
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
        paramName = PARAM_STEPS;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a numeric value");
            }
            steps = Integer.parseInt(param);
        }
        paramName = PARAM_SPEED;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a numeric value");
            }
            speed = Integer.parseInt(param);
        }
        paramName = PARAM_URL;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            param = findFirstByRegex(param, REGEX_URL);
            if (param == null) {
                throw new PluginException(paramName + " parameter is not a valid url: " + param);
            }
            url = param;
        }
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
        paramName = PARAM_AUTOPLAY;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a numeric value");
            }
            autoPlay = Integer.parseInt(param);
        }
        paramName = PARAM_ITEMS;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a numeric value");
            }
            items = Integer.parseInt(param);
        }
        paramName = PARAM_LAZYLOAD;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                Boolean paramValue = Boolean.parseBoolean(param);
                lazyLaod = paramValue;
            } catch (Exception e) {
                throw new PluginException(paramName + " parameter is not true or false");
            }
        }
        paramName = PARAM_NAVIGATION;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                Boolean paramValue = Boolean.parseBoolean(param);
                navigation = paramValue;
            } catch (Exception e) {
                throw new PluginException(paramName + " parameter is not true or false");
            }
        }
        paramName = PARAM_ARROWS;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                Boolean paramValue = Boolean.parseBoolean(param);
                arrows = paramValue;
            } catch (Exception e) {
                throw new PluginException(paramName + " parameter is not true or false");
            }
        }
        paramName = PARAM_SUFFIX;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            param = findFirstByRegex(param, REGEX_PLAINTEXT);
            if (param != null) {
                suffix = param;
            }
        }
        paramName = PARAM_PREFIX;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            param = findFirstByRegex(param, REGEX_PLAINTEXT);
            if (param != null) {
                prefix = param;
            }
        }
        paramName = PARAM_SORTBY;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (param.toLowerCase().equals("name") || param.toLowerCase().equals("date")) {
                sortBy = param.toLowerCase();
            } else {
                throw new PluginException(paramName + " parameter can only be true or false");
            }
        }
        paramName = PARAM_SORTDESC;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                Boolean paramValue = Boolean.parseBoolean(param);
                sortDesc = paramValue;
            } catch (Exception e) {
                throw new PluginException(paramName + " parameter is not true or false");
            }
        }
        log.info("validateParams() DONE");
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

    public static String findFirstByRegex(String data, String regex) {
        Collection<String> results = findByRegex(data, regex);
        if (!results.isEmpty()) {
            return results.iterator().next();
        }
        return null;
    }

    public static Collection<String> findImages(String data) {
        Collection<String> results = findByRegex(data, REGEX_IMAGE);
        Collection<String> newResults = new HashSet<String>();
        for (String result : results) {
            if (result != null) {
                // trim src="  and "
                result = result.substring(5);
                result = result.substring(0, result.length() - 1);
                newResults.add(result);
            }
        }
        results = null;
        return newResults;
    }

    public static Collection<String> findByRegex(String data, String regex) {
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

    public List<String> applyPrefixSuffix(List<String> imageUrls) throws Exception {
        List<String> results = new ArrayList<String>();
        boolean applyPrefix = StringUtils.isNotBlank(prefix) ? true : false;
        boolean applySuffix = StringUtils.isNotBlank(suffix) ? true : false;
        if (!applyPrefix && !applySuffix) {
            return imageUrls;
        }
        log.info("imageUrls.size() BEFORE=" + imageUrls.size());
        for (String imageUrl : imageUrls) {
            String imageName = imageUrl;
            log.debug("imageUrl=" + imageUrl);
            int index = StringUtils.lastIndexOf(imageUrl, "/");
            if (index > 0) {
                imageName = imageName.substring(index + 1);
            }
            log.debug("imageName=" + imageName);
            if (applyPrefix && applySuffix) {
                log.debug("prefix=" + prefix + " suffix=" + suffix);
                if (imageName.startsWith(prefix) && imageName.endsWith(suffix)) {
                    log.debug("addingImage=" + imageName);
                    results.add(imageUrl);
                }
            } else if (applyPrefix) {
                log.debug("prefix=" + prefix);
                if (imageName.startsWith(prefix)) {
                    log.debug("addingImage=" + imageName);
                    results.add(imageUrl);
                }
            } else if (applySuffix) {
                log.debug("suffix=" + suffix);
                if (imageName.endsWith(suffix)) {
                    log.debug("addingImage=" + imageName);
                    results.add(imageUrl);
                }
            }
        }
        if (results.isEmpty()) {
            throw new Exception("No images left after applying prefix=" + prefix + " suffix=" + suffix);
        }
        log.info("imageUrls.size() AFTER=" + results.size());
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

    private String getText(Context wikiContext, Collection<String> attachmentUrls) {
        StringBuffer buf = new StringBuffer();
        buf.append(getHtmlForScript());
        buf.append(getHtmlForSlider(attachmentUrls));
        return buf.toString();
    }

    private String getHtmlForScript() {
        StringBuffer template = new StringBuffer();
        template.append(
                "<script>\n"
                + "//<![CDATA[\n"
                + "   jssor_" + sliderId + "_starter = function (containerId) {\n"
                + "		var options = {\n"
                + "			$AutoPlay: " + (autoPlay > 0) + ", $AutoPlaySteps: " + steps + ", $AutoPlayInterval: " + autoPlay + ", $PauseOnHover: 1, $SlideHeight: " + height + ", $SlideWidth: " + new Integer((width / items) - 5).toString() + ", $SlideSpacing: 5, $DisplayPieces: " + items + ", $SlideDuration: " + speed + ", $ParkingPosition: 0\n");
        if (navigation) {
            template.append(
                    "			,$BulletNavigatorOptions: {\n"
                    + "				$Class: $JssorBulletNavigator$, $ChanceToShow: 2, $AutoCenter: 0, $Steps: 1, $Lanes: 1, $SpacingX: 0, $SpacingY: 0, $Orientation: 1\n"
                    + "			}\n");
        }
        if (arrows) {
            template.append(
                    "			,$ArrowNavigatorOptions: {\n"
                    + "				$Class: $JssorArrowNavigator$, $ChanceToShow: 1, $AutoCenter: 2, $Steps: " + steps + "\n"
                    + "			}\n");
        }
        template.append(
                "		};\n"
                + "       var jssor_" + sliderId + " = new $JssorSlider$(containerId, options);\n"
                + "		function ScaleSlider() {\n"
                + "			var bodyWidth = document.body.clientWidth;\n"
                + "			if (bodyWidth)\n"
                + "				jssor_" + sliderId + ".$SetScaleWidth(Math.min(bodyWidth, " + width + "));\n"
                + "			else\n"
                + "				window.setTimeout(ScaleSlider, 30);\n"
                + "		}\n"
                + "		ScaleSlider();\n"
                + "		$JssorUtils$.$AddEvent(window, 'load', ScaleSlider);\n"
                + "		if (!navigator.userAgent.match(/(iPhone|iPod|iPad|BlackBerry|IEMobile)/)) {\n"
                + "			$JssorUtils$.$OnWindowResize(window, ScaleSlider);\n"
                + "		}\n"
                + "   };\n"
                + "//]]>\n"
                + "</script>\n");
        return template.toString();
    }

    private String getHtmlForSlider(Collection<String> attachmentUrls) {
        StringBuffer template = new StringBuffer();
        template.append(
                "<div id='" + sliderId + "' class='" + className + "' style='position: relative; top: 0px; left: 0px; width: " + width + "px; height: " + height + "px;'>\n"
                + "		<!-- Slides Container -->\n"
                + "		<div u='slides' style='cursor: move; position: absolute; overflow: hidden; left: 0px; top: 0px; width: 100%; height: 100%;'>\n");
        for (String attachmentUrl : attachmentUrls) {
            template.append(
                    "			<div><img u='image' src='" + attachmentUrl + "' /></div>\n"
            );
        }
        template.append(
                "		</div>\n");
        if (navigation) {
            template.append(
                    "		<div u='navigator' class='jssorb03' style='position: absolute; bottom: 4px; right: 6px;'>\n"
                    + "           <div u='prototype' style='position: absolute; width: 21px; height: 21px; text-align:center; line-height:21px; color:white; font-size:12px;'><numbertemplate></numbertemplate></div>\n"
                    + "       </div>\n");
        }
        if (arrows) {
            template.append(
                    "       <span u='arrowleft' class='jssora03l' style='width: 55px; height: 55px; top: 123px; left: 8px;'></span>\n"
                    + "       <span u='arrowright' class='jssora03r' style='width: 55px; height: 55px; top: 123px; right: 8px'></span>\n");
        }
        template.append(
                "       <!-- Trigger -->\n"
                + "       <script>jssor_" + sliderId + "_starter('" + sliderId + "'); </script>\n"
                + "</div>\n");
        return template.toString();
    }

    /* Based on owl-carousel
	private String getText(List<String> attachmentUrls) {
		StringBuffer template = new StringBuffer();
		template.append(
			"<link href='owl-carousel/owl.carousel.css' rel='stylesheet'></link> \n"+
			"<link href='owl-carousel/owl.theme.css' rel='stylesheet'></link> \n"+

			"<div id='image-gallery' class='"+className+"'> \n"
		);
		for (String attachmentUrl : attachmentUrls) {
			template.append(
				"	<div class='item'><img class='lazyOwl' data-src='"+attachmentUrl+"' alt='Gallery Image'>&nbsp;</img></div> \n"
			);
		}
		template.append(
			"</div> \n"+
		    "<script src='owl-carousel/jquery-1.11.1.min.js'></script> \n"+
		    "<script src='owl-carousel/owl.carousel.min.js'></script> \n"+

		    "<style> \n"+
		    "//<![CDATA[\n"+
		    "#image-gallery .item{ margin: 3px; } \n"+
		    "#image-gallery .item img{ display: block; width: 100%; height: auto; } \n"+
		    "//]]>\n"+
		    "</style> \n"+

		    "<script> \n"+
		    "//<![CDATA[\n"+
		    "$(document).ready(function() { \n"+
		    "  $('#image-gallery').owlCarousel({ \n");
		if (autoPlay>0) {
		    template.append("  	autoPlay: "+autoPlay+", \n");
		}
		template.append(
		    "    items : "+items+", \n"+
		    "    lazyLoad : "+lazyLaod+", \n"+
		    "    navigation : "+navigation+" \n"+
		    "  }); \n"+
		    "}); \n"+
		    "//]]>\n"+
		    "</script>"
		);
		return template.toString();
	}
     */
    private class ReverseComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return o2.toLowerCase().compareTo(o1.toLowerCase());
        }
    }

    public static void main(String[] args) {
        String url = "http://www.gocomics.com/garfield/2014/08/18";
        //String url = "http://owlgraphic.com/owlcarousel/demos/images.html";
        //String url = "http://www.lasoo.com.au/catalogues.html";
        //String url = "http://www.smh.com.au";
        try {

            ImageGalleryPlugin plugin = new ImageGalleryPlugin();
            System.out.println("url=" + url);
            url = findFirstByRegex(url, REGEX_URL);
            if (url == null) {
                throw new Exception("url " + url + " is not valid");
            }
            String comic = findFirstByRegex("dilbert-classics", REGEX_PLAINTEXT);
            System.out.println("comic=" + comic);
            String data = plugin.readConnection(url, DEFAULT_TIMEOUT);
            //System.out.println("data = "+data);
            if (data != null) {
                String regex = "src=\"(https?:\\/\\/.*\\.(?:png|jpg))\"";
                Collection<String> results = plugin.findByRegex(data, regex);
                //Collection<String> results = plugin.findImages(data);
                System.out.println("results (" + results.size() + ") = " + results);
            }
            List<String> attUrls = new ArrayList<String>();
            attUrls.add("assets/owl1.jpg");
            attUrls.add("assets/owl2.jpg");
            attUrls.add("assets/david.jpg");
            attUrls.add("assets/owl3.png");
            attUrls.add("http://localhost:8080/JSPWiki/attach/ImageGalleryPlugin/owl8.jpg");
            plugin.prefix = "owl";
            plugin.suffix = "jpg";
//			System.out.println(attUrls);
//			attUrls = plugin.applyPrefixSuffix(attUrls);
//			System.out.println(attUrls);

            //System.out.println(plugin.getText(attUrls));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
