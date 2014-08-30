package com.digitalspider.jspwiki.plugin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.render.XHTMLRenderer;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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

	public static Map<String,String> iconImageMapCache = new HashMap<String, String>();
	
	static {
		iconImageMapCache.put("https://issues.apache.org/jira/rest/api/2/priority/1", "https://issues.apache.org/jira/images/icons/priorities/blocker.png");
		iconImageMapCache.put("https://issues.apache.org/jira/rest/api/2/priority/2", "https://issues.apache.org/jira/images/icons/priorities/critical.png");
		iconImageMapCache.put("https://issues.apache.org/jira/rest/api/2/priority/3", "https://issues.apache.org/jira/images/icons/priorities/major.png");
		iconImageMapCache.put("https://issues.apache.org/jira/rest/api/2/priority/4", "https://issues.apache.org/jira/images/icons/priorities/minor.png");
		iconImageMapCache.put("https://issues.apache.org/jira/rest/api/2/priority/3", "https://issues.apache.org/jira/images/icons/priorities/trivial.png");
	}
	
	@Override
	public String execute(WikiContext wikiContext, Map<String, String> params) throws PluginException {
		log.info("STARTED");
		String result = "";
		StringBuffer buffer = new StringBuffer();
		
		// Validate all parameters
		validateParams(wikiContext,params);
		
		try {
			JiraRestClient restClient = getRestClient(jiraBaseUrl,jiraUsername,jiraPassword);
			
			List<Issue> issues = doJQLSearch(restClient, project, max, start, jql);
			if (!issues.isEmpty()) {
				buffer.append("|| Key || Priority || Type || Summary || Status || Resolution || Assignee || Reporter || Comments");
				buffer.append("\n");
//				buffer.append("<br/>");
			}
			for (Issue issue : issues) {
				buffer.append(getIssueAsWikiText(jiraBaseUrl,issue));
				buffer.append("\n");
//				buffer.append("<br/>");
			}
			Reader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes())));
			JSPWikiMarkupParser parser = new JSPWikiMarkupParser(wikiContext, in);
			WikiDocument doc = parser.parse();
			log.debug("doc="+doc);
			XHTMLRenderer renderer = new XHTMLRenderer(wikiContext, doc);
			result = renderer.getString();
			//result += "<br/><br/>"+buffer.toString();
		} catch (Throwable e) {
			log.error(e,e);
			throw new PluginException("ERROR: "+e);
		}
		
		return result;
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
	
	public static String getIconUrl(String url) throws JSONException {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = parser.getJSONFromUrl(url);
		String iconUrl = (String)jsonObject.get("iconUrl");
		String name = (String)jsonObject.get("name");
		String id = (String)jsonObject.get("id");
		return iconUrl;
	}

	public static String getIssueAsWikiText(String jiraBaseUrl, Issue issue) throws JSONException {
		if (issue == null) {
			return "";
		}
		String DELIM = " | ";
//		String link = "<a href='"+jiraBaseUrl+"/browse/"+issue.getKey()+"'>"+issue.getKey()+"</a>";
		String link = "["+issue.getKey()+"|"+jiraBaseUrl+"/browse/"+issue.getKey()+"]";
		String priority = issue.getPriority() == null ? "" : "["+ getCachedIconUrl(issue.getPriority().getSelf().toString()) + "]";
		String status = issue.getStatus() == null ? "" : "["+ getCachedIconUrl(issue.getStatus().getSelf().toString()) + "]";
		String resolution = issue.getResolution() == null ? "" : issue.getResolution().getName();
		String type = issue.getIssueType() == null ? "" : "["+ getCachedIconUrl(issue.getIssueType().getSelf().toString()) + "]";
		String summary = issue.getSummary();
		String assignee = issue.getAssignee() == null ? "" : issue.getAssignee().getDisplayName();
		String reporter = issue.getReporter() == null ? "" : issue.getReporter().getDisplayName();
		String comments = Integer.toString(countComments(issue));
		String result = "| "+link+DELIM+priority+DELIM+type+DELIM+summary+DELIM+status+DELIM+resolution+DELIM+assignee+DELIM+reporter+DELIM+comments;
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
	
	public static String getCachedIconUrl(String url) throws JSONException {
		if (iconImageMapCache.containsKey(url)) {
			return iconImageMapCache.get(url);
		}
		String iconUrl = getIconUrl(url);
		iconImageMapCache.put(url,iconUrl);
		return iconUrl;
	}
	

	public static class JSONParser {
		Logger log = Logger.getLogger(JSONParser.class);
		InputStream is = null;
		JSONObject jObj = null;
		String json = "";
		
		// constructor
		public JSONParser() {
		
		}
		
		public JSONObject getJSONFromUrl(String url) {
		
		    // Making HTTP request
		    try {
		        // defaultHttpClient
		        DefaultHttpClient httpClient = new DefaultHttpClient();
		        HttpGet httpGet = new HttpGet(url);
		
		        HttpResponse httpResponse = httpClient.execute(httpGet);
		        HttpEntity httpEntity = httpResponse.getEntity();
		        is = httpEntity.getContent();
		
		    } catch (UnsupportedEncodingException e) {
		        e.printStackTrace();
		    } catch (ClientProtocolException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		
		    try {
		        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
		        StringBuilder sb = new StringBuilder();
		        String line = null;
		        while ((line = reader.readLine()) != null) {
		            sb.append(line + "\n");
//		            System.out.println(line);
		        }
		        is.close();
		        json = sb.toString();
		
		    } catch (Exception e) {
		        log.error("Error converting result " + e.toString());
		    }
		
		    // try parse the string to a JSON object
		    try {
		        jObj = new JSONObject(json);
		    } catch (JSONException e) {
		        log.error("Error parsing data " + e.toString());
		        System.out.println("error on parse data in jsonparser.java");
		    }
		
		    // return JSON String
		    return jObj;
		
		}
	}
}
