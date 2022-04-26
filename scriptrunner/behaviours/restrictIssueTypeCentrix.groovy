//JADMIN-1290
//Ref: https://scriptrunner.adaptavist.com/5.5.0.3-jira8/jira/recipes/behaviours/restricting-issue-types.html
//Ref: https://community.atlassian.com/t5/Jira-questions/How-to-restrict-some-issue-types-to-some-groups/qaq-p/261541

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager

import static com.atlassian.jira.issue.IssueFieldConstants.ISSUE_TYPE

def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def allIssueTypes = ComponentAccessor.constantsManager.allIssueTypeObjects
def issueTypeField = getFieldById(ISSUE_TYPE)
def availableIssueTypes = []

def remoteUsersRoles = ComponentAccessor.getGroupManager().isUserInGroup(user, "Centrix_Support")

if (remoteUsersRoles) {
	availableIssueTypes.addAll(allIssueTypes.findAll { it.name in ["Inquiry"] })
}

else {
	availableIssueTypes.addAll(allIssueTypes.findAll { it.name in ["Bug","Epic","Initiative","Inquiry","Release","Story","Sub-task","Task","Test"] })
}

issueTypeField.setFieldOptions(availableIssueTypes)