/*

This script is designed to monitor external users inside the Q2 Jira instance.
The purpose is to monitor the groups and projects that external users are part of, and notify admins if there is a discrepancy in membership

The naming convention of users, groups and projects should be followed precicely in order for this script to function. Below are a list of regex queries
that identify what this script looks for

Users: ^(ext-)\d{4,}/i
    * Username must start with 'ext-' and be immediately followed by 4 or more digits
    * This query is case insenstive, so it will find users with upper-case 'EXT-' as well
    example: ext-1234-jdoe
Groups: (-external-)\d{4,}/i
    * Group must contain the exact phrase '-external-' immediately followed by 4 or more digits
    * This query is case insenstive, so it will find groups with upper-case '-EXTERNAL-' as well
    example: cls-external-1234-01
Projects: \d{4,}
    * Project name must contain 4 or more digits in sequence
    example: ext-1234-SampleBank

* Note: Each username, group and project should contain a unique project code that is specific to each external client. For example, 3448.
* Projects are found to match if the unique project code exists in the name, no other validation occurs

This script follows the process below to identify discrepancies

1a. Search through all users on the Jira instance, and filter out users that contain 'ext-'' as part of their username
1b. Filter this list down further to only users that match the regex above
1c. Identify the unique identifier the user has in their username

2a. Search through all groups the user is a part of. Only groups that contain the same unique identifer are considered valid
2b. If an invalid group is found as part of the 'ignore list' defined further down in the script, that group is ignored

3a. Iterate through all projects, and determine if this user is part of that project
3b. If this user is part of a project that does not share the same unique identifer, this will be considered invalid.

4. Collect all the invalid entries, and submit this as an email to the Jira Administrators


*/



import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.mail.server.MailServerManager
import com.atlassian.mail.Email
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.RoleActor
import com.onresolve.scriptrunner.parameters.annotation.*
import com.atlassian.jira.bc.user.search.UserSearchService
import com.atlassian.jira.bc.user.search.UserSearchParams
import com.atlassian.jira.user.UserFilter
import com.atlassian.jira.user.util.UserUtil

// improve logging
import org.apache.log4j.Logger
import org.apache.log4j.Level
log = Logger.getLogger("log") 
log.setLevel(Level.DEBUG)

// List of groups that ALL external users are able to be a part of
List allowedExtGroups = ['ps-external-no2FA']


def groupManager = ComponentAccessor.groupManager
def projectManager = ComponentAccessor.projectManager
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def userManager = ComponentAccessor.userManager
def userSearchService = ComponentAccessor.getComponent(UserSearchService.class);

/** 
* UNCOMMENT THE FOLLOWING LINES IF THIS SCRIPT IS BEING RUN IN THE CONSOLE.
* THIS WILL ALLOW A "DRY RUN" OF THE REPORT.
**/
/*
@Checkbox(label = "Email report", description = "If selected, a report of users with invalid permissions will be emailed to the admin group defined in the script.")
Boolean EMAIL_ADMINS
*/
Boolean EMAIL_ADMINS = true

// Get notification group for email purposes
final notificationGroupName = 'q2-admin-notifications'
try {
    // Attempt to collect the group object for the notification group
    // Attempt to collect the members of said group
	def notificationGroup = groupManager.getGroup(notificationGroupName)
	def notificationUsers = groupManager.getUsersInGroup(notificationGroup)
} catch (Exception e) {
    // If an exception occurs, the script will exit gracefully
    // Example, the notificaiton group does not exist
	log.error("An exception was encountered when trying to collect members of the notification group '${notificationGroupName}'")
    return null
}

// List of invalid users for use later
List<LinkedHashMap> invalidUsers = []
LinkedHashMap invalidUserDetails = []

// Get a list of all the projects for use later
def projectCategories = projectManager.getAllProjectCategories()
Collection<Project> projectsToCheck = []
projectCategories.each { category ->
    Collection<Project> project = projectManager.getProjectsFromProjectCategory(category)
    projectsToCheck.addAll(project)
}

// Get a list of all project roles that exist
def projectRoles = projectRoleManager.getProjectRoles()

// Find all users that start with "ext-"
// Get all users that belong to JIRA Internal Directory
def allowEmptyQuery = true
def includeActive = true
def includeInactive = true
def canMatchEmail = false
def userFilter = null
def projectIds = null

def allUserParams = new UserSearchParams(allowEmptyQuery, includeActive, includeInactive, canMatchEmail, userFilter as UserFilter, projectIds as Set<Long>)

// Generate a list of all usernames that contain 'ext-'
List<String> userNames = []
userNames.addAll(userSearchService.findUserNames('ext-', allUserParams))

//Iterate over each user that returned from the query
userNames.each { userName ->
    String uniqueKey = ""
    // Check if the username matches the regex below
    // This validates it's a user created for a person, and not some add-on that happens to also contain 'ext' in the name
    if (userName =~ /(?i)^(ext-)\d{4,}/ ){
        uniqueKey = userName.split('-')[1]

        // Get a list of all the groups this specific user is a part of
        SortedSet groupList = getGroupsForUser(userName)
        
        // Iterate over all the groups this user is a part of
        groupList.each { group ->
            group = group.toString()
            String group_uniqueKey = ""
            
            // Check if the group matches the pattern for external groups
            if (group =~ /(?i)(-external-)\d{4,}/){
				group_uniqueKey = group.toString().split('-')[2]
                // If the unique key for this group does not match the unique key for the user, this is an issue
                if (group_uniqueKey != uniqueKey){
                    log.info("User ${userName} and group ${group} combination does not pass conditions")
                    invalidUserDetails = ['Type': 'User', 'Username': userName, 'Group': group]
                    invalidUsers.add(invalidUserDetails)
                }
            } 
            // Check if the group is part of the allowable external groups list
            else if (allowedExtGroups.contains(group) ) {
                // do nothing
            }
            // If the group does NOT match this pattern, the user needs to be removed from said group
            else {
                log.info("User ${userName} and group ${group} combination does not pass conditions")
                invalidUserDetails = ['Type': 'User', 'Username': userName, 'Group': group]
                invalidUsers.add(invalidUserDetails)
            }
        }
        // Iterate through each project
        projectsToCheck.each { project ->
            // Get the name of the project
            String projectName = project.getName()
            // Iterate through each role in said project
            projectRoles.each { role ->
                def user = ComponentAccessor.getUserManager().getUserByName(userName)
                def userInProject = projectRoleManager.isUserInProjectRole(user, role, project)
                // If the project does not have the unique project key assigned to this user, and the user is found in this project, we have a problem
                if (!projectName.contains(uniqueKey) && userInProject){
                    final ProjectRoleActors projectRoleActors = projectRoleManager.getProjectRoleActors(role, project)
                    Set roleActor = projectRoleActors.getRoleActors()
                    invalidUserDetails = ['Type': 'User', 'Username': userName, 'Role': role, 'Project': projectName]
                    invalidUsers.add(invalidUserDetails)
                    log.info("User ${userName} was found in ${projectName} as part of role ${role}")
                }
            }
        }
    } else {
        // If the username does not match the regex provided, it is not a user we should consider
		log.info("${userName} does not match regex, skipping")
    }
}

SortedSet getGroupsForUser(String userName){
    UserUtil userUtil = ComponentAccessor.getUserUtil()
    return userUtil.getGroupNamesForUser(userName)
}


// a summary to use in our output
String scriptSummary = "${invalidUsers.size()} exceptions were found where external users were members of groups or project roles for which they may not belong. Please review the output and remove any users necessary."

def emailBody = """Hello,

${scriptSummary}

${formatInvalidUsers(invalidUsers)}
"""

// notify admins if checkbox is checked
if (!EMAIL_ADMINS) {
	return "${scriptSummary}. After review, check the 'Email report' box. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}"
} else {
    if (invalidUsers.size() > 0) {
        // get list of emails to send report to
        List adminEmails = getGroupEmails(notificationUsers)
        if (adminEmails) {
            log.info("Sent email to ${adminEmails}")
        	sendEmail(adminEmails, "[Automated Message] External users with questionable group or project memberships", emailBody)
            return "${scriptSummary} Emailed report to ${notificationGroupName}. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}"
        } else {
            return "${scriptSummary} No emails found in ${notificationGroupName} to send report to. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}" 
        }
    } else {
        return "No external users with questionable memberships found, so no email was sent."
    }
}

void sendEmail(List emailAddrs, String subject, String body) { 
    def mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()

    if (mailServer) {
        def emailAddrsStr = emailAddrs.join(",")
        Email email = new Email(emailAddrsStr)
        email.setSubject(subject)
        email.setBody(body)
        try {
        	mailServer.send(email)
            log.info("Sent email to ${emailAddrsStr}")
        } catch(Exception ex) {
            log.error("Error sending email to ${emailAddrsStr}: ${ex}")
        }
	}
    else {
    	log.error("Error sending email; no mail server configured")
    }
}


List getGroupEmails(Collection<ApplicationUser> users) {
  	List emails = [] 
    users.each { user ->
        def email = user.getEmailAddress()
        if (email) {
            emails.add(email)
        }
    }  
    return emails
}

// TODO: refactor use of this method in find_users_outside_company_domain
String formatInvalidUsers(List<LinkedHashMap> invalidUsers, linebreak="\n") {
    String formattedInvalidUsers = ""
    invalidUsers.each { 
        it.each { key, value ->
        	formattedInvalidUsers += "$key: $value | "
        }
        formattedInvalidUsers += "${linebreak}"
    }
    return formattedInvalidUsers
}