import com.atlassian.jira.component.ComponentAccessor

// get the latest comment
def commentManager = ComponentAccessor.getCommentManager()
def comment = commentManager.getLastComment(issue)

// check to see if comments exist - return comment author if so
if (comment != null) {
def commentAuthor = comment.authorFullName
return commentAuthor
}