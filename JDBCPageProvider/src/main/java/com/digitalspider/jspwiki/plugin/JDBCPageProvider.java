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

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.search.QueryItem;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.search.SearchMatcher;

public class JDBCPageProvider implements PageProvider {

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
    public static final Integer DEFAULT_MAXRESULTS = 500;
    public static final String DEFAULT_TABLENAME = "jspwiki";
    public static final Boolean DEFAULT_VERSIONING = false;
    public static final Boolean DEFAULT_C3P0 = false;
    public static final Integer DEFAULT_C3P0_MINPOOLSIZE = 5;
    public static final Integer DEFAULT_C3P0_INCREMENT = 5;
    public static final Integer DEFAULT_C3P0_MAXPOOLSIZE = 40;
    public static final String DEFAULT_SOURCE = null;

    private static final String PROP_DRIVER = "jdbc.driver";
    private static final String PROP_URL = "jdbc.url";
    private static final String PROP_USER = "jdbc.user";
    private static final String PROP_PASSWORD = "jdbc.password";
    private static final String PROP_TABLENAME = "jdbc.tablename";
    private static final String PROP_MAXRESULTS = "jdbc.maxresults";
    private static final String PROP_VERSIONING = "jdbc.versioning";
    private static final String PROP_C3P0 = "jdbc.c3p0";
    private static final String PROP_C3P0_MINPOOLSIZE = "jdbc.c3p0.minpoolsize";
    private static final String PROP_C3P0_INCREMENT = "jdbc.c3p0.increment";
    private static final String PROP_C3P0_MAXPOOLSIZE = "jdbc.c3p0.maxpoolsize";
    private static final String PARAM_SOURCE = "src";

    public static final String COLUMN_ID="id";
    public static final String COLUMN_PAGENAME="name";
    public static final String COLUMN_VERSION="version";
    public static final String COLUMN_TEXT="text";
    public static final String COLUMN_AUTHOR="author";
    public static final String COLUMN_CHANGENOTE="changenote";
    public static final String COLUMN_LASTMODIFIED="lastmodified";
    public static final String COLUMN_STATUS="status";

    private ComboPooledDataSource cpds = null;
    private SQLType sqlType = DEFAULT_TYPE;
    private String dbUrl = DEFAULT_URL;
    private String dbUser = DEFAULT_USER;
    private String dbPassword = DEFAULT_PASSWORD;
    private Integer maxResults = DEFAULT_MAXRESULTS;
    private String tableName = DEFAULT_TABLENAME;
    private Boolean isVersioned = DEFAULT_VERSIONING;
    private Boolean c3p0 = DEFAULT_C3P0;
    private Integer c3p0MinPoolSize = DEFAULT_C3P0_MINPOOLSIZE;
    private Integer c3p0Increment = DEFAULT_C3P0_INCREMENT;
    private Integer c3p0MaxPoolSize = DEFAULT_C3P0_MAXPOOLSIZE;
    private String source = DEFAULT_SOURCE;
    private DataSource ds = null;
    private Engine wikiEngine = null;

    @Override
    public void initialize(Engine wikiEngine, Properties properties) throws NoRequiredPropertyException, IOException {
        setLogForDebug(properties.getProperty(PluginManager.PARAM_DEBUG));
        log.info("STARTED");
        this.wikiEngine = wikiEngine;

        // Validate all parameters
        validateParams(properties);

        String sql = "select 1";
        ResultSet rs =null;
        try {
            if (c3p0) {
                initialiseConnectionPool();
            }
            rs = executeQuery(sql,null);
            if (rs.next()) {
                log.info("Successfully initialised JDBCPageProvider");
            }
        } catch (Exception e) {
            log.error("ERROR. "+e.getMessage()+". sql="+sql,e);
            throw new IOException(e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
        }
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
        paramName = getPropKey(PROP_TABLENAME, source);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_TABLENAME);
            }
            tableName = param;
        }
        paramName = getPropKey(PROP_MAXRESULTS, source);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_MAXRESULTS);
            }
            maxResults = Integer.parseInt(param);
        }
        paramName = getPropKey(PROP_VERSIONING, source);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            try {
                Boolean paramValue = Boolean.parseBoolean(param);
                isVersioned = paramValue;
            } catch (Exception e) {
                throw new NoRequiredPropertyException(paramName+" parameter is not true or false",PROP_MAXRESULTS);
            }
        }
        paramName = getPropKey(PROP_C3P0, source);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            try {
                Boolean paramValue = Boolean.parseBoolean(param);
                c3p0 = paramValue;
            } catch (Exception e) {
                throw new NoRequiredPropertyException(paramName+" parameter is not true or false",PROP_C3P0);
            }

            paramName = getPropKey(PROP_C3P0_MINPOOLSIZE, source);
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                log.info(paramName + "=" + param);
                if (!StringUtils.isNumeric(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_C3P0_MINPOOLSIZE);
                }
                c3p0MinPoolSize = Integer.parseInt(param);
            }

            paramName = getPropKey(PROP_C3P0_INCREMENT, source);
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                log.info(paramName + "=" + param);
                if (!StringUtils.isNumeric(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_C3P0_INCREMENT);
                }
                c3p0Increment = Integer.parseInt(param);
            }

            paramName = getPropKey(PROP_C3P0_MAXPOOLSIZE, source);
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                log.info(paramName + "=" + param);
                if (!StringUtils.isNumeric(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value",PROP_C3P0_MAXPOOLSIZE);
                }
                c3p0MaxPoolSize = Integer.parseInt(param);
            }
        }
    }

    protected void initialiseConnectionPool() throws SQLException {
        cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass(sqlType.driverClass);
        } catch (Exception e) {
            throw new SQLException(e);
        }
        cpds.setJdbcUrl( dbUrl );
        cpds.setUser(dbUser);
        cpds.setPassword(dbPassword);

        cpds.setMinPoolSize(c3p0MinPoolSize);
        cpds.setAcquireIncrement(c3p0Increment);
        cpds.setMaxPoolSize(c3p0MaxPoolSize);
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

    private Connection getConnection() throws SQLException {
        Connection conn = null;
        if (ds != null) {
            conn = ds.getConnection();
        } else if (c3p0 && cpds != null) {
            conn = cpds.getConnection();
        } else {
            if (StringUtils.isBlank(dbUser) && StringUtils.isBlank(dbPassword)) {
                conn = DriverManager.getConnection(dbUrl);
            } else {
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            }
            if (conn == null) {
                throw new SQLException("Could not create connection for url=" + dbUrl + " user=" + dbUser);
            }
        }
        return conn;
    }

    private ResultSet executeQuery(String sql, String[] args) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            log.debug("executeQuery() sql=" + sql);
            if (args != null) {
                PreparedStatement stmt = conn.prepareStatement(sql, args);
                return stmt.executeQuery();
            } else {
                Statement stmt = conn.createStatement();
                return stmt.executeQuery(sql);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e,e);
                }
            }
        }
    }

    private int executeUpdate(String sql, String[] args) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, args);
            log.debug("executeUpdate() sql="+sql);
            int result = stmt.executeUpdate();
            log.debug("result="+result);
            return result;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e,e);
                }
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getProviderInfo() {
        return "JDBCPageProvider";
    }

    private int findLatestVersion( String page )
    {
        int version = -1;
        if (!isVersioned) {
            return version;
        }
        try {
            String sql = "select * from "+getTableName()+" where "+COLUMN_PAGENAME+" = '?' and "+COLUMN_STATUS+" != '?' order by "+COLUMN_VERSION+" DESC";
            String[] args = new String[] { page, String.valueOf(version), PageStatus.DELETED.dbValue };
            ResultSet rs = executeQuery(sql,args);
            if (rs.next()) {
                version = rs.getInt(COLUMN_VERSION);
            }
        } catch (Exception e) {
            log.error(e,e);
        }

        return version;
    }

    
 
    /**
     *  {@inheritDoc}
     */
    @Override
    public void putPageText( Page page, String text ) throws ProviderException {
        try {
            String changenote = "new page";
            if (page.getAttribute(WikiPage.CHANGENOTE) != null) {
                changenote = page.getAttribute(WikiPage.CHANGENOTE).toString();
            }
            if (isVersioned) {
                int latest = findLatestVersion( page.getName() );
                if (pageExists(page.getName(), latest)) {
                    latest++;
                }
                String sql = "insert into " + getTableName() + " (" + COLUMN_PAGENAME + "," + COLUMN_VERSION + "," + COLUMN_TEXT + "," + COLUMN_AUTHOR + "," + COLUMN_CHANGENOTE + "," + COLUMN_STATUS + ") values ('?',','?','?','?','?')";
                String[] args = new String[]{page.getName(), String.valueOf(latest), text, page.getAuthor(), changenote, PageStatus.ACTIVE.dbValue};
                int result = executeUpdate(sql,args);
            }
            else {
                int latest = -1;
                if (pageExists(page.getName(), latest)) {
                    String sql = "update " + getTableName() + " set " + COLUMN_TEXT + "='?', " + COLUMN_AUTHOR + "='?', " + COLUMN_CHANGENOTE + "='?', " + COLUMN_STATUS + "='?' where " + COLUMN_PAGENAME + "='?', " + COLUMN_VERSION + "=?";
                    String[] args = new String[]{text, page.getAuthor(), changenote, PageStatus.ACTIVE.dbValue, page.getName(), "-1"};
                    int result = executeUpdate(sql,args);
                } else {
                    String sql = "insert into " + getTableName() + " (" + COLUMN_PAGENAME + "," + COLUMN_VERSION + "," + COLUMN_TEXT + "," + COLUMN_AUTHOR + "," + COLUMN_CHANGENOTE + "," + COLUMN_STATUS + ") values ('?',?,'?','?','?','?')";
                    String[] args = new String[]{page.getName(), String.valueOf(latest), text, page.getAuthor(), changenote, PageStatus.ACTIVE.dbValue};
                    int result = executeUpdate(sql,args);
                }
            }
        } catch (Exception e) {
            throw new ProviderException(e.getMessage());
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean pageExists( String page ) {
        int version = findLatestVersion( page );
        return pageExists( page, version );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean pageExists(String page, int version) {
        try {
            String sql = "select count(1) from "+getTableName()+" where "+COLUMN_PAGENAME+" = '?' and "+COLUMN_VERSION+" = '?' and "+COLUMN_STATUS+" != '?' ";
            String[] args = new String[] { page, String.valueOf(version), PageStatus.DELETED.dbValue };
            ResultSet rs = executeQuery(sql,args);
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public WikiPage getPageInfo( String page, int version ) throws ProviderException {
        try {
            String sql = "select * from "+getTableName()+" where "+COLUMN_PAGENAME+" = '?' and "+COLUMN_VERSION+" = '?' and "+COLUMN_STATUS+" != '?' ";
            String[] args = new String[] { page, String.valueOf(version), PageStatus.DELETED.dbValue };
            ResultSet rs = executeQuery(sql,args);
            if (rs.next()) {
                WikiPage wikiPage = new WikiPage(wikiEngine,rs.getString(COLUMN_PAGENAME));
                wikiPage.setAuthor(rs.getString(COLUMN_AUTHOR));
                wikiPage.setVersion(version);
                wikiPage.setSize(rs.getString(COLUMN_TEXT).length());
                wikiPage.setAttribute( WikiPage.CHANGENOTE, rs.getString(COLUMN_CHANGENOTE) );
                wikiPage.setLastModified(rs.getDate(COLUMN_LASTMODIFIED));
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return null;
    }

    
    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection<SearchResult> findPages( org.apache.wiki.api.search.QueryItem[] query ) {
        List<SearchResult> pages = new ArrayList<SearchResult>();
        try {
            if (query.length >0 && StringUtils.isNotBlank(query[0].word)) {
                String sql = "select distinct " + COLUMN_PAGENAME + " from " + getTableName() + " where " + COLUMN_TEXT + " like '%" + query[0].word + "%' and " + COLUMN_STATUS + " != '?'";
                sql = addLimits(sqlType,sql,maxResults);
                String[] args = new String[]{PageStatus.DELETED.dbValue};
                ResultSet rs = executeQuery(sql,args);
                while (rs.next()) {
                    String pageName = rs.getString(COLUMN_PAGENAME);
                    WikiPage wikiPage = getPageInfo(pageName, findLatestVersion(pageName));
                    SearchResult  result = new SearchResult() {
                        @Override
                        public Page getPage() {
                            return wikiPage;
                        }

                        @Override
                        public int getScore() {
                            return 1;
                        }

                        @Override
                        public String[] getContexts() {
                            return new String[0];
                        }
                    };
                    pages.add(result);
                }
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return pages;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection getAllPages() throws ProviderException {
        List<WikiPage> pages = new ArrayList<WikiPage>();
        try {
            String sql = "select distinct "+COLUMN_PAGENAME+" from "+getTableName()+" where "+COLUMN_STATUS+" != '?'";
            sql = addLimits(sqlType,sql,maxResults);
            String[] args = new String[] { PageStatus.DELETED.dbValue };
            ResultSet rs = executeQuery(sql,args);
            while (rs.next()) {
                String pageName = rs.getString(COLUMN_PAGENAME);
                WikiPage wikiPage = getPageInfo( pageName, findLatestVersion(pageName) );
                pages.add(wikiPage);
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return pages;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection getAllChangedSince( Date date ) {
        List<WikiPage> pages = new ArrayList<WikiPage>();
        try {
            String sql = "select distinct "+COLUMN_PAGENAME+" from "+getTableName()+" where "+COLUMN_LASTMODIFIED+" >= '"+date+"' and "+COLUMN_STATUS+" != '?'";
            sql = addLimits(sqlType,sql,maxResults);
            String[] args = new String[] { PageStatus.DELETED.dbValue };
            ResultSet rs = executeQuery(sql,args);
            while (rs.next()) {
                String pageName = rs.getString(COLUMN_PAGENAME);
                WikiPage wikiPage = getPageInfo( pageName, findLatestVersion(pageName) );
                pages.add(wikiPage);
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return pages;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int getPageCount() throws ProviderException {
        int result = 0;
        try {
            String sql = "select count(distinct "+COLUMN_PAGENAME+") from "+getTableName()+" where "+COLUMN_STATUS+" != ?";
            String[] args = new String[] { PageStatus.DELETED.dbValue };
            ResultSet rs = executeQuery(sql,args);
            while (rs.next()) {
                result ++;
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return result;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public List getVersionHistory( String page ) throws ProviderException {
        List<WikiPage> versionHistory = new ArrayList<WikiPage>();
        try {
            int version = findLatestVersion( page );
            while (version >= 0) {
                WikiPage wikiPage = getPageInfo( page, version );
                versionHistory.add(wikiPage);
                version--;
            }
            if (version == -1) {
                WikiPage wikiPage = getPageInfo( page, version );
                versionHistory.add(wikiPage);
            }
        } catch (Exception e) {
            log.error(e,e);
        }
        return versionHistory;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getPageText( String page, int version ) throws ProviderException {
        WikiPage wikiPage = getPageInfo( page, version );
        return wikiPage.getWiki();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deleteVersion( String pageName, int version ) throws ProviderException {
        try {
            String sql = "update "+getTableName()+" set "+COLUMN_STATUS+" = '?' where "+COLUMN_PAGENAME+" = '?' and "+COLUMN_VERSION+" = '?'";
            String[] args = new String[] { PageStatus.DELETED.dbValue, pageName, String.valueOf(version) };
            int result = executeUpdate(sql,args);
        } catch (Exception e) {
            log.error(e,e);
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deletePage( String pageName ) throws ProviderException {
        try {
            String sql = "update "+getTableName()+" set "+COLUMN_STATUS+" = '?' where "+COLUMN_PAGENAME+" = '?'";
            String[] args = new String[] { PageStatus.DELETED.dbValue, pageName };
            int result = executeUpdate(sql,args);
        } catch (Exception e) {
            log.error(e,e);
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void movePage(String from, String to) throws ProviderException {
        if (pageExists(to)) {
            throw new ProviderException("The destination page "+to+" already exists");
        }
        try {
            String sql = "update "+getTableName()+" set "+COLUMN_PAGENAME+" = '?' where "+COLUMN_PAGENAME+" = '?'";
            String[] args = new String[] { to, from };
            int result = executeUpdate(sql,args);
        } catch (Exception e) {
            log.error(e,e);
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
