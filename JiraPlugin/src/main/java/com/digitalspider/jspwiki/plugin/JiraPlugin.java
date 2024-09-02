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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.PluginException;
import org.codehaus.jettison.json.JSONException;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Priority;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import org.apache.commons.lang3.StringUtils;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.render.RenderingManager;

/**
 * Use [{Jira project=JSPWIKI jql='status = open'}]
 *
 * @author vittord
 */
public class JiraPlugin implements Plugin {

    private static final Logger log = Logger.getLogger(JiraPlugin.class);

    public enum MetadataType {
        PRIORITY, STATUS, ISSUETYPE
    }

    public static final String DEFAULT_JIRA_BASEURL = "https://issues.apache.org/jira";
    public static final String DEFAULT_JIRA_USERNAME = null;
    public static final String DEFAULT_JIRA_PASSWORD = null;
    public static final String DEFAULT_PROJECT = "JSPWIKI";
    public static final String DEFAULT_JQL = "status = open order by key DESC";
    public static final int DEFAULT_MAX = 10;
    public static final int UPPERLIMIT_MAX = 50;
    public static final int DEFAULT_START = 0;
    public static final String DEFAULT_CLASS = "jira-table";

    private static final String PROP_JIRA_BASEURL = "jira.baseurl";
    private static final String PROP_JIRA_USERNAME = "jira.username";
    private static final String PROP_JIRA_PASSWORD = "jira.password";
    private static final String PARAM_PROJECT = "project";
    private static final String PARAM_JQL = "jql";
    private static final String PARAM_MAX = "max";
    private static final String PARAM_START = "start";
    private static final String PARAM_CLASS = "class";

    private String jiraBaseUrl = DEFAULT_JIRA_BASEURL;
    private String jiraUsername = DEFAULT_JIRA_USERNAME;
    private String jiraPassword = DEFAULT_JIRA_PASSWORD;
    private String project = DEFAULT_PROJECT;
    private String jql = DEFAULT_JQL;
    private int max = DEFAULT_MAX;
    private int start = DEFAULT_START;
    private String className = DEFAULT_CLASS;

    private static Map<String, String> iconImageMapCache = new HashMap<String, String>();

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        setLogForDebug(params.get(PluginManager.PARAM_DEBUG));
        log.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();
        Engine engine = wikiContext.getEngine();
        Properties props = engine.getWikiProperties();

        // Validate all parameters
        validateParams(props, params);

        try {
            JiraRestClient restClient = getRestClient(jiraBaseUrl, jiraUsername, jiraPassword);

            List<Issue> issues = doJQLSearch(restClient, project, max, start, jql);
            if (!issues.isEmpty()) {
                buffer.append("|| Key || Priority || Type || Summary || Status || Resolution || Assignee || Reporter || Comments");
                buffer.append("\n");
            }
            for (Issue issue : issues) {
                buffer.append(getIssueAsWikiText(restClient, jiraBaseUrl, issue));
                buffer.append("\n");
            }
            log.info("result=" + result);
            result = engine.getManager(RenderingManager.class).
                    textToHTML(wikiContext, buffer.toString());

            result = "<div class='" + className + "'>" + result + "</div>";
        } catch (Throwable e) {
            log.error("ERROR: " + e.getMessage() + ". jql=" + jql, e);
            throw new PluginException(e.getMessage());
        }

        return result;
    }

    protected void validateParams(Properties props, Map<String, String> params) throws PluginException {
        String paramName;
        String param;

        log.info("validateParams() START");
        paramName = PROP_JIRA_BASEURL;
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            jiraBaseUrl = param;
        }
        paramName = PROP_JIRA_USERNAME;
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            jiraUsername = param;
        }
        paramName = PROP_JIRA_PASSWORD;
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            jiraPassword = param;
        }
        paramName = PARAM_PROJECT;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            project = param;
        }
        paramName = PARAM_JQL;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            jql = param;
        }
        paramName = PARAM_MAX;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            max = Integer.parseInt(param);
            if (max > UPPERLIMIT_MAX) {
                max = UPPERLIMIT_MAX;
            }
        }
        paramName = PARAM_START;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            log.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            start = Integer.parseInt(param);
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
        log.info("validateParams() DONE");
    }

    public static JiraRestClient getRestClient(String url) throws URISyntaxException {
        return getRestClient(url, null, null);
    }

    public static JiraRestClient getRestClient(String url, String username, String password) throws URISyntaxException {
        final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        final URI jiraServerUri = new URI(url);
        AuthenticationHandler auth = new AnonymousAuthenticationHandler();
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            auth = new BasicHttpAuthenticationHandler(username, password);
        }
        final JiraRestClient restClient = factory.create(jiraServerUri, auth);
        return restClient;
    }

    public static List<Issue> doJQLSearch(JiraRestClient restClient, String project, String jqlParam) {
        return doJQLSearch(restClient, project, 10, 0, jqlParam);
    }

    public static List<Issue> doJQLSearch(JiraRestClient restClient, String project, int max, int start, String jqlParam) {
        log.info("doJSQLSearch() project=" + project + " max=" + max + " start=" + start + " jqlParam=" + jqlParam);
        List<Issue> issues = new ArrayList<Issue>();
        String jql = "project = " + project + " and " + jqlParam;
        log.debug("final jql=" + jql);
        SearchRestClient searchRestClient = restClient.getSearchClient();
        SearchResult res = searchRestClient.searchJql(jql, max, start, null).claim();
        log.debug("res=" + res);
        for (BasicIssue iss : res.getIssues()) {
            Issue issue = restClient.getIssueClient().getIssue(iss.getKey()).claim();
            if (issue != null) {
                issues.add(issue);
            }
        }
        log.info("doJSQLSearch() found " + issues.size() + " issues");
        return issues;
    }

    public static String getIconUrl(JiraRestClient jiraRestClient, MetadataType type, URI uri) throws JSONException {
        switch (type) {
            case PRIORITY:
                Priority priority = jiraRestClient.getMetadataClient().getPriority(uri).claim();
                return priority.getIconUri().toString();
            case STATUS:
                com.atlassian.jira.rest.client.api.domain.Status status = jiraRestClient.getMetadataClient().getStatus(uri).claim();
                return status.getIconUrl().toString();

            case ISSUETYPE:
                IssueType issueType = jiraRestClient.getMetadataClient().getIssueType(uri).claim();
                return issueType.getIconUri().toString();
        }
        return null;
    }

    public static String getIssueAsWikiText(JiraRestClient jiraRestClient, String jiraBaseUrl, Issue issue) throws JSONException {
        if (issue == null) {
            return "";
        }
        String DELIM = " | ";
        String link = "[" + issue.getKey() + "|" + jiraBaseUrl + "/browse/" + issue.getKey() + "]";
        String priority = issue.getPriority() == null ? "" : "[" + getCachedIconUrl(jiraRestClient, MetadataType.PRIORITY, issue.getPriority().getSelf()) + "]";
        String status = issue.getStatus() == null ? "" : "[" + getCachedIconUrl(jiraRestClient, MetadataType.PRIORITY, issue.getStatus().getSelf()) + "]";
        String resolution = issue.getResolution() == null ? "" : issue.getResolution().getName();
        String type = issue.getIssueType() == null ? "" : "[" + getCachedIconUrl(jiraRestClient, MetadataType.PRIORITY, issue.getIssueType().getSelf()) + "]";
        String summary = issue.getSummary();
        String assignee = issue.getAssignee() == null ? "" : issue.getAssignee().getDisplayName();
        String reporter = issue.getReporter() == null ? "" : issue.getReporter().getDisplayName();
        String comments = Integer.toString(countComments(issue));
        String result = "| " + link + DELIM + priority + DELIM + type + DELIM + summary + DELIM + status + DELIM + resolution + DELIM + assignee + DELIM + reporter + DELIM + comments;
        log.debug("result=" + result);
        return result;
    }

    public static int countComments(Issue issue) {
        int i = 0;
        if (issue == null) {
            return 0;
        }
        Iterable<Comment> comments = issue.getComments();
        for (Comment comment : comments) {
            i++;
        }
        log.debug("Found +" + i + " comments for issue " + issue.getKey());
        return i;
    }

    public static String getCachedIconUrl(JiraRestClient jiraRestClient, MetadataType type, URI url) throws JSONException {
        if (iconImageMapCache.containsKey(url.toString())) {
            return iconImageMapCache.get(url.toString());
        }
        String iconUrl = getIconUrl(jiraRestClient, type, url);
        if (iconUrl != null) {
            iconImageMapCache.put(url.toString(), iconUrl);
        }
        return iconUrl;
    }

    private void setLogForDebug(String value) {
        if (StringUtils.isNotBlank(value) && (value.equalsIgnoreCase("true") || value.equals("1"))) {
            log.setLevel(Level.INFO);
        }
    }
}
