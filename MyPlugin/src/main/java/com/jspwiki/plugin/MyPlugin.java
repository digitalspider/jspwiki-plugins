package com.jspwiki.plugin;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

public class MyPlugin implements WikiPlugin {

	private final Logger log = Logger.getLogger(MyPlugin.class);
	
	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
		log.info("plugin executed");
		return "My Plugin rocks";
	}

}
