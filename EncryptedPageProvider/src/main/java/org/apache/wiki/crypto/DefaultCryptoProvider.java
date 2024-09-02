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
package org.apache.wiki.crypto;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.EncryptionException;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.util.TextUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.apache.wiki.api.core.Engine;

/**
 * Provides cryptographic encryption and decryption services.
 *
 * See: <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html">Oracle CryptoSpec.html</a>
 *
 * The default implementation here uses a password-based encryption (PBE) cipher.
 *
 * The configuration expects <b>crypto.file</b> in jspwiki-custom.properties which is the absolute
 * path to a file specifying the other cryptographic properties. Ideally this file should be well
 * protected, and read only.
 *
 * Properties within the "crypto.file" include:
 * <table>
 *     <th>
 *         <td>property</td>
 *         <td>description</td>
 *     </th>
 *     <tr>
 *         <td>crypto.base64</td>
 *         <td>If true will apply base64 encoding and decoding to the encrypted content.
 *         This ensures the content store in the {@link org.apache.wiki.providers.WikiPageProvider} is not just binary, but base64 encoded.
 *         Default is true</td>
 *     </tr>
 *     <tr>
 *         <td>crypto.salt</td>
 *         <td>The salt used to create the PBEParameterSpec</td>
 *     </tr>
 *     <tr>
 *         <td>crypto.blocksize</td>
 *         <td>The blocksize specified the length of the salt, must be equal or smaller than the salt length</td>
 *     </tr>
 *     <tr>
 *         <td>crypto.itrcount</td>
 *         <td>The iteration count used to create the PBEParameterSpec</td>
 *     </tr>
 *     <tr>
 *         <td>crypto.algorithm</td>
 *         <td>The algorithm to use to create the SecretKeyFactory and Cipher</td>
 *     </tr>
 *
 * </table>
 */
public class DefaultCryptoProvider implements CryptoProvider {

    private static final Logger log = Logger.getLogger(DefaultCryptoProvider.class);

    public static final String PROP_CRYPTO_FILE = "crypto.file";
    public static final String PROP_CRYPTO_BASE64 = "crypto.base64";
    public static final String PROP_CRYPTO_SALT = "crypto.salt";
    public static final String PROP_CRYPTO_BLOCKSIZE = "crypto.blocksize";
    public static final String PROP_CRYPTO_ITERACTIONCOUNT = "crypto.itrcount";
    public static final String PROP_CRYPTO_ALGORITHM = "crypto.algorithm";

    private boolean base64;
    private String salt;
    private int blockSize;
    private int iterationCount;
    private String algorithm;

    @Override
    public void initialize(Engine engine, Properties properties) throws NoRequiredPropertyException, IOException {
        Properties cryptoProperties = new Properties();
        String filename = TextUtil.getStringProperty(properties,PROP_CRYPTO_FILE, CryptoProvider.DEFAULT_CRYPTO_FILENAME);
        File f = new File(filename);
        if (!f.exists()) {
            log.warn("The file specified by " + PROP_CRYPTO_FILE + "=" + f.getAbsolutePath() + " does not exist!");
        } else {
            cryptoProperties.load(new FileReader(f));
        }
        base64 = TextUtil.getBooleanProperty(cryptoProperties, PROP_CRYPTO_BASE64, true);
        salt = TextUtil.getStringProperty(cryptoProperties,PROP_CRYPTO_SALT, "Ra%$ESSQA#!@)#$@)");
        blockSize = TextUtil.getIntegerProperty(cryptoProperties, PROP_CRYPTO_BLOCKSIZE, 8);
        iterationCount = TextUtil.getIntegerProperty(cryptoProperties,PROP_CRYPTO_ITERACTIONCOUNT, 2048);
        algorithm = TextUtil.getStringProperty(cryptoProperties,PROP_CRYPTO_ALGORITHM, "PBEWithMD5AndDES");
        if (blockSize > salt.length()) {
            throw new NoRequiredPropertyException("The block size specified is longer then the salt length",PROP_CRYPTO_BLOCKSIZE);
        }
    }

    @Override
    public byte[] encrypt(char[] key, byte[] content) throws EncryptionException {
        try {
            Cipher pbeCipher = getCipher(key, Cipher.ENCRYPT_MODE);
            byte[] encrypted = pbeCipher.doFinal(content);
            if (base64) {
                encrypted = Base64.encodeBase64(encrypted);
            }
            return encrypted;
        } catch (Exception e) {
            throw new EncryptionException("Could not encrypt content. ERROR="+e+" "+e.getMessage());
        }
    }

    @Override
    public byte[] decrypt(char[] key, byte[] content) throws EncryptionException {
        try {
            Cipher pbeCipher = getCipher(key, Cipher.DECRYPT_MODE);
            if (base64) {
                content = Base64.decodeBase64(content);
            }
            content = pbeCipher.doFinal(content);
            return content;
        } catch (Exception e) {
            throw new EncryptionException("Could not decrypt content. ERROR="+e+" "+e.getMessage());
        }
    }

    private Cipher getCipher(char[] key, int mode) throws EncryptionException {
        Cipher cipher = null;
        try {
            String transformation = algorithm;
            byte[] saltBytes = salt.substring(0, blockSize).getBytes();
            int count = iterationCount;

            // Create PBE parameter set
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(saltBytes, count);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(key);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance(transformation);
            SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

            Cipher pbeCipher = Cipher.getInstance(transformation);
            pbeCipher.init(mode, pbeKey, pbeParamSpec);
            return pbeCipher;
        } catch (Exception e) {
            throw new EncryptionException("Could not create cipher. ERROR="+e+" "+e.getMessage());
        }
    }

    @Override
    public String getProviderInfo() {
        return "DefaultCryptoProvider";
    }
}
