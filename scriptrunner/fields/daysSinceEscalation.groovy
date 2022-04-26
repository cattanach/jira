//JADMIN-1402
//Template: Number Field

import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor
import org.joda.time.Days
import org.joda.time.LocalDate
import com.atlassian.jira.issue.CustomFieldManager

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

def sfDate = customFieldManager.getCustomFieldObject("customfield_13000")
def sfDateValue = issue.getCustomFieldValue(sfDate)

def resDate = issue.resolutionDate

if (sfDateValue){
    if (resDate){
		int days = Days.daysBetween(LocalDate.fromDateFields(sfDateValue), LocalDate.fromDateFields(resDate)).getDays()
            return days.toString()
    }
}
