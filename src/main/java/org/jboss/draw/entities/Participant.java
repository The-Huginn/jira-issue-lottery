package org.jboss.draw.entities;

import io.smallrye.mutiny.tuples.Tuple2;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Participant {

    private final String email;
    private final Map<String, Tuple2<Integer, Set<String>>> projectComponentsMap;

    public Participant(String email, Map<String, Tuple2<Integer, Set<String>>> projectComponentsMap) {
        this.email = email;
        this.projectComponentsMap = projectComponentsMap;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Participant that = (Participant) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    public void assignIssue(Issue issue) {
        issue.setAssignee(this);
        Tuple2<Integer, Set<String>> entry = projectComponentsMap.get(issue.getProject());
        projectComponentsMap.put(issue.getProject(), Tuple2.of(entry.getItem1() - 1, entry.getItem2()));
    }

    public Tuple2<Integer, Set<String>> getProjectComponents(String project) {
        return projectComponentsMap.getOrDefault(project, Tuple2.of(0, Collections.emptySet()));
    }
}
