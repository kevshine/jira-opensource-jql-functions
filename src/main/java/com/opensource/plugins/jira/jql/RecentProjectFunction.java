
package com.opensource.plugins.jira.jql;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.user.UserHistoryItem;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.NotNull;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;
import com.google.common.collect.Iterables;
import com.atlassian.crowd.embedded.api.User;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A handler for the "recentHistory" function. This function will return all the projects within the user's history.
 */
public class RecentProjectFunction extends AbstractJqlFunction
{
    private static final Logger log = LoggerFactory.getLogger(RecentProjectFunction.class);

    private final UserProjectHistoryManager userProjectHistoryManager;

    public RecentProjectFunction(UserProjectHistoryManager userProjectHistoryManager)
    {
        this.userProjectHistoryManager = userProjectHistoryManager;
    }

    /**
     * This method validates the passed in args. In this case the function accepts no args, so let's validate that were none.
     */
    public MessageSet validate(User searcher, FunctionOperand operand, TerminalClause terminalClause)
    {
        return validateNumberOfArgs(operand, 0);
    }

    
    public List<QueryLiteral> getValues(QueryCreationContext queryCreationContext, FunctionOperand operand, TerminalClause terminalClause)
    {
        final List<QueryLiteral> literals = new LinkedList<QueryLiteral>();

        /*
        * We do not need to do a security check here as the search will do the security checks for us.
        */
        final List<UserHistoryItem> projects = userProjectHistoryManager.getProjectHistoryWithoutPermissionChecks(queryCreationContext.getUser());
        for (final UserHistoryItem userHistoryItem : projects)
        {
            final String value = userHistoryItem.getEntityId();

            try
            {
                literals.add(new QueryLiteral(operand, Long.parseLong(value)));
            }
            catch (NumberFormatException e)
            {
                log.warn(String.format("User history returned a non numeric project ID '%s'.", value));
            }
        }

         return literals;
    }

   /**
    * This method returns the min number of args the function takes. In this case - 0
    */
    public int getMinimumNumberOfExpectedArguments()
    {
        return 0;
    }

   /**
    * This method needs to return the type of objects the function deals with. In this case - Projects
    */
    public JiraDataType getDataType()
    {
        return JiraDataTypes.PROJECT;
    }
}
