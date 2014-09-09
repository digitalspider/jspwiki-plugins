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

import org.apache.log4j.Logger;

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

        Integer level = 3;
        Integer passwordId = PasswordPlugin.getPasswordID(level);
        System.out.println("passwordId="+passwordId);
        assertEquals(level,PasswordPlugin.getPasswordLevel(passwordId));

    }

    public void testSecrets() throws Exception {
        String plain = "This is my secret";
        Integer pid1 = PasswordPlugin.doLock("1",plain.getBytes(),"david","daniela","vittor");
        System.out.println("pid1="+pid1);
        // level1 unlock
        assertEquals(plain,PasswordPlugin.doUnlock(pid1,"david"));
        assertEquals(plain,PasswordPlugin.doUnlock(pid1,"david","daniela","vittor"));
        assertEquals(plain,PasswordPlugin.doUnlock(pid1,"david","daxniela","vittor"));
        assertNull(PasswordPlugin.doUnlock(pid1,"daxvid","daniela","vittor"));

        Integer pid2 = PasswordPlugin.doLock("2",plain.getBytes(),"david","daniela","vittor");
        System.out.println("pid2="+pid2);
        assertNotSame(plain,pid2);
        assertNotSame(pid1,pid2);
        // level2 unlock
        assertNull(PasswordPlugin.doUnlock(pid2, "david"));
        assertEquals(plain,PasswordPlugin.doUnlock(pid2,"david","daniela","vittor"));
        assertNull(PasswordPlugin.doUnlock(pid2, "daxvid", "daniela", "vittor"));
        assertNull(PasswordPlugin.doUnlock(pid2, "david", "danxiela", "vittor"));
        assertEquals(plain,PasswordPlugin.doUnlock(pid2,"david","daniela","vitxtor"));

        Integer pid3 = PasswordPlugin.doLock("3",plain.getBytes(),"david","daniela","vittor");
        System.out.println("pid3="+pid3);
        assertNotSame(plain,pid3);
        assertNotSame(pid1,pid3);
        assertNotSame(pid2,pid3);
        // level3 unlock
        assertNull(PasswordPlugin.doUnlock(pid3, "david"));
        assertNull(PasswordPlugin.doUnlock(pid3, "david", "daniela"));
        assertEquals(plain, PasswordPlugin.doUnlock(pid3, "david", "daniela", "vittor"));
        assertNull(PasswordPlugin.doUnlock(pid3, "daxvid", "daniela", "vittor"));
        assertNull(PasswordPlugin.doUnlock(pid3, "david", "danxiela", "vittor"));
        assertNull(PasswordPlugin.doUnlock(pid3, "david", "daniela", "vitxtor"));
        assertNull(PasswordPlugin.doUnlock(pid3,"david","daniela","vitxtor","test"));
        assertEquals(plain, PasswordPlugin.doUnlock(pid3, "david", "daniela", "vittor", "texst"));
    }
}
