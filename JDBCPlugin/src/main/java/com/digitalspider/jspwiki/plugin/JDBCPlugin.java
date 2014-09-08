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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.ProviderException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.plugin.DefaultPluginManager;
import org.apache.wiki.render.XHTMLRenderer;
import org.apache.wiki.util.comparators.JavaNaturalComparator;

public class JDBCPlugin implements WikiPlugin {

	private final Logger log = Logger.getLogger(JDBCPlugin.class);

    public enum SQLType {
        MYSQL("com.mysql.jdbc.Driver", "jdbc:mysql:", "jdbc:mysql://hostname:portNumber/databaseName"),
        MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver:", "jdbc:sqlserver://serverName\\instanceName:portNumber"),
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql:", "jdbc:postgresql://hostname:portNumber/databaseName"),
        ORACLE("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:", "jdbc:oracle:thin:@hostname:portNumber:databaseName"),
        DB2("COM.ibm.db2.jdbc.net.DB2Driver", "jdbc:db2:", "jdbc:db2:hostname:portNumber/databaseName"),
        SYBASE("com.sybase.jdbc.SybDriver", "jdbc:sybase:", "jdbc:sybase:Tds:hostname:portNumber/databaseName");

        private String driverClass;
        private String startsWith;
        private String urlDefaultPath;
        SQLType(String driverClass, String startsWith, String urlDefaultPath) {
            this.driverClass = driverClass;
            this.startsWith = startsWith;
            this.urlDefaultPath = urlDefaultPath;
        }
        public static SQLType parse(String input) throws Exception {
            for (SQLType type : SQLType.values()) {
                if (type.name().equalsIgnoreCase(input) || type.driverClass.equalsIgnoreCase(input)) {
                    return type;
                }
            }
            throw new Exception("Could not find SQLType of value: "+input);
        }
    }

    public static final SQLType DEFAULT_TYPE = SQLType.MYSQL;
    public static final String DEFAULT_URL = "";
    public static final String DEFAULT_USER = "";
    public static final String DEFAULT_PASSWORD = "";
    public static final Integer DEFAULT_MAXRESULTS = 100;
    public static final String DEFAULT_CLASS = "sql-table";
    public static final String DEFAULT_SQL = "select 1";

    private static final String PROP_DRIVER = "jdbc.driver";
    private static final String PROP_URL = "jdbc.url";
    private static final String PROP_USER = "jdbc.user";
    private static final String PROP_PASSWORD = "jdbc.password";
    private static final String PROP_MAXRESULTS = "jdbc.maxresults";
    private static final String PARAM_CLASS = "class";
    private static final String PARAM_SQL = "sql";

    private SQLType sqlType = DEFAULT_TYPE;
    private String dbUrl = DEFAULT_URL;
    private String dbUser = DEFAULT_USER;
    private String dbPassword = DEFAULT_PASSWORD;
    private Integer maxResults = DEFAULT_MAXRESULTS;
    private String className = DEFAULT_CLASS;
    private String sql = DEFAULT_SQL;

    private static final String DELIM = " | ";
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
            Connection conn = null;
            if (StringUtils.isBlank(dbUser) && StringUtils.isBlank(dbPassword)) {
                conn = DriverManager.getConnection(dbUrl);
            } else {
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            }
            if (conn == null) {
                throw new Exception("Could not create connection for url="+dbUrl+" user="+dbUser);
            }

            sql = addLimits(sqlType,sql,maxResults);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            ResultSetMetaData md = rs.getMetaData();
            for (int i=0; i<md.getColumnCount(); i++) {
                String header = md.getColumnLabel(i+1);
                buffer.append("|| "+header);
            }
            buffer.append("\n");

            while (rs.next()) {
                for (int i=0; i<md.getColumnCount(); i++) {
                    String value = rs.getString(i+1);
                    buffer.append("| "+value);
                }
                buffer.append("\n");
            }

            log.info("result="+buffer.toString());
            Reader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes())));
            JSPWikiMarkupParser parser = new JSPWikiMarkupParser(wikiContext, in);
            WikiDocument doc = parser.parse();
            log.debug("doc=" + doc);
            XHTMLRenderer renderer = new XHTMLRenderer(wikiContext, doc);
            result = renderer.getString();

            result = "<div class='"+className+"'>"+result+"</div>";
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
        paramName = PROP_DRIVER;
        param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                sqlType = SQLType.parse(param);
            } catch (Exception e) {
                throw new PluginException(paramName + " property is not a valid value. " +param);
            }
            try {
                Class.forName(param).newInstance();
            }
            catch(ClassNotFoundException e) {
                log.error("Error: unable to load driver class "+param+"!",e);
                throw new PluginException("Error: unable to load driver class "+param+"!");
            }
            catch(IllegalAccessException e) {
                log.error("Error: access problem while loading "+param+"!",e);
                throw new PluginException("Error: access problem while loading "+param+"!");
            }
            catch(InstantiationException e) {
                log.error("Error: unable to instantiate driver "+param+"!",e);
                throw new PluginException("Error: unable to instantiate driver "+param+"!");
            }
            catch(Exception e) {
                log.error("Error: unable to load driver "+param+"!",e);
                throw new PluginException("Error: unable to load driver "+param+"! "+e.getMessage());
            }
        } else {
            log.error("jspwiki-custom.properties has not been configured for "+paramName+"!");
            throw new PluginException("jspwiki-custom.properties has not been configured for "+paramName+"!");
        }
        paramName = PROP_URL;
        param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " property is not a valid value");
            }
            if (!param.trim().startsWith(sqlType.startsWith)) {
                throw new PluginException("Error: "+paramName+" property has value "+param+". "+
                    "Expected: "+sqlType.urlDefaultPath);
            }
            dbUrl = param;
        }
        paramName = PROP_USER;
        param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " property is not a valid value");
            }
            dbUser = param;
        }
        paramName = PROP_PASSWORD;
        param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " property is not a valid value");
            }
            dbPassword = param;
        }
        paramName = PROP_MAXRESULTS;
        param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " property is not a valid value");
            }
            maxResults = Integer.parseInt(param);
        }

        paramName = PARAM_CLASS;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            className = param;
        }
        paramName = PARAM_SQL;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            if (!sql.toLowerCase().startsWith("select")) {
                throw new PluginException(paramName + " parameter needs to start with 'SELECT'.");
            }
            sql = param;
        }
    }

    private String addLimits(SQLType sqlType, String sql, Integer maxResults) {
        String result = sql;
        if (StringUtils.isNotBlank(sql)) {
            switch (sqlType) {
                case MSSQL:
                    result = sql.replace("select","select top "+maxResults);
                    break;
                case MYSQL:
                    result = sql+" limit "+maxResults;
                    break;
                case ORACLE:
                    result = sql.replace("select","select top "+maxResults);
                    break;
                case POSTGRESQL:
                    result = sql.replace("select","select top "+maxResults);
                    break;
                case DB2:
                    result = sql.replace("select","select top "+maxResults);
                    break;
                case SYBASE:
                    result = sql.replace("select","select top "+maxResults);
                    break;
            }
        }
        return result;
    }
}
