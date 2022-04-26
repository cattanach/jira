import com.atlassian.jira.component.ComponentAccessor

// get the latest comment
def commentManager = ComponentAccessor.getCommentManager()
def comment = commentManager.getLastComment(issue)

// check to see if comments exist - return comment body if so
if (comment != null) {
return comment.body
}

return null