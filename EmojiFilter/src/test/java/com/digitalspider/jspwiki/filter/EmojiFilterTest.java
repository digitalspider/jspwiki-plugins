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
        String content = "This is a :::bowtie::: emoji with a :::smiley::: face, but not inside <pre> :::noformat::: \n tags </pre>";

        Collection<String> htmlStrings = EmojiFilter.findByRegex(content,EmojiFilter.REGEX_EMOJI);
//        System.out.println("htmlStrings="+htmlStrings);
        assertEquals(2, htmlStrings.size());
        assertTrue(htmlStrings.contains(":::bowtie:::"));
        assertTrue(htmlStrings.contains(":::smiley:::"));
        assertFalse(htmlStrings.contains(":::noformat:::"));

	content = EmojiFilter.replaceEmoji(content,htmlStrings);
	System.out.println("content="+content);
    }

}
