package com.digitalspider.jspwiki.plugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

import com.atlassian.jira.rest.client.AuthenticationHandler;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.SearchRestClient;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Comment;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

/**
 * Use [{Jira project=JSPWIKI jql='status = open'}]
 * @author vittord
 */
public class JiraPlugin implements WikiPlugin {
	
	private static final Logger log = Logger.getLogger(JiraPlugin.class);
	
	public static final String DEFAULT_JIRA_BASEURL = "https://issues.apache.org/jira";
	public static final String DEFAULT_JIRA_USERNAME = null;
	public static final String DEFAULT_JIRA_PASSWORD = null;
	public static final String DEFAULT_PROJECT = "JSPWIKI";
	public static final String DEFAULT_JQL = "status = open order by key DESC";
	public static final int DEFAULT_MAX = 10;
	public static final int UPPERLIMIT_MAX = 50;
	public static final int DEFAULT_START = 0;
	
	private static final String PROP_JIRA_BASEURL = "jira.baseurl";
	private static final String PROP_JIRA_USERNAME = "jira.username";
	private static final String PROP_JIRA_PASSWORD = "jira.password";
	private static final String PARAM_PROJECT = "project";
	private static final String PARAM_JQL = "jql";
	private static final String PARAM_MAX = "max";
	private static final String PARAM_START = "start";
	
	private String jiraBaseUrl = DEFAULT_JIRA_BASEURL;
	private String jiraUsername = DEFAULT_JIRA_USERNAME;
	private String jiraPassword = DEFAULT_JIRA_PASSWORD;
	private String project = DEFAULT_PROJECT;
	private String jql = DEFAULT_JQL;
	private int max = DEFAULT_MAX;
	private int start = DEFAULT_START;

	public static Map<String,String> statusImageMap = new HashMap<String, String>();
	
	static {
		statusImageMap.put("Closed", "https://issues.apache.org/jira/images/icons/statuses/closed.png");
		statusImageMap.put("Open", "https://issues.apache.org/jira/images/icons/statuses/open.png");
		statusImageMap.put("In Progress", "https://issues.apache.org/jira/images/icons/statuses/inprogress.png");
		statusImageMap.put("Reopened", "https://issues.apache.org/jira/images/icons/statuses/reopened.png");
		statusImageMap.put("Resolved", "https://issues.apache.org/jira/images/icons/statuses/resolved.png");
	}
	
	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
		log.info("STARTED");
		StringBuffer result = new StringBuffer();
		
		// Validate all parameters
		validateParams(wikiContext,params);
		
		try {
			JiraRestClient restClient = getRestClient(jiraBaseUrl,jiraUsername,jiraPassword);
			
			List<Issue> issues = doJQLSearch(restClient, project, max, start, jql);
			if (!issues.isEmpty()) {
				result.append("|| Key || Priority || Type || Summary || Status || Resolution || Assignee || Reporter || Comments ||");
				result.append("<br/>");
			}
			for (Issue issue : issues) {
				result.append(getIssueAsWikiText(jiraBaseUrl,issue));
				result.append("<br/>");
			}
		} catch (Throwable e) {
			log.error(e,e);
			throw new PluginException("ERROR: "+e);
		}
		
		return result.toString();
	}

	protected void validateParams(WikiContext wikiContext, Map<String, String> params) throws PluginException {
		String paramName;
		String param;
		
		log.info("validateParams() START");
		paramName = PROP_JIRA_BASEURL;
		param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isAsciiPrintable(param)) {
				throw new PluginException(paramName+" parameter is not a valid value");
			}
			jiraBaseUrl = param;
		}
		paramName = PROP_JIRA_USERNAME;
		param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isAsciiPrintable(param)) {
				throw new PluginException(paramName+" parameter is not a valid value");
			}
			jiraUsername = param;
		}
		paramName = PROP_JIRA_PASSWORD;
		param = wikiContext.getEngine().getWikiProperties().getProperty(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isAsciiPrintable(param)) {
				throw new PluginException(paramName+" parameter is not a valid value");
			}
			jiraPassword = param;
		}
		paramName = PARAM_PROJECT;
		param = params.get(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isAsciiPrintable(param)) {
				throw new PluginException(paramName+" parameter is not a valid value");
			}
			project = param;
		}
		paramName = PARAM_JQL;
		param = params.get(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isAsciiPrintable(param)) {
				throw new PluginException(paramName+" parameter is not a valid value");
			}
			jql = param;
		}
		paramName = PARAM_MAX;
		param = params.get(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isNumeric(param)) {
				throw new PluginException(paramName+" parameter is not a valid value");
			}
			max = Integer.parseInt(param);
			if (max > UPPERLIMIT_MAX) {
				max = UPPERLIMIT_MAX;
			}
		}
		paramName = PARAM_START;
		param = params.get(paramName);
		if (StringUtils.isNotBlank(param)) {
			log.info(paramName+"="+param);
			if (!StringUtils.isNumeric(param)) {
				throw new PluginException(paramName+" parameter is not a valid value");
			}
			start = Integer.parseInt(param);
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
        	auth = new BasicHttpAuthenticationHandler(username,password);
        }
        final JiraRestClient restClient = factory.create(jiraServerUri, auth);
        return restClient;
	}
	
	public static List<Issue> doJQLSearch(JiraRestClient restClient, String project, String jqlParam) {
		return doJQLSearch(restClient, project, 10, 0, jqlParam);
	}

	public static List<Issue> doJQLSearch(JiraRestClient restClient, String project, int max, int start, String jqlParam) {
		log.info("doJSQLSearch() project="+project+" max="+max+" start="+start+" jqlParam="+jqlParam);
		List<Issue> issues = new ArrayList<Issue>();
		String jql = "project = "+project + " and "+jqlParam;
		log.debug("final jql="+jql);
        SearchRestClient searchRestClient = restClient.getSearchClient();
        SearchResult res = searchRestClient.searchJql(jql, max, start).claim();
        log.debug("res="+res);
        for (BasicIssue iss : res.getIssues()) {
        	Issue issue = restClient.getIssueClient().getIssue(iss.getKey()).claim();
        	if (issue != null) {
        		issues.add(issue);
        	}
        }
        log.info("doJSQLSearch() found "+issues.size()+" issues");
		return issues;
	}
	
	public static String getIssueAsWikiText(String jiraBaseUrl, Issue issue) {
		if (issue == null) {
			return "";
		}
		String DELIM = " | ";
		String link = "<a href='"+jiraBaseUrl+"/browse/"+issue.getKey()+"'>"+issue.getKey()+"</a>";
		String priority = issue.getPriority() == null ? "" : issue.getPriority().getName();
		String status = issue.getStatus() == null ? "" : issue.getStatus().getName();
		String resolution = issue.getResolution() == null ? "" : issue.getResolution().getName();
		String type = issue.getIssueType() == null ? "" : issue.getIssueType().getName();
		String summary = issue.getSummary();
		String assignee = issue.getAssignee() == null ? "" : issue.getAssignee().getDisplayName();
		String reporter = issue.getReporter() == null ? "" : issue.getReporter().getDisplayName();
		String comments = Integer.toString(countComments(issue));
		String result = "| "+link+DELIM+priority+DELIM+type+DELIM+summary+DELIM+status+DELIM+resolution+DELIM+assignee+DELIM+reporter+DELIM+comments+" |";
		log.debug("result="+result);
		return result;
	}
	
	public static int countComments(Issue issue) {
		int i = 0 ;
		if (issue == null) {
			return 0;
		}
		Iterable<Comment> comments = issue.getComments();
		for (Comment comment : comments) {
			i++;
		}
		log.debug("Found +"+i+" comments for issue "+issue.getKey());
		return i;
	}
}
