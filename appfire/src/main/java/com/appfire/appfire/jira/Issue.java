package com.appfire.appfire.jira;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Issue {
    public String summary;
    public String key;
    public String url;
    public String type;
    public String priority;
    public String description;
    public String reporter;
    public String createDate;
    public List<Comment> comments;

}
