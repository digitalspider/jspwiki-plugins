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

public class EmojiFilterTest extends TestCase {

    public void testEmojiSyntax() {
        String content = "This is a :::bowtie::: emoji with a :::smiley::: face, but not inside {{{ :::noformat::: tags }}}";

        Collection<String> htmlStrings = EmojiFilter.findByRegex(content,EmojiFilter.REGEX_EMOJI);
//        System.out.println("htmlStrings="+htmlStrings);
        assertEquals(2, htmlStrings.size());
        assertTrue(htmlStrings.contains(":::bowtie:::"));
        assertTrue(htmlStrings.contains(":::smiley:::"));
        assertFalse(htmlStrings.contains(":::noformat:::"));

	content = EmojiFilter.replaceEmoji(content,htmlStrings);
	System.out.println("content="+content);
    }

    public void testFindURLInEmbeddedPlugin() {
        String content = "This is a [{ImageGallery url=http://www.embed.com/ items=3 width=900 steps=3 autoplay=2000 speed=200 arrows=true nav=true sortby=name sortdesc=true}] embedded plugin with http://test.com parameters. With {{{ http://noformat.com text }}} inside.";

        Collection<String> htmlStrings = EmojiFilter.findByRegex(content,EmojiFilter.REGEX_HTML);
        //System.out.println("htmlStrings="+htmlStrings);
        assertEquals(1, htmlStrings.size());
        assertTrue(htmlStrings.contains("http://test.com"));
        assertFalse(htmlStrings.contains("http://www.embed.com"));
        assertFalse(htmlStrings.contains("http://noformat.com"));
    }
}
