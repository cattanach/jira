//JADMIN-420
//Not an initialiser!
//Field: Resolution
//Set Required on

import com.atlassian.jira.issue.resolution.Resolution
 
def resolutionField = getFieldById("resolution")
def linkedIssuesField = getFieldById("issuelinks")
def commentField = getFieldById("comment")
 
def resolution = resolutionField.getValue() as Resolution
 
if (resolution.name == 'Duplicate') {
    linkedIssuesField.setRequired(true)
    commentField.setRequired(true)
} else {
    linkedIssuesField.setRequired(false)
    commentField.setRequired(false)
}