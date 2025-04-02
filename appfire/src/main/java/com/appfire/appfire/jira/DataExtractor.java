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
import java.util.concurrent.*;

public class DataExtractor {
    private static final String JIRA_REST_API = "https://jira.atlassian.com/rest/api/2/search";
    private static final String JIRA_BASE_BROWSE_URL = "https://jira.atlassian.com/browse/";
    private static final int PAGE_SIZE = 50;
    private static int MAX_RESULTS = 50;

    static RestTemplate restTemplate = new RestTemplate();

    public static List<Issue> fetchIssues() {
        List<Issue> allIssues = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int startAt = 0; startAt < MAX_RESULTS; startAt += PAGE_SIZE) {
                int finalStartAt = startAt;
                futures.add(executor.submit(() -> {
                    fetchIssuesBatch(finalStartAt, allIssues);
                    return null;
                }));
            }
            for (Future<Void> future : futures) future.get(); // Wait for all threads
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        return allIssues;
    }


    private static void fetchIssuesBatch(int startAt, List<Issue> allIssues) {
        String url = JIRA_REST_API + "?jql=issuetype in (Bug,Documentation,Enhancement) and updated > startOfWeek()"
                + "&startAt=" + startAt + "&maxResults=" + PAGE_SIZE;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        if (response == null || !response.has("issues")) {
            return;
        }

        response.get("issues").forEach(issueNode -> {
            Issue issue = new Issue();
            issue.key = issueNode.path("key").asText();
            issue.url = JIRA_BASE_BROWSE_URL + issue.key;
            issue.summary = issueNode.path("fields").path("summary").asText();
            issue.type = issueNode.path("fields").path("issuetype").path("name").asText();
            issue.priority = issueNode.path("fields").path("priority").asText("Unspecified");
            issue.description = issueNode.path("fields").path("description").asText();
            issue.reporter = issueNode.path("fields").path("reporter").path("displayName").asText("No reporter");
            issue.createDate = issueNode.path("fields").path("created").asText();

            issue.comments = fetchCommentsByIssue(issue.key);
            allIssues.add(issue);
        });
    }

    public static List<Comment> fetchCommentsByIssue(String issueKey) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Comment> comments = new CopyOnWriteArrayList<>();

        executor.submit(() -> {
            String url = "https://jira.atlassian.com/rest/api/2/issue/" + issueKey + "/comment";
            JsonNode commentNode = restTemplate.getForObject(url, JsonNode.class);
            if (commentNode != null && commentNode.has("comments")) {
                commentNode.get("comments").forEach(comment -> {
                    Comment currentComment = new Comment();
                    currentComment.author = comment.path("author").path("displayName").asText();
                    currentComment.text = comment.path("body").asText();
                    comments.add(currentComment);
                });
            }
        });

        executor.shutdown();
        return comments;
    }

    private static int totalIssuesCount() {
        String url = JIRA_REST_API + "?jql=issuetype in (Bug,Documentation,Enhancement) and updated > startOfWeek()";
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        if (response == null || !response.has("total")) {
            return MAX_RESULTS; //default value is limited by Jira to 50
        }
        return MAX_RESULTS = response.get("total").asInt();
    }

    public static void saveResultAsJson(List<Issue> issues) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        File file = new File("Jira_Issues_" + System.currentTimeMillis() + ".json");
        mapper.writeValue(file, issues);
        System.out.println("File path = " + file.getAbsolutePath());
    }

    public static void saveResultAsXML(List<Issue> issues) throws IOException {
        ObjectMapper mapper = new XmlMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        File file = new File("Jira_Issues_" + System.currentTimeMillis() + ".xml");
        mapper.writeValue(file, issues);
        System.out.println("File path = " + file.getAbsolutePath());
    }

    public static void main(String[] args) throws IOException {
        long startTimeInMilis = System.currentTimeMillis();
        System.out.println("Total issue count = " + totalIssuesCount());
        List<Issue> issues = fetchIssues();
        saveResultAsJson(issues);
        saveResultAsXML(issues);
        long endTimeInMilis = System.currentTimeMillis();
        double timeInSeconds = (double) (endTimeInMilis - startTimeInMilis) / 1000;
        System.out.println("Time to proceed using virtual threads = " + timeInSeconds + " seconds.");
    }
}
