package com.jspwiki.plugin;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

public class HitCountPlugin implements WikiPlugin {

	private final Logger log = Logger.getLogger(HitCountPlugin.class);
	private static final String KEY_PAGEHITCOUNT = "@pageHitCount";
	
	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
		log.info("STARTED");
		int pageHitCount = 0;
		try {
			WikiPage currentPage = wikiContext.getPage();
			log.info("currentPage="+currentPage);
			Object pageHitCountAtt = currentPage.getAttribute(KEY_PAGEHITCOUNT);
			Integer hitCount = 0;
			if (pageHitCountAtt != null) {
				hitCount = Integer.parseInt(pageHitCountAtt.toString());
			}
			hitCount++;
			pageHitCount = hitCount;
			currentPage.setAttribute(KEY_PAGEHITCOUNT,hitCount);
		} catch (Exception e) {
			log.error(e,e);
		}
	
		log.info("DONE. pageHitCount="+pageHitCount);
		return ""+pageHitCount;
	}

}
