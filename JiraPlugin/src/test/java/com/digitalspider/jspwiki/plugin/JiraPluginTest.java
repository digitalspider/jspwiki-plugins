package com.digitalspider.jspwiki.plugin;

import java.net.URISyntaxException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.domain.Issue;

public class JiraPluginTest extends TestCase {

	Logger log = Logger.getLogger(JiraPluginTest.class);
	JiraRestClient restClient;
	
    public void setUp() throws Exception {
    	restClient = JiraPlugin.getRestClient(JiraPlugin.DEFAULT_JIRA_BASEURL);
    }
    
	public void tearDown() throws Exception {
		restClient = null;
	}
	
	public void testJiraConnection() throws URISyntaxException {
        assertNotNull(restClient);
        Issue issue1 = restClient.getIssueClient().getIssue("JSPWIKI-864").claim();
        assertNotNull(issue1);
        assertEquals("JSPWIKI-864",issue1.getKey());
//        System.out.println("issue ="+issue1.getKey()+" "+issue1.getSummary());
	}
	
	public void testSearch() throws URISyntaxException {
        int max = 10;
        int start = 0;
        List<Issue> issues = JiraPlugin.doJQLSearch(restClient, "JSPWIKI", max, start, JiraPlugin.DEFAULT_JQL);

        assertEquals(max,issues.size());
        for (Issue issue : issues) {
        	assertNotNull(issue);
        	assertNotNull(issue.getKey());
        	assertNotNull(issue.getSummary());
        	assertNotNull(issue.getSelf());
        	
        	assertEquals("Open",issue.getStatus().getName());
        	log.info(issue.getKey()+" "+issue.getSummary()+" "+issue.getStatus());
        }
	}
	
	public void testPrintIssue() {
        Issue issue = restClient.getIssueClient().getIssue("JSPWIKI-123").claim();
        assertNotNull(issue);
        log.debug(issue.getKey()+" "+issue.getSummary()+" "+issue.getStatus().getName());
        log.trace(issue);
        // | ID | Type | Priority | Summary | Status | Resolution | Assignee | Reporter | Comments
        String expected = "| <a href='https://issues.apache.org/jira/browse/JSPWIKI-123'>JSPWIKI-123</a> | Minor | Improvement | missing german date format | Closed | Fixed |  | Florian Holeczek | 11 |";
        String actual = JiraPlugin.getIssueAsWikiText(JiraPlugin.DEFAULT_JIRA_BASEURL,issue);
        assertEquals(expected, actual);
	}
	
}