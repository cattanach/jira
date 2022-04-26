import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.jira.issue.Issue
import java.text.SimpleDateFormat;
import java.util.Date
import com.atlassian.core.util.DateUtils
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.issue.IssueManager


ApplicationUser user = event.user
def issueManager = ComponentAccessor.getIssueManager()
Issue issue = issueManager.getIssueObject(event.issue.key)

final salesforceCaseRank = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName('Salesforce Case Rank').first()
def salesforceCaseRankVal = issue.getCustomFieldValue(salesforceCaseRank)

final scr = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName('Salesforce Case Rank (Hidden)').first()

def changeHistoryManager = ComponentAccessor.getChangeHistoryManager()
def changeHistories = changeHistoryManager.getChangeHistories(issue)
def changeItem = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue, 'Salesforce Case Rank (Hidden)')

def changeSCR
def timeDiff
def minute
double scrVal
if(changeItem){
    changeSCR = changeItem.last()
    timeDiff = System.currentTimeMillis() - changeSCR.created.getTime()
    scrVal = changeSCR.toString as Double
    minute = Math.round((timeDiff.toString().toDouble() / 1000 / 60) as Double)
    if (minute <= 2.0 && salesforceCaseRankVal != scrVal && scrVal != 999 && scrVal != -1) {
        issue.setCustomFieldValue(salesforceCaseRank, scrVal)        
        issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false)
        return "Salesforce Case Rank is now ${issue.getCustomFieldValue(salesforceCaseRank)}"
    } else
        return "No SCR update"
     
}