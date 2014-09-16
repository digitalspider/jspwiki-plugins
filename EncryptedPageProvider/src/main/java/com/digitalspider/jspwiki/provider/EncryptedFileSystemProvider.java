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
package com.digitalspider.jspwiki.provider;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.providers.FileSystemProvider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class EncryptedFileSystemProvider extends FileSystemProvider {

	private static final Logger log = Logger.getLogger(EncryptedFileSystemProvider.class);

    public static final String PROP_ENCRYPT_KEY = "encrypt.provider.key";
    public static final String PROP_ENCRYPT_PREFIX = "encrypt.provider.prefix";
    public static final String PROP_ENCRYPT_SALT = "encrypt.provider.salt";
    public static final String PROP_ENCRYPT_BLOCKSIZE = "encrypt.provider.blocksize";
    public static final String PROP_ENCRYPT_ITERACTIONCOUNT = "encrypt.provider.itrcount";
    public static final String PROP_ENCRYPT_ALGORITHM = "encrypt.provider.algorithm";

    char[] key = new char[0];
    String prefix = "JSPWiki:";
    String salt = "Ra%$ESSQA#!@)#$@)";
    int blockSize = 8;
    int iteractionCount = 2048;
    String algorithm = "PBEWithMD5AndDES";

    @Override
    public void initialize(WikiEngine engine, Properties properties) throws NoRequiredPropertyException, IOException, FileNotFoundException {
        super.initialize(engine, properties);
        if (StringUtils.isNotEmpty(properties.getProperty(PROP_ENCRYPT_KEY))) {
            char[] key = engine.getWikiProperties().getProperty(PROP_ENCRYPT_KEY).toCharArray();
        } else {
            throw new NoRequiredPropertyException("The encryption key property "+PROP_ENCRYPT_KEY+" is required!", PROP_ENCRYPT_KEY);
        }
        if (StringUtils.isNotEmpty(properties.getProperty(PROP_ENCRYPT_PREFIX))) {
            prefix = engine.getWikiProperties().getProperty(PROP_ENCRYPT_PREFIX);
        }
        if (StringUtils.isNotEmpty(properties.getProperty(PROP_ENCRYPT_SALT))) {
            salt = engine.getWikiProperties().getProperty(PROP_ENCRYPT_SALT);
        }
        if (StringUtils.isNotEmpty(properties.getProperty(PROP_ENCRYPT_BLOCKSIZE))) {
            String blockSizeString = engine.getWikiProperties().getProperty(PROP_ENCRYPT_BLOCKSIZE);
            try {
                blockSize = Integer.parseInt(blockSizeString);
            } catch (Exception e) {
                log.error("Invalid parameter "+PROP_ENCRYPT_BLOCKSIZE+"="+blockSizeString+" is not an integer");
            }
        }
        if (StringUtils.isNotEmpty(properties.getProperty(PROP_ENCRYPT_ITERACTIONCOUNT))) {
            String itrCountString = engine.getWikiProperties().getProperty(PROP_ENCRYPT_ITERACTIONCOUNT);
            try {
                iteractionCount = Integer.parseInt(itrCountString);
            } catch (Exception e) {
                log.error("Invalid parameter "+PROP_ENCRYPT_ITERACTIONCOUNT+"="+itrCountString+" is not an integer");
            }
        }
        if (StringUtils.isNotEmpty(properties.getProperty(PROP_ENCRYPT_ALGORITHM))) {
            algorithm = engine.getWikiProperties().getProperty(PROP_ENCRYPT_ALGORITHM);
        }
    }

    @Override
    public String getPageText(String page, int version) throws ProviderException {
        String result = super.getPageText(page, version);
        try {
            if (result.endsWith(salt)) {
                result = result.substring(0,result.length()-salt.length());
                String plain = new String(decrypt(key, result.getBytes()));
                if (plain.startsWith(prefix)) {
                    result = plain.substring(prefix.length());
                }
            }

        } catch (Exception e) {
            throw new ProviderException("Error decrypting content. ERROR="+e);
        }
        return result;
    }

    @Override
    public void putPageText(WikiPage page, String text) throws ProviderException {
        try {
            text = prefix + text;
            text = new String(encrypt(key, text.getBytes()));
            text += salt;
        } catch (Exception e) {
            throw new ProviderException("Error encrypting content. ERROR="+e);
        }
        super.putPageText(page, text);
    }

    private Cipher getCipher(char[] key, int mode) throws Exception {
        // TODO: Document - http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html
        String transformation = algorithm;
        if (blockSize > salt.length()) {
            throw new ProviderException("The block size specified is longer then the salt length");
        }
        byte[] saltBytes = salt.substring(0,blockSize).getBytes();
        int count = iteractionCount;

        // Create PBE parameter set
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(saltBytes, count);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(key);
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance(transformation);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        Cipher pbeCipher = Cipher.getInstance(transformation);
        pbeCipher.init(mode, pbeKey, pbeParamSpec);
        return pbeCipher;
    }

    private byte[] encrypt(char[] key, byte[] content) throws Exception {
        Cipher pbeCipher = getCipher(key, Cipher.ENCRYPT_MODE);
        byte[] encrypted = pbeCipher.doFinal(content);
        encrypted = Base64.encodeBase64(encrypted);
        return encrypted;
    }

    private byte[] decrypt(char[] key, byte[] content) throws Exception {
        Cipher pbeCipher = getCipher(key, Cipher.DECRYPT_MODE);
        content = Base64.decodeBase64(content);
        content = pbeCipher.doFinal(content);
        return content;
    }

}
