package org.jboss;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.jira.JiraComponent;
import org.apache.camel.component.jira.JiraConfiguration;
import org.apache.camel.component.jira.JiraEndpoint;
import org.jboss.config.JiraLotteryAppConfig;
import org.jboss.processing.NewIssueCollector;
import org.jboss.processing.IssueProcessor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "jira-issue-lottery", mixinStandardHelpOptions = true)
public class JiraIssueLotteryCommand implements Runnable {

    @Inject
    JiraLotteryAppConfig jiraLotteryAppConfig;

    @Inject
    CamelContext camelContext;

    @Parameters(paramLabel = "<name>", defaultValue = "picocli", description = "Your name.")
    String name;

    private JiraConfiguration jiraConfiguration;
    private JiraEndpoint jiraEndpoint;

    @PostConstruct
    public void setup() {
        jiraConfiguration = setupJiraConfiguration();
        jiraEndpoint = setupJiraEndpoint(jiraConfiguration);
    }

    private JiraEndpoint setupJiraEndpoint(JiraConfiguration jiraConfiguration) {
        JiraEndpoint jiraEndpoint = new JiraEndpoint("issues.redhat.com", new JiraComponent(camelContext), jiraConfiguration);
        jiraEndpoint.connect();
        jiraEndpoint.setMaxResults(jiraLotteryAppConfig.maxResults());
        return jiraEndpoint;
    }

    private JiraConfiguration setupJiraConfiguration() {
        JiraConfiguration jiraConfiguration = new JiraConfiguration();
        jiraConfiguration.setJiraUrl("https://issues.redhat.com");
        jiraConfiguration.setAccessToken(jiraLotteryAppConfig.accessToken());
        return jiraConfiguration;
    }

    @Override
    public void run() {
        NewIssueCollector newIssueCollector = NewIssueCollector.getInstance(jiraEndpoint);
        Exchange exchange = jiraEndpoint.createExchange();
        try {
            newIssueCollector.execute().getIssueStates().forEach(Log::info);
            new IssueProcessor(jiraEndpoint, "JBEAP-25900").process(exchange);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
