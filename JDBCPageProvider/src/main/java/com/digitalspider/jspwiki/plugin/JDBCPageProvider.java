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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.plugin.DefaultPluginManager;
import org.apache.wiki.providers.WikiPageProvider;
import org.apache.wiki.render.XHTMLRenderer;
import org.apache.wiki.search.QueryItem;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class JDBCPageProvider implements WikiPageProvider {

	private final Logger log = Logger.getLogger(JDBCPageProvider.class);

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

    public enum PageStatus {
        ACTIVE("AC"), DELETED("DL");

        private String dbValue;
        PageStatus(String dbValue) {
            this.dbValue = dbValue;
        }
    }

    public static final SQLType DEFAULT_TYPE = SQLType.MYSQL;
    public static final String DEFAULT_URL = "";
    public static final String DEFAULT_USER = "";
    public static final String DEFAULT_PASSWORD = "";
    public static final Integer DEFAULT_MAXRESULTS = 50;
    public static final String DEFAULT_TABLENAME = "jspwiki-page";
    public static final String DEFAULT_SQL = "select 1";
    public static final Boolean DEFAULT_HEADER = true;
    public static final String DEFAULT_SOURCE = null;

    private static final String PROP_DRIVER = "jdbc.driver";
    private static final String PROP_URL = "jdbc.url";
    private static final String PROP_USER = "jdbc.user";
    private static final String PROP_PASSWORD = "jdbc.password";
    private static final String PROP_TABLENAME = "jdbc.tablename";
    private static final String PROP_MAXRESULTS = "jdbc.maxresults";
    private static final String PARAM_SOURCE = "src";

    public static final String COLUMN_PAGENAME="name";
    public static final String COLUMN_VERSION="version";
    public static final String COLUMN_TEXT="text";
    public static final String COLUMN_AUTHOR="author";
    public static final String COLUMN_CHANGENOTE="changenote";
    public static final String COLUMN_STATUS="status";

    private SQLType sqlType = DEFAULT_TYPE;
    private String dbUrl = DEFAULT_URL;
    private String dbUser = DEFAULT_USER;
    private String dbPassword = DEFAULT_PASSWORD;
    private Integer maxResults = DEFAULT_MAXRESULTS;

    private String tableName = DEFAULT_TABLENAME;
    private String sql = DEFAULT_SQL;
    private Boolean header = DEFAULT_HEADER;
    private String source = DEFAULT_SOURCE;
    private DataSource ds = null;
    private WikiEngine wikiEngine = null;

    @Override
    public void initialize(WikiEngine wikiEngine, Properties properties) throws NoRequiredPropertyException, IOException {
        setLogForDebug(properties.getProperty(PluginManager.PARAM_DEBUG));
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();
        this.wikiEngine = wikiEngine;
        Properties props = wikiEngine.getWikiProperties();

        // Validate all parameters
        validateParams(properties);

        Connection conn = null;
        try {
            conn = getConnection();

            sql = addLimits(sqlType,sql,maxResults);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            ResultSetMetaData md = rs.getMetaData();
            if (header) {
                for (int i = 0; i < md.getColumnCount(); i++) {
                    String header = md.getColumnLabel(i + 1);
                    buffer.append("|| " + header);
                }
                buffer.append("\n");
            }

            while (rs.next()) {
                for (int i=0; i<md.getColumnCount(); i++) {
                    String value = rs.getString(i+1);
                    buffer.append("| "+value);
                }
                buffer.append("\n");
            }
        } catch (Exception e) {
            log.error("ERROR. "+e.getMessage()+". sql="+sql,e);
            throw new IOException(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
	}

    private Connection getConnection() throws Exception {
        Connection conn = null;
        try {

            if (ds == null) {
                if (StringUtils.isBlank(dbUser) && StringUtils.isBlank(dbPassword)) {
                    conn = DriverManager.getConnection(dbUrl);
                } else {
                    conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                }
                if (conn == null) {
                    throw new Exception("Could not create connection for url=" + dbUrl + " user=" + dbUser);
                }
            } else {
                conn = ds.getConnection();
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
        return conn;
    }

    protected void validateParams(Properties props) throws NoRequiredPropertyException {
        String paramName;
        String param;

        log.info("validateParams() START");
        paramName = PARAM_SOURCE;
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new NoRequiredPropertyException(paramName + " parameter is not a valid value",PARAM_SOURCE);
            }
            source = param;
        }
        paramName = getPropKey(PROP_DRIVER, source);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                sqlType = SQLType.parse(param);
            } catch (Exception e) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value. " +param,PROP_DRIVER);
            }
            try {
                Class.forName(param).newInstance();
            }
            catch(ClassNotFoundException e) {
                log.error("Error: unable to load driver class "+param+"!",e);
                throw new NoRequiredPropertyException("Error: unable to load driver class "+param+"!",PROP_DRIVER);
            }
            catch(IllegalAccessException e) {
                log.error("Error: access problem while loading "+param+"!",e);
                throw new NoRequiredPropertyException("Error: access problem while loading "+param+"!",PROP_DRIVER);
            }
            catch(InstantiationException e) {
                log.error("Error: unable to instantiate driver "+param+"!",e);
                throw new NoRequiredPropertyException("Error: unable to instantiate driver "+param+"!",PROP_DRIVER);
            }
            catch(Exception e) {
                log.error("Error: unable to load driver "+param+"!",e);
                throw new NoRequiredPropertyException("Error: unable to load driver "+param+"! "+e.getMessage(),PROP_DRIVER);
            }
        } else {
            try {
                Context ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/" + source);
            } catch (NamingException e) {
                log.error("Neither jspwiki-custom.properties or conf/context.xml has not been configured for "+source+"!");
                throw new NoRequiredPropertyException("Neither jspwiki-custom.properties or conf/context.xml has not been configured for "+source+"!",PARAM_SOURCE);
            }
        }
        if (ds == null) {
            paramName = getPropKey(PROP_URL, source);
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                log.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_URL);
                }
                if (!param.trim().startsWith(sqlType.startsWith)) {
                    throw new NoRequiredPropertyException("Error: " + paramName + " property has value " + param + ". " +
                            "Expected: " + sqlType.urlDefaultPath,PROP_URL);
                }
                dbUrl = param;
            }
            paramName = getPropKey(PROP_USER, source);
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                log.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_USER);
                }
                dbUser = param;
            }
            paramName = getPropKey(PROP_PASSWORD, source);
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                log.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_PASSWORD);
                }
                dbPassword = param;
            }
        }
        paramName = getPropKey(PROP_TABLENAME,source);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_TABLENAME);
            }
            tableName = param;
        }
        paramName = getPropKey(PROP_MAXRESULTS,source);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_MAXRESULTS);
            }
            maxResults = Integer.parseInt(param);
        }
    }

    private String addLimits(SQLType sqlType, String sql, Integer maxResults) {
        String result = sql;
        if (StringUtils.isNotBlank(sql)) {
            result = sql.trim();
            if (result.endsWith(";")) {
                result = result.substring(result.length()-1);
            }
            switch (sqlType) {
                case MSSQL:
                    if (!result.toLowerCase().contains(" top")) {
                        result = sql.replace("select", "select top " + maxResults);
                        result += ";";
                    }
                    break;
                case MYSQL:
                    if (!result.toLowerCase().contains(" limit ")) {
                        result = result + " limit " + maxResults+";";
                    }
                    break;
                case ORACLE:
                    if (!result.toLowerCase().contains("rownum")) {
                        result = "select * from ( "+result+" ) where ROWNUM <= " + maxResults+";";
                    }
                    break;
                case POSTGRESQL:
                    if (!result.toLowerCase().contains(" limit ")) {
                        result = result + " limit " + maxResults+";";
                    }
                    break;
                case DB2:
                    if (!result.toLowerCase().contains(" fetch")) {
                        result = result + " FETCH FIRST "+maxResults+" ROWS ONLY;";
                    }
                    break;
                case SYBASE:
                    if (!result.toLowerCase().contains(" top")) {
                        result = result.replace("select", "select top " + maxResults);
                        result += ";";
                    }
                    break;
            }
        }
        return result;
    }

    private String getPropKey(String currentKey, String source) {
        String result = currentKey;
        if (StringUtils.isNotBlank(source)) {
            result+="."+source;
        }
        return result;
    }

    private void setLogForDebug(String value) {
        if (StringUtils.isNotBlank(value) && (value.equalsIgnoreCase("true") || value.equals("1"))) {
            log.setLevel(Level.INFO);
        }
    }

    @Override
    public String getProviderInfo() {
        return "JDBCPageProvider";
    }

    @Override
    public void putPageText( WikiPage page, String text ) throws ProviderException {
        try {
            Connection conn = getConnection();
            String sql = "insert into "+getTableName()+" ("+COLUMN_PAGENAME+","+COLUMN_VERSION+","+COLUMN_TEXT+","+COLUMN_AUTHOR+","+COLUMN_STATUS+") values (?,?,?,?)";
            String[] args = new String[] { page.getName(), String.valueOf(page.getVersion()), text, page.getAuthor() };
            Statement stmt = conn.prepareStatement(sql, args);
            int result = stmt.executeUpdate(sql);
        } catch (Exception e) {
            throw new ProviderException(e.getMessage());
        }
    }

    @Override
    public boolean pageExists( String page ) {
        try {
            Connection conn = getConnection();
            String sql = "select count(1) from "+getTableName()+" where "+COLUMN_PAGENAME+" = ? and "+COLUMN_STATUS+" != ? ";
            String[] args = new String[] { page, PageStatus.DELETED.dbValue };
            Statement stmt = conn.prepareStatement(sql, args);
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return false;
    }

    @Override
    public boolean pageExists(String page, int version) {
        try {
            Connection conn = getConnection();
            String sql = "select count(1) from "+getTableName()+" where "+COLUMN_PAGENAME+" = ? and "+COLUMN_VERSION+" = ? and "+COLUMN_STATUS+" != ? ";
            String[] args = new String[] { page, String.valueOf(version), PageStatus.DELETED.dbValue };
            Statement stmt = conn.prepareStatement(sql, args);
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return false;
    }

    @Override
    public Collection findPages( QueryItem[] query ) {

    }

    @Override
    public WikiPage getPageInfo( String page, int version ) throws ProviderException {
        try {
            Connection conn = getConnection();
            String sql = "select * from "+getTableName()+" where "+COLUMN_PAGENAME+" = ? and "+COLUMN_VERSION+" = ? and "+COLUMN_STATUS+" != ? ";
            String[] args = new String[] { page, String.valueOf(version), PageStatus.DELETED.dbValue };
            Statement stmt = conn.prepareStatement(sql, args);
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                WikiPage wikiPage = new WikiPage(wikiEngine,rs.getString(COLUMN_PAGENAME));
                wikiPage.setAuthor(rs.getString(COLUMN_AUTHOR));
                wikiPage.setVersion(version);
                wikiPage.setSize(rs.getString(COLUMN_TEXT).length());
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return null;
    }

    @Override
    public Collection getAllPages() throws ProviderException {
        List<WikiPage> pages = new ArrayList<WikiPage>();
        try {
            Connection conn = getConnection();
            String sql = "select distinct "+COLUMN_PAGENAME+" from "+getTableName()+" where "+COLUMN_STATUS+" != ?";
            String[] args = new String[] { PageStatus.DELETED.dbValue };
            Statement stmt = conn.prepareStatement(sql, args);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                WikiPage wikiPage = new WikiPage(wikiEngine,rs.getString(COLUMN_PAGENAME));
                wikiPage.setAuthor(rs.getString(COLUMN_AUTHOR));
                wikiPage.setVersion(rs.getInt(COLUMN_VERSION));
                wikiPage.setSize(rs.getString(COLUMN_TEXT).length());
                pages.add(wikiPage);
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return pages;
    }

    @Override
    public Collection getAllChangedSince( Date date ) {

    }

    @Override
    public int getPageCount() throws ProviderException {
        int result = 0;
        try {
            Connection conn = getConnection();
            String sql = "select count(distinct "+COLUMN_PAGENAME+") from "+getTableName()+" where "+COLUMN_STATUS+" != ?";
            String[] args = new String[] { PageStatus.DELETED.dbValue };
            Statement stmt = conn.prepareStatement(sql, args);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result ++;
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return result;
    }

    @Override
    public List getVersionHistory( String page ) throws ProviderException {

    }

    @Override
    public String getPageText( String page, int version ) throws ProviderException {

    }

    @Override
    public void deleteVersion( String pageName, int version ) throws ProviderException {

    }

    @Override
    public void deletePage( String pageName ) throws ProviderException {

    }


    @Override
    public void movePage(String from, String to) throws ProviderException {

    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
