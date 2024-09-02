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

import junit.framework.TestCase;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;

import java.util.Properties;

public class EncryptedPageFilterTest extends TestCase {

    public void testPageEncryption() throws Exception {
        String pageText = "david was here";
        EncryptedPageFilter provider = new EncryptedPageFilter();
        Properties properties = new Properties();
        properties.setProperty("jspwiki.pageProvider","com.digitalspider.jspwiki.filter.EncryptedPageFilter");
//        WikiEngine engine = new WikiEngine(properties);
//        WikiPage page = new WikiPage(engine,"Test Page");
//        provider.putPageText(page, pageText);
//
//        String result = provider.getPageText(page.getName(),-1);
//        assertEquals(pageText,result);
    }
}
