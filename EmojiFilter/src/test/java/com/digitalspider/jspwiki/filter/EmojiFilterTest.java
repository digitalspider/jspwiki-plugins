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
        String content = "This is a :::bowtie::: emoji with a :::smiley::: face, but not inside <pre> :::noformat::: \n tags </pre>\n"
        		+ "However :::outsidepre::: has another <pre>:::insidepre:::</pre>";

        Collection<String> htmlStrings = EmojiFilter.findByRegex(content,EmojiFilter.REGEX_EMOJI);
//        System.out.println("htmlStrings="+htmlStrings);
        assertEquals(3, htmlStrings.size());
        assertTrue(htmlStrings.contains(":::bowtie:::"));
        assertTrue(htmlStrings.contains(":::smiley:::"));
        assertFalse(htmlStrings.contains(":::noformat:::"));
        assertTrue(htmlStrings.contains(":::outsidepre:::"));
        assertFalse(htmlStrings.contains(":::insidepre:::"));

		content = EmojiFilter.replaceEmoji(content,htmlStrings);
		String expectedContent = 
				"This is a <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/bowtie.png' height=20 weigth=20 /></span> "
				+ "emoji with a "
				+ "<span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/smiley.png' height=20 weigth=20 /></span> "
				+ "face, but not inside <pre> :::noformat::: \n"
				+ " tags </pre>\n"
				+ "However <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/outsidepre.png' height=20 weigth=20 /></span> has another <pre>:::insidepre:::</pre>";
		System.out.println("content="+content);
		assertEquals(expectedContent, content);
    }

    public void testEmojiSyntaxSecondTime() {
		String content = 
				"This is a <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/bowtie.png' height=20 weigth=20 /></span> "
				+ "emoji with a :::smiley::: face, but not inside <pre> :::noformat::: \n"
				+ " tags </pre>\n"
				+ "However <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/outsidepre.png' height=20 weigth=20 /></span> has another <pre>:::insidepre:::</pre>";

        Collection<String> htmlStrings = EmojiFilter.findByRegex(content,EmojiFilter.REGEX_EMOJI);
//        System.out.println("htmlStrings="+htmlStrings);
        assertEquals(1, htmlStrings.size());
        assertFalse(htmlStrings.contains(":::bowtie:::"));
        assertTrue(htmlStrings.contains(":::smiley:::"));
        assertFalse(htmlStrings.contains(":::noformat:::"));
        assertFalse(htmlStrings.contains(":::outsidepre:::"));
        assertFalse(htmlStrings.contains(":::insidepre:::"));

		content = EmojiFilter.replaceEmoji(content,htmlStrings);
		String expectedContent = 
				"This is a <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/bowtie.png' height=20 weigth=20 /></span> "
				+ "emoji with a "
				+ "<span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/smiley.png' height=20 weigth=20 /></span> "
				+ "face, but not inside <pre> :::noformat::: \n"
				+ " tags </pre>\n"
				+ "However <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/outsidepre.png' height=20 weigth=20 /></span> has another <pre>:::insidepre:::</pre>";
		assertEquals(expectedContent, content);
    }
}
