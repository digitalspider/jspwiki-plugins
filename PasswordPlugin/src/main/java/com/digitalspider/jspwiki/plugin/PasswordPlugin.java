/*
 * Copyright (C) 2014 David Vittor http://digitalspider.com.au
 */
package com.digitalspider.jspwiki.plugin;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.PluginException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.pages.PageManager;

public class PasswordPlugin implements Plugin {

    private static final Logger log = Logger.getLogger(PasswordPlugin.class);

    private static final Integer DEFAULT_ID = null;
    private static final byte[] DEFAULT_SECRET = new byte[0];
    private static final Integer DEFAULT_LEVEL = 1;

    private static final String PARAM_ID = "id";
    private static final String PARAM_SECRET = "secret";
    private static final String PARAM_LEVEL = "level";

    private static final String KEY_PREFIX = "password.key.";
    private static Map<Integer, byte[]> cache = new HashMap<Integer, byte[]>();

    private Integer id = DEFAULT_ID;
    private byte[] secret = DEFAULT_SECRET;
    private Integer level = DEFAULT_LEVEL;

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();

        // Validate all parameters
        validateParams(wikiContext, params);

        Engine engine = wikiContext.getEngine();
        PageManager pageManager = engine.getManager(PageManager.class);
        String baseUrl = engine.getBaseURL();
        Properties properties = engine.getWikiProperties();

        try {
            if (id == null) {
                id = doLock(level.toString(), secret, properties);
                params.put(PARAM_ID, id.toString());
            }
            result = "<div class='password'>" + id + "</div>";
        } catch (Exception e) {
            log.error("Start up error. " + e.getMessage());
            log.debug(e, e);
            throw new PluginException(e.getMessage());
        }

        return result;
    }

    protected void validateParams(Context wikiContext, Map<String, String> params) throws PluginException {
        String paramName;
        String param;

        log.info("validateParams() START");
        paramName = PARAM_ID;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            id = Integer.parseInt(param);
        }
        paramName = PARAM_SECRET;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            secret = param.getBytes();
        }
        paramName = PARAM_LEVEL;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a valid number");
            }
            level = Integer.parseInt(param);
            if (level < 1 || level > 9) {
                throw new PluginException(paramName + " value " + param + " cannot be less than 0 or more than 9");
            }
        }
    }

    private String getPropKey(String currentKey, String source) {
        String result = currentKey;
        if (StringUtils.isNotBlank(source)) {
            result += "." + source;
        }
        return result;
    }

    private static Cipher getCipher(char[] key, int mode) throws PluginException {
        // TODO: Document - http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html
        try {
            String transformation = "PBEWithMD5AndDES";
            byte[] salt = "protects".substring(0, 8).getBytes();
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
            log.error("Error encrypting password. " + e.getMessage());
            log.debug(e, e);
            throw new PluginException("Error encrypting password. " + e.getMessage());
        }
    }

    public static byte[] encrypt(char[] key, byte[] content) throws PluginException {
        try {
            Cipher pbeCipher = getCipher(key, Cipher.ENCRYPT_MODE);
            byte[] encrypted = pbeCipher.doFinal(content);
            encrypted = Base64.encodeBase64(encrypted);
            return encrypted;
        } catch (Exception e) {
            log.error("Error encrypting password. " + e.getMessage());
            log.debug(e, e);
            throw new PluginException("Error encrypting password. " + e.getMessage());
        }
    }

    public static byte[] decrypt(char[] key, byte[] content) throws PluginException {
        try {
            Cipher pbeCipher = getCipher(key, Cipher.DECRYPT_MODE);
            content = Base64.decodeBase64(content);
            content = pbeCipher.doFinal(content);
            return content;
        } catch (Exception e) {
            log.error("Error decrypting password. " + e.getMessage());
            log.debug(e, e);
            throw new PluginException("Error decrypting password. " + e.getMessage());
        }
    }

    public static Integer getPasswordID(Integer level) throws PluginException {
        if (level > 9) {
            throw new PluginException("Level greater than 9 is unsupported");
        }
        if (level < 1) {
            throw new PluginException("Level less than 1 is invalid");
        }
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString().replaceAll("[a-zA-Z]|-", "").substring(0, 5);
        id = id.substring(0, 2) + level + id.substring(2);
        return Integer.parseInt(id);
    }

    public static Properties getUnlockProperties(String... params) {
        Properties properties = new Properties();
        int i = 1;
        for (String param : params) {
            properties.put(KEY_PREFIX + i, param);
            i++;
        }
        return properties;
    }

    public static char[] getDecryptKey(Integer level, Properties properties) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 1; i <= level; i++) {

            buffer.append(properties.getProperty(KEY_PREFIX + i));
        }
        return buffer.toString().toCharArray();
    }

    /**
     * Calculate the number of passwords required to unlock this password
     *
     * @param passwordId the id of the password required
     * @return the number of passwords required
     */
    public static Integer getPasswordLevel(Integer passwordId) {
        Integer level = (passwordId % 10000) / 1000;
        return level;
    }

    public static String doUnlock(Integer passwordId, String... params) {
        return doUnlock(passwordId, getUnlockProperties(params));
    }

    public static Integer doLock(String levelParam, byte[] plain, String... params) {
        return doLock(levelParam, plain, getUnlockProperties(params));
    }

    /**
     * Main method to unlock a secret
     *
     * @param passwordId which password do you want to unlock
     * @param params provide the passwords to unlock the secret
     * @return the unlocked secret, or null if failed
     */
    public static String doUnlock(Integer passwordId, Properties params) {
        try {
            Integer level = getPasswordLevel(passwordId);
            char[] unlockKey = getDecryptKey(level, params);
            byte[] result = decrypt(unlockKey, cache.get(passwordId));
            return new String(result);
        } catch (Exception e) {
            log.error("Could not decrypt the password");
            log.debug("Could not decrypt the password", e);
        }
        return null;
    }

    public static Integer doLock(String levelParam, byte[] plain, Properties params) {
        try {
            Integer level = Integer.parseInt(levelParam);
            char[] unlockKey = getDecryptKey(level, params);
            byte[] result = encrypt(unlockKey, plain);
            Integer passwordId = getPasswordID(level);
            cache.put(passwordId, result);
            return passwordId;
        } catch (Exception e) {
            log.error("Could not encrypt the password");
            log.debug("Could not encrypt the password", e);
        }
        return -1;
    }
}
