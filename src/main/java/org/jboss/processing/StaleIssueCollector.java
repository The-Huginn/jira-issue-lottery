package org.jboss.processing;

import com.atlassian.jira.rest.client.api.domain.Issue;
import io.quarkus.logging.Log;
import org.apache.camel.component.jira.JiraEndpoint;
import org.apache.camel.component.jira.consumer.NewIssuesConsumer;
import org.jboss.jql.JqlBuilder;
import org.jboss.processing.state.EveryIssueState;
import org.jboss.processing.state.SingleIssueState;
import org.jboss.query.IssueStatus;
import org.jboss.query.SearchQuery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class StaleIssueCollector extends NewIssuesConsumer implements Executable<EveryIssueState> {

    private static final State state = new State();

    private StaleIssueCollector(JiraEndpoint jiraEndpoint) {
        super(jiraEndpoint, exchange -> {
            Issue issue = exchange.getIn().getBody(Issue.class);
            if (issue != null) {
                state.issueStates.add(new SingleIssueState(issue));
            }
        });
    }

    private static final class State {
        List<SingleIssueState> issueStates = new CopyOnWriteArrayList<>();
    }

    public static StaleIssueCollector getInstance(JiraEndpoint jiraEndpoint) {
        // TODO add stale period for SearchBuilder.endDate
        SearchQuery searchQuery = SearchQuery.builder().projects("WFLY").assigneeNotEmpty()
                .status(IssueStatus.CREATED, IssueStatus.ASSIGNED, IssueStatus.POST).build();
        jiraEndpoint.setJql(JqlBuilder.build(searchQuery));
        return new StaleIssueCollector(jiraEndpoint);
    }

    @Override
    public EveryIssueState execute() throws Exception {
        int issuesCount = doPoll();
        Log.infof("Found number of issues %d", issuesCount);
        return new EveryIssueState(state.issueStates);
    }
}
