package com.appfire.appfire.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataExtractorTest {

    private RestTemplate restTemplate;
    private DataExtractor dataExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode mockIssueResponse;
    private JsonNode mockCommentResponse;

    @BeforeEach
    void setUp() throws IOException {
        restTemplate = mock(RestTemplate.class);
        dataExtractor = new DataExtractor();
        DataExtractor.restTemplate = restTemplate;

        String issueJson = """
            {
                "total": 1,
                "issues": [
                    {
                        "key": "JIRA-123",
                        "fields": {
                            "summary": "Test issue",
                            "issuetype": { "name": "Bug" },
                            "priority": "High",
                            "description": "description",
                            "created": "2024-03-30T10:15:30Z",
                            "reporter": { "displayName": "Bai Ivan" }
                        }
                    }
                ]
            }
        """;
        mockIssueResponse = objectMapper.readTree(issueJson);

        String commentJson = """
            {
                "comments": [
                    {
                        "author": { "displayName": "Bai Ivan" },
                        "body": "comment"
                    }
                ]
            }
        """;
        mockCommentResponse = objectMapper.readTree(commentJson);
    }

    @Test
    void fetchIssues_ShouldReturnListOfIssues() {
        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(mockIssueResponse);

        List<Issue> issues = dataExtractor.fetchIssues();

        assertNotNull(issues);
        assertEquals(1, issues.size());
        assertEquals("JIRA-123", issues.get(0).key);
        assertEquals("Test issue", issues.get(0).summary);
        assertEquals("Bug", issues.get(0).type);


    }

    @Test
    void fetchCommentsByIssue_ShouldReturnNonEmptyListOfComments() {
        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(mockCommentResponse);

        List<Comment> comments = dataExtractor.fetchCommentsByIssue("JIRA-123");

        assertNotNull(comments);
        assertEquals(1, comments.size());
        assertEquals("Bai Ivan", comments.get(0).author);
        assertEquals("comment", comments.get(0).text);
    }

    @Test
    void fetchIssues_ShouldHandleNullResponse() {
        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(null);

        List<Issue> issues = dataExtractor.fetchIssues();

        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    void fetchCommentsByIssue_ShouldHandleNullResponse() {
        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(null);

        List<Comment> comments = dataExtractor.fetchCommentsByIssue("JIRA-123");

        assertNotNull(comments);
        assertTrue(comments.isEmpty());
    }
}
