//JADMIN-1236
//Uses constrained issue script fragment https://jira.q2ebanking.com/plugins/servlet/scriptrunner/admin/fragments/edit/69adafe6-d0d0-43c9-9ca8-961ffb2cd5ec
//Section: operations-top-level
//Key: link-create-upgrade
//Condition: jiraHelper.project?.key in ["ETMS","DTS","PIQS"]
//Tutorial: https://scriptrunner.adaptavist.com/4.3.9/jira/fragments/CreateConstrainedIssue.html

import com.atlassian.jira.component.ComponentAccessor

def issueManager = ComponentAccessor.getIssueManager()

if (getBehaviourContextId() == "link-create-upgrade") {
    getFieldById("project-field").setReadOnly(true)
    getFieldById("issuetype-field").setReadOnly(true)

    def contextIssue = issueManager.getIssueObject(getContextIssueId())
    
    getFieldById("summary").setFormValue("${contextIssue.summary}").setReadOnly(false) //Summary
    getFieldById("description").setFormValue(contextIssue.getCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_10800"))).setReadOnly(false) //Implementation Instructions to Description
    //getFieldById("customfield_14404").setFormValue(contextIssue.getCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_14404"))).setReadOnly(false) //Customer
    //getFieldById("issuelinks-linktype").setFormValue("relates to").setReadOnly(true) //Linked issue type
    //getFieldById("issuelinks-issues").setFormValue(contextIssue.key).setReadOnly(true) //Linked issue name
}
