package com.appfire.appfire.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataExtractor {
    private static final String JIRA_REST_API = "https://jira.atlassian.com/rest/api/2/search?jql=issuetype in (Bug,Documentation,Enhancement) and updated > startOfWeek()";
    private static final String JIRA_BASE_BROWSE_URL = "https://jira.atlassian.com/browse/";
    static RestTemplate restTemplate = new RestTemplate();

    private static List<Issue> fetchIssues() {

        JsonNode response = restTemplate.getForObject(JIRA_REST_API, JsonNode.class);
        List<Issue> issues = new ArrayList<>();

        if (response != null && response.has("issues")) {
            for (JsonNode issueNode : response.get("issues")) {
                Issue issue = new Issue();
                issue.key = issueNode.get("key").asText();
                issue.url = JIRA_BASE_BROWSE_URL + issue.key;
                issue.summary = issueNode.get("fields").get("summary").asText();
                issue.type = issueNode.get("fields").get("issuetype").get("name").asText();
                issue.priority = issueNode.get("fields").get("priority") != null ? issueNode.get("fields").get("priority").get("name").asText() : "N/A";
                issue.description = issueNode.get("fields").get("description") != null ? issueNode.get("fields").get("description").asText() : "No Description";
                issue.reporter = issueNode.get("fields").get("reporter").get("name").asText();
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
                issues.add(issue);
            }
        }
        return issues;
    }

    public static void saveResultAsJson(List<Issue> issues) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        File file = new File("jira_json_issues" + "json");
        mapper.writeValue(file, issues);
        System.out.println("File path = " + file.getAbsolutePath());
    }

    public static void main(String[] args) throws IOException {
        List<Issue> issues = fetchIssues();
        saveResultAsJson(issues);
    }


}
