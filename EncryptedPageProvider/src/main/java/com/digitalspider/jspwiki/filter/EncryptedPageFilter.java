/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package com.digitalspider.jspwiki.filter;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.crypto.CryptoProvider;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.spi.FileSystemProvider;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.filters.BasePageFilter;
import org.apache.wiki.api.filters.PageFilter;

/**
 * This class uses a {@link org.apache.wiki.crypto.CryptoProvider} to encrypt
 * and decrypt page content.
 *
 * The default crypto provider is
 * {@link org.apache.wiki.crypto.DefaultCryptoProvider}.
 *
 * An alternative crypto provider can be set by setting <b>crypto.provider</b>
 * in jspwiki-custom.properties
 *
 * This functionality only encrypts the page content, and not the page
 * properties files. The encryption happens on a
 * {@link org.apache.wiki.providers.FileSystemProvider:putPageText()} and
 * decryption on
 * {@link org.apache.wiki.providers.FileSystemProvider:getPageText()}. This
 * means one page is encrypted at a time.
 *
 * The configuration expects <b>crypto.file</b> in jspwiki-custom.properties
 * which is the absolute path to a file specifying the other cryptographic
 * properties. Ideally this file should be well protected, and read only.
 *
 * <div style="color: red>WARNING: Once you use this <b>EncryptedPageFilter</b>
 * there is no going back. Make sure you know what you are doing!</div>
 *
 * In future there are plans to be able to export your wiki content, so you can
 * import it back with with a simple
 * {@link org.apache.wiki.providers.FileSystemProvider}. This does not exist
 * yet!!
 *
 * Properties within the "crypto.file" include:
 * <table>
 * <th>
 * <td>property</td>
 * <td>description</td>
 * </th>
 * <tr>
 * <td>crypto.key</td>
 * <td>The password that will do the encryption and decryption of content.
 * <b>Required</b></td>
 * </tr>
 * <tr>
 * <td>crypto.prefix</td>
 * <td>A value used to determine if the page content is correctly decrypted.
 * Should be a random string of minimum 10 length</td>
 * </tr>
 * <tr>
 * <td>crypto.suffix</td>
 * <td>A value appended to the end of the encrypted string. Should be a random
 * string of minimum 10 length</td>
 * </tr>
 * </table>
 */
public class EncryptedPageFilter extends BasePageFilter {

    private static final Logger log = Logger.getLogger(EncryptedPageFilter.class);

    private static final int MIN_LENGTH = 10;
    public static final String PROP_CRYPTO_PROVIDER = "crypto.provider";
    public static final String PROP_CRYPTO_FILE = "crypto.file";
    public static final String PROP_CRYPTO_KEY = "crypto.key";
    public static final String PROP_CRYPTO_PREFIX = "crypto.prefix";
    public static final String PROP_CRYPTO_SUFFIX = "crypto.suffix";

    private CryptoProvider cryptoProvider;
    private char[] key;
    private String prefix;
    private String suffix;

    @Override
    public void initialize(Engine engine, Properties properties) throws FilterException {
        super.initialize(engine, properties);

        String providerClassName = TextUtil.getStringProperty(properties, PROP_CRYPTO_PROVIDER, "org.apache.wiki.crypto.DefaultCryptoProvider");
        try {
            String packageName = "org.apache.wiki.crypto"; // TODO: I don't like this
            Class providerClass = ClassUtil.findClass(packageName, providerClassName);
            cryptoProvider = (CryptoProvider) providerClass.newInstance();

            //cryptoProvider.initialize(engine, properties);
        } catch (ClassNotFoundException e) {
            throw new FilterException("The property value " + providerClassName + " could not be created. " + e.getMessage() + PROP_CRYPTO_PROVIDER);
        } catch (InstantiationException e) {
            throw new FilterException("The property value " + providerClassName + " could not be instantiated. " + e.getMessage() + PROP_CRYPTO_PROVIDER);
        } catch (IllegalAccessException e) {
            throw new FilterException("The property value " + providerClassName + " could not be accessed. " + e.getMessage() + PROP_CRYPTO_PROVIDER);
        } catch (ClassCastException e) {
            throw new FilterException("The property value " + providerClassName + " is not a CryptoProvider class. " + e.getMessage() + PROP_CRYPTO_PROVIDER);
        }

        Properties cryptoProperties = new Properties();
        String filename = TextUtil.getStringProperty(properties, PROP_CRYPTO_FILE, CryptoProvider.DEFAULT_CRYPTO_FILENAME);
        File f = new File(filename);
        if (!f.exists()) {
            log.warn("The file specified by " + PROP_CRYPTO_FILE + "=" + f.getAbsolutePath() + " does not exist!");
        } else {
            try {
                cryptoProperties.load(new FileReader(f));
            } catch (Exception ex) {
                throw new FilterException(ex.getMessage());
            }
        }
        key = TextUtil.getRequiredProperty(cryptoProperties, PROP_CRYPTO_KEY).toCharArray();
        if (key.length < MIN_LENGTH) {
            throw new FilterException("The encryption key provided is to short. Min Length required is " + MIN_LENGTH + PROP_CRYPTO_KEY);
        }
        prefix = TextUtil.getStringProperty(cryptoProperties, PROP_CRYPTO_PREFIX, "A@I@#!)KDAS)$:");
        if (prefix.length() < MIN_LENGTH) {
            throw new FilterException("The property value " + prefix + " is to short. Min Length required is " + MIN_LENGTH + PROP_CRYPTO_PREFIX);
        }
        suffix = TextUtil.getStringProperty(cryptoProperties, PROP_CRYPTO_SUFFIX, ")FEJ*$Y@LSDFKJ@V");
        if (suffix.length() < MIN_LENGTH) {
            throw new FilterException("The property value " + suffix + " is to short. Min Length required is " + MIN_LENGTH + PROP_CRYPTO_SUFFIX);
        }
    }

    @Override
    public String preTranslate(final Context context, String result) throws FilterException {
        try {

            result = new String(cryptoProvider.decrypt(key, result.getBytes()));

        } catch (Exception e) {
            throw new FilterException("Error decrypting content. ERROR=" + e + " " + e.getMessage());
        }
        return super.preTranslate(context, result);
    }

    @Override
    public String preSave(final Context context, String content) throws FilterException {
        try {
            content = new String(cryptoProvider.encrypt(key, content.getBytes()));

        } catch (Exception e) {
            throw new FilterException("Error encrypting content. ERROR=" + e + " " + e.getMessage());
        }
        return super.preSave(context, content);
    }
}
