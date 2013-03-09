package com.opensource.plugins.jira.jql;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;

/**
 * A handler for the "ParentsWithAFixVersionOf" function. This function will
 * return all the issues that have parent issues in a specific fixVersion.
 * 
 * @author Kevin Shine
 * 
 */
public class ParentsWithAFixVersionOf extends AbstractJqlFunction {

	private static final Logger log = LoggerFactory.getLogger(ParentsWithAFixVersionOf.class);

	/**
	 * We are looking for issues
	 **/
	public JiraDataType getDataType() {
		return JiraDataTypes.ISSUE;
	}

	/**
	 * Only have 1 agr.
	 */
	public int getMinimumNumberOfExpectedArguments() {
		return 1;
	}
	
	

	/**
	 * Return a list for a specific fix Version.
	 */
	public List<QueryLiteral> getValues(@NotNull QueryCreationContext queryCreationContext, @NotNull FunctionOperand operand, @NotNull TerminalClause terminalClause) {

		final JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
		final SearchService searchService;
		final List<String> arguments = operand.getArgs();
		List<Issue> subTaskIssues = new LinkedList<Issue>();
		/** For return **/
		final List<QueryLiteral> literals = new LinkedList<QueryLiteral>();

		ComponentManager cm = ComponentManager.getInstance();
		SearchRequest sr = cm.getSearchRequestService().getFilter(new JiraServiceContextImpl(queryCreationContext.getUser()), new Long(1));
		searchService = cm.getSearchService();

		try {
			for (Iterator<String> iterator = arguments.iterator(); iterator.hasNext();) {
				String version = (String) iterator.next();
				if (version.contains(",")) {
					String[] versions = version.split(",");
					builder.where().fixVersion(versions);
				} else {
					builder.where().fixVersion(version);
				}

			}

		} catch (Exception e1) {
			log.error("Error building the where clause", e1.getMessage());
		}

		Query query = builder.buildQuery();

		try {

			final SearchResults results = searchService.search(queryCreationContext.getUser(), query, PagerFilter.getUnlimitedFilter());
			final List<Issue> issues = results.getIssues();
			for (Iterator<Issue> iterator = issues.iterator(); iterator.hasNext();) {
				Issue issue = (Issue) iterator.next();
				/* add all the subtaskIssues */
				subTaskIssues.addAll(issue.getSubTaskObjects());

			}

		} catch (SearchException e) {
			log.error("Error running search", e);
		}

		for (Iterator<Issue> iterator = subTaskIssues.iterator(); iterator.hasNext();) {
			Issue issue = (Issue) iterator.next();
			try {
				literals.add(new QueryLiteral(operand, issue.getId()));
			} catch (NumberFormatException e) {
				log.warn(String.format("Subtask with a non numeric key ID '%s'.", issue.getKey()));
			}
		}

		return literals;

	}

	/**
	 * Validate input.
	 */
	public MessageSet validate(User searcher, @NotNull FunctionOperand operand, @NotNull TerminalClause terminalClause) {
		return validateNumberOfArgs(operand, 1);
	}

}