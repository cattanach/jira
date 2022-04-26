//JADMIN-1027
//Template: Date Time

Date releaseDate = null
issue.fixVersions?.each{releaseDate = it.releaseDate}
return releaseDate
