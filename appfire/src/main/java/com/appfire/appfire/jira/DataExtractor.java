package com.appfire.appfire.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataExtractor {
    //private static final String JIRA_REST_API = "https://jira.atlassian.com/rest/api/2/search?jql=issuetype in (Bug,Documentation,Enhancement) and updated > startOfWeek()";
    // private static final String JIRA_REST_API = "https://jira.atlassian.com/rest/api/2/search?jql=issuetype in (Bug,Documentation,Enhancement) and updated > startOfYear()";
    private static final String JIRA_REST_API = "https://jira.atlassian.com/rest/api/2/search";
    private static final String JIRA_BASE_BROWSE_URL = "https://jira.atlassian.com/browse/";
    private static final int PAGE_SIZE = 50;

    static RestTemplate restTemplate = new RestTemplate();
    static int startAt = 0;
    static int totalIssues = Integer.MAX_VALUE;
    static int restCalls = 0;


    private static List<Issue> fetchIssues() {
        List<Issue> allIssues = new ArrayList<>();

        while (startAt < totalIssues) {
            String url = JIRA_REST_API + "?jql=issuetype in (Bug,Documentation,Enhancement) and updated > startOfWeek()"
                    + "&startAt=" + startAt + "&maxResults=" + PAGE_SIZE;
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response == null || !response.has("issues")) {
                break;
            }
            totalIssues = response.get("total").asInt();
            System.out.println("Total issues = " + totalIssues); //not great place to count, but 1 less call to API
            restCalls++;

            if (response.has("total")) {
                totalIssues = response.get("total").asInt();
            }

            for (JsonNode issueNode : response.get("issues")) {
                Issue issue = new Issue();
                issue.key = issueNode.get("key").asText();
                issue.url = JIRA_BASE_BROWSE_URL + issue.key;
                issue.summary = issueNode.get("fields").get("summary").asText();
                issue.type = issueNode.get("fields").get("issuetype").get("name").asText();

                //I assume we want to display priority as String / Low, High, Critical ... etc
                issue.priority = issueNode.get("fields").get("priority").asText();
                issue.description = issueNode.get("fields").get("description").asText();
                //reporter name is unreadable like "name -> {TextNode@4585} ""5479fe2c9e8b"

                if (response.has("reporter")) {
                    issue.reporter = issueNode.get("fields").get("reporter") != null ? issueNode.get("fields").get("displayName").asText() : "No reporter";

                }
                issue.createDate = issueNode.get("fields").get("created").asText();

                List<Comment> comments = new ArrayList<>();
                JsonNode commentNode = issueNode.get("fields").get("comment");
                if (commentNode != null && commentNode.has("comments")) {
                    for (JsonNode comment : commentNode.get("comments")) {
                        Comment currentComment = new Comment();
                        currentComment.author = comment.get("author").get("name").asText();
                        currentComment.text = comment.get("body").asText();
                        comments.add(currentComment);
                    }
                }
                issue.comments = comments;
                allIssues.add(issue);
            }
            startAt += PAGE_SIZE;
        }
        System.out.println(restCalls);
        return allIssues;
    }


    public static void saveResultAsJson(List<Issue> issues) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        File file = new File("Jira_Issues_" + System.currentTimeMillis() + ".json");
        mapper.writeValue(file, issues);
        System.out.println("File path = " + file.getAbsolutePath());
    }


    //almost the same method
    public static void saveResultAsXML(List<Issue> issues) throws IOException {
        ObjectMapper mapper = new XmlMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        File file = new File("Jira_Issues_" + System.currentTimeMillis() + ".xml");
        mapper.writeValue(file, issues);
        System.out.println("File path = " + file.getAbsolutePath());
    }

    public static void main(String[] args) throws IOException {
        List<Issue> issues = fetchIssues();
        saveResultAsJson(issues);
//    saveResultAsXML(issues);
    }


}
