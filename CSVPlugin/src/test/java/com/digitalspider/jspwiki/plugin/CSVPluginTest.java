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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import au.com.bytecode.opencsv.CSVReader;

@RunWith(MockitoJUnitRunner.class)
public class CSVPluginTest {

	private final Logger log = Logger.getLogger(CSVPluginTest.class);

	private CSVPlugin plugin = new CSVPlugin();
	@Mock private WikiContext wikiContext;
	@Mock private WikiEngine wikiEngine;
	private Map<String,String> params = new HashMap<>();
	
	@Before
	public void setup() {
		BDDMockito.when(wikiContext.getEngine()).thenReturn(wikiEngine);
	}
	
	@Test
	public void testSimple() throws PluginException, IOException {
		log.info("testSimple() START");
		InputStream is=null;
		CSVReader reader=null;
		try {
			is = this.getClass().getResourceAsStream("../../../../test.csv"); // up 4 based on package folders
			reader = new CSVReader(new InputStreamReader(is));
			List<String[]> lines = reader.readAll();
			assertEquals(3,lines.size());
			assertEquals("david",lines.get(1)[0]);
			//plugin.execute(wikiContext, params);
		} finally {
			if (is!=null) { is.close(); }
			if (reader!=null) { reader.close(); }
		}
	}
}