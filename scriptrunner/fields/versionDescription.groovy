//JADMIN-1027
//Template: Text Field (multi-line)

String description = null
issue.fixVersions?.each{description = it.description}
return description
