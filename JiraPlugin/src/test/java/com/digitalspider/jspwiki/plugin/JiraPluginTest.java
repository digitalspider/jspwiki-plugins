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

import java.net.URISyntaxException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.digitalspider.jspwiki.plugin.JiraPlugin.MetadataType;

public class JiraPluginTest extends TestCase {

    Logger log = Logger.getLogger(JiraPluginTest.class);
    JiraRestClient restClient;

    public void setUp() throws Exception {
        restClient = JiraPlugin.getRestClient(JiraPlugin.DEFAULT_JIRA_BASEURL);
    }

    public void tearDown() throws Exception {
        restClient = null;
    }

    public void testJiraConnection() throws URISyntaxException, JSONException {
        assertNotNull(restClient);
        Issue issue1 = restClient.getIssueClient().getIssue("JSPWIKI-864").claim();
        assertNotNull(issue1);
        assertEquals("JSPWIKI-864", issue1.getKey());
        String expected = "https://issues.apache.org/jira/images/icons/statuses/closed.png";
        String iconUrl = JiraPlugin.getIconUrl(restClient, MetadataType.STATUS, issue1.getStatus().getSelf());
        assertEquals(expected, iconUrl);
//        System.out.println("issue ="+issue1.getKey()+" "+issue1.getSummary());
    }

    public void testSearch() throws URISyntaxException {
        int max = 10;
        int start = 0;
        List<Issue> issues = JiraPlugin.doJQLSearch(restClient, "JSPWIKI", max, start, JiraPlugin.DEFAULT_JQL);

        assertEquals(max, issues.size());
        for (Issue issue : issues) {
            assertNotNull(issue);
            assertNotNull(issue.getKey());
            assertNotNull(issue.getSummary());
            assertNotNull(issue.getSelf());

            assertEquals("Open", issue.getStatus().getName());
            log.info(issue.getKey() + " " + issue.getSummary() + " " + issue.getStatus());
        }
    }

    public void testPrintIssue() throws JSONException {
        Issue issue = restClient.getIssueClient().getIssue("JSPWIKI-123").claim();
        assertNotNull(issue);
        log.debug(issue.getKey() + " " + issue.getSummary() + " " + issue.getStatus().getName());
        log.trace(issue);
        // | ID | Type | Priority | Summary | Status | Resolution | Assignee | Reporter | Comments
        String expected = "| [JSPWIKI-123|https://issues.apache.org/jira/browse/JSPWIKI-123] | [https://issues.apache.org/jira/images/icons/priorities/minor.svg] | [https://issues.apache.org/jira/secure/viewavatar?size=xsmall&avatarId=21140&avatarType=issuetype] | missing german date format | [https://issues.apache.org/jira/images/icons/statuses/closed.png] | Fixed |  | Florian Holeczek | 11";
        String actual = JiraPlugin.getIssueAsWikiText(restClient, JiraPlugin.DEFAULT_JIRA_BASEURL, issue);
        //System.out.println(actual);
        assertEquals(expected, actual);
    }

}
