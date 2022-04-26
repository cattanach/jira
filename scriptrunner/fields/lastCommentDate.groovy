import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import java.text.SimpleDateFormat

// get the latest comment
def commentManager = ComponentAccessor.getCommentManager()
def comment = commentManager.getLastComment(issue)
def commentDate

// check to see if comments exist - return comment date if so
if (comment != null) {
commentDate = comment.created
def date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(commentDate)
return date
}