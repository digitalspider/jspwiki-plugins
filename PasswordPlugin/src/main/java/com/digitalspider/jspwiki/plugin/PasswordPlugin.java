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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class PasswordPlugin implements WikiPlugin {

	private static final Logger log = Logger.getLogger(PasswordPlugin.class);

    private static final String DEFAULT_ID = null;
    private static final String DEFAULT_PASSWORD = null;
    private static final Integer DEFAULT_LEVEL = 1;

    private static final String PARAM_ID = "id";
    private static final String PARAM_PASSWORD = "p";
    private static final String PARAM_LEVEL = "l";

    private String id = DEFAULT_ID;
    private String password = DEFAULT_PASSWORD;
    private Integer level = DEFAULT_LEVEL;

	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();

        // Validate all parameters
        validateParams(wikiContext, params);

        WikiEngine engine = wikiContext.getEngine();
        PageManager pageManager = engine.getPageManager();
        String baseUrl = engine.getBaseURL();

        try {

        } catch (Exception e) {
            log.error(e,e);
            throw new PluginException(e.getMessage());
        }

		return result;
	}

    protected void validateParams(WikiContext wikiContext, Map<String, String> params) throws PluginException {
        String paramName;
        String param;

        log.info("validateParams() START");
        paramName = PARAM_ID;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            id = param;
        }
    }

    private String getPropKey(String currentKey, String source) {
        String result = currentKey;
        if (StringUtils.isNotBlank(source)) {
            result+="."+source;
        }
        return result;
    }

    private static Cipher getCipher(char[] key, int mode) throws PluginException {
        // TODO: Document - http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html
        try {
            String transformation = "PBEWithMD5AndDES";
            byte[] salt = "protects".substring(0,8).getBytes();
            int count = 20;

            // Create PBE parameter set
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(key);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance(transformation);
            SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

            Cipher pbeCipher = Cipher.getInstance(transformation);
            pbeCipher.init(mode, pbeKey, pbeParamSpec);
            return pbeCipher;
        } catch (Exception e) {
            log.error(e,e);
            throw new PluginException("Error encrypting password. "+e.getMessage());
        }
    }

    public static byte[] encrypt(char[] key, byte[] content) throws PluginException {
        try {
            Cipher pbeCipher = getCipher(key, Cipher.ENCRYPT_MODE);
            byte[] encrypted = pbeCipher.doFinal(content);
            encrypted = Base64.encodeBase64(encrypted);
            return encrypted;
        } catch (Exception e) {
            log.error(e,e);
            throw new PluginException("Error encrypting password. "+e.getMessage());
        }
    }

    public static byte[] decrypt(char[] key, byte[] content) throws PluginException {
        try {
            Cipher pbeCipher = getCipher(key, Cipher.DECRYPT_MODE);
            content = Base64.decodeBase64(content);
            content = pbeCipher.doFinal(content);
            return content;
        } catch (Exception e) {
            log.error(e,e);
            throw new PluginException("Error decrypting password. "+e.getMessage());
        }
    }

    public static Integer getPasswordID(Integer level) throws PluginException {
        if (level>9) {
            throw new PluginException("Level greater than 9 is unsupported");
        }
        if (level<1) {
            throw new PluginException("Level less than 1 is invalid");
        }
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString().replaceAll("[a-zA-Z]|-","").substring(0,5);
        id = id.substring(0,2)+level+id.substring(2);
        return Integer.parseInt(id);
    }
}
