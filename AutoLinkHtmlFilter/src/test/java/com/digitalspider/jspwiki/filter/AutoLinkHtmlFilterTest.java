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

import java.util.Collection;

public class AutoLinkHtmlFilterTest extends TestCase {

    public void testFindURL() {
        String content = "This is a http://www.google.com link to a page on [http://digitalspider.com.au] and http://test/abc.com?abc='21'";

        Collection<String> htmlStrings = AutoLinkHtmlFilter.findByRegex(content,AutoLinkHtmlFilter.REGEX_HTML);
//        System.out.println("htmlStrings="+htmlStrings);
        assertEquals(3, htmlStrings.size());
        assertTrue(htmlStrings.contains("http://www.google.com"));
        assertTrue(htmlStrings.contains("http://digitalspider.com.au"));
        assertTrue(htmlStrings.contains("http://test/abc.com?abc='21'"));

        Collection<String> linkedHtmlStrings = AutoLinkHtmlFilter.findByRegex(content,AutoLinkHtmlFilter.LINKED_REGEX_HTML);
//        System.out.println("linkedHtmlStrings="+linkedHtmlStrings);
        assertEquals(1,linkedHtmlStrings.size());
        assertTrue(linkedHtmlStrings.contains("[http://digitalspider.com.au]"));
        linkedHtmlStrings = AutoLinkHtmlFilter.getUnlinkedCollection(linkedHtmlStrings);
        assertTrue(linkedHtmlStrings.contains("http://digitalspider.com.au"));

        htmlStrings = AutoLinkHtmlFilter.removeAll(htmlStrings, linkedHtmlStrings);
        assertEquals(2,htmlStrings.size());

        for (String link: htmlStrings) {
            content = content.replace(link,"["+link+"]");
        }

        htmlStrings = AutoLinkHtmlFilter.findByRegex(content,AutoLinkHtmlFilter.REGEX_HTML);
        assertEquals(3, htmlStrings.size());
        linkedHtmlStrings = AutoLinkHtmlFilter.findByRegex(content,AutoLinkHtmlFilter.LINKED_REGEX_HTML);
        assertEquals(3,linkedHtmlStrings.size());
    }
}