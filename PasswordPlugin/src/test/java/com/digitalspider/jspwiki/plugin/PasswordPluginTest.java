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

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.render.XHTMLRenderer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class PasswordPluginTest extends TestCase {

	private final Logger log = Logger.getLogger(PasswordPluginTest.class);

    public void testEncrption() throws Exception {
        char[] key = "pwd".toCharArray();

        String content = "This is another example";
        byte[] encrypted = PasswordPlugin.encrypt(key,content.getBytes());
        System.out.println("encrypted="+encrypted);
        System.out.println("encrypted="+new String(encrypted));

        byte[] cleartext = PasswordPlugin.decrypt(key, encrypted);
        System.out.println("decrypted="+new String(cleartext));
        assertEquals(content,new String(cleartext));

        int level = 3;
        Integer uuid = PasswordPlugin.getPasswordID(level);
        System.out.println("uuid="+uuid);
        assertEquals(level,(uuid%10000)/1000);

    }
}
