/**
* Check project roles in internal projects 
* (projects not in categories PS-External and CLS-External). 
* IF any user who is a member of jira-access OR jira-access-project-restricted (or any groups defined in userGroupNames)
* OR who is not a member of those groups and has a role in any project, 
* notify the admins.
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

// improve logging
import org.apache.log4j.Logger
import org.apache.log4j.Level
log = Logger.getLogger("log") 
log.setLevel(Level.DEBUG)

/** 
* UNCOMMENT THE FOLLOWING LINES IF THIS SCRIPT IS BEING RUN IN THE CONSOLE.
* THIS WILL ALLOW A "DRY RUN" OF THE REPORT TO SEE WHAT PERMISSIONS WILL BE REMOVED.
**/

@Checkbox(label = "Email report", description = "If selected, a report of users with invalid permissions will be emailed to the admin group defined in the script.")
Boolean EMAIL_ADMINS

/*Boolean EMAIL_ADMINS = true
*/
// set userGroupNames to check group for users without company domain emails
// set adminGroupName to email report to group members
// set companyDomain to check for the text emails should contain
// set categoriesToExclude to specify the project categories that will not be checked

final userGroupNames = ['jira-access','jira-access-project-restricted']
final adminGroupName = 'q2-admin-notifications' 
final companyDomain = 'q2'
final categoriesToExclude = ["PS-External", "CLS-External"]

// get users to check for valid emails
// get admins to email with report
// get all global project roles
def groupManager = ComponentAccessor.groupManager
def userManager = ComponentAccessor.userManager
def userSearchService = ComponentAccessor.getComponent(UserSearchService.class);

Collection<ApplicationUser> users = []
userGroupNames.each { groupName ->
    tmpGroup = groupManager.getGroup(groupName)
    users.addAll(groupManager.getUsersInGroup(tmpGroup))
}

def projectManager = ComponentAccessor.projectManager
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def adminGroup = groupManager.getGroup(adminGroupName)
def admins = groupManager.getUsersInGroup(adminGroup)
def projectRoles = projectRoleManager.getProjectRoles()
def projectCategories = projectManager.getAllProjectCategories()

Set allUsernames = users.collect { it.name } as Set

// get all projects without a category 
Collection<Project> projectsToCheck = projectManager.getProjectObjectsWithNoCategory()
// get all projects in categories we don't want to skip
projectCategories.each { category ->
    if (!categoriesToExclude.contains(category.getName())) {
        Collection<Project> project = projectManager.getProjectsFromProjectCategory(category)
        projectsToCheck.addAll(project)
    }
}

List<LinkedHashMap> invalidUsers = []

projectsToCheck.each { project ->
    projectRoles.each { role ->
        // get all roles and actors for the project
        final ProjectRoleActors projectRoleActors = projectRoleManager.getProjectRoleActors(role, project)
        // iterate through each role/actor combination defined in the project
        // an actor can be an individual user assigned to a role, or a group assigned to a role
        // these are represented by types atlassian-user-role-actor and atlassian-group-role-actor
        for (RoleActor actor : projectRoleActors.getRoleActors()) {
            // if this is an indivdual user, set will contain one item
            // if this is a group, set will contain all users in the group
            Set<ApplicationUser> roleUsers = actor.getUsers()
            // get the usernames for each user
            List<String> roleUsersUsernames = roleUsers.collect { it.getUsername() }

            List<String> rolePermissionViolations = []
            // check if any are not in the groups defined at the top (jira-access, jira-access-project-restricted, etc)
            rolePermissionViolations.addAll(roleUsersUsernames.findAll { !allUsernames.contains(it) } )
            // if they don't meet our critera, report on them
            if (rolePermissionViolations) {
                LinkedHashMap invalidUserDetails = []
                String invalidUser = rolePermissionViolations.join(", ")
                if (actor.getType() == 'atlassian-group-role-actor') {
                    invalidUserDetails = ['Type': 'Group', 'Username(s)': invalidUser, 'Group Assiged To': actor.getDescriptor(), 'Role': role, 'Project': project.getKey()]
                } else {
                    invalidUserDetails = ['Type': 'User', 'Username': invalidUser, 'Role': role, 'Project': project.getKey()]
                }                 
                invalidUsers.add(invalidUserDetails)       
        	}
        }
    }	
}

// a summary to use in our output
String scriptSummary = "${invalidUsers.size()} exceptions were found where users who are members of neither jira-access nor jira-access-project-restricted have role-based permissions to internal projects. Please review the output and remove any users necessary."

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
        List adminEmails = getGroupEmails(admins)
        if (adminEmails) {
            log.info("Sent email to ${adminEmails}")
        	sendEmail(adminEmails, "[Automated Message] External users with internal project permissions", emailBody)
            return "${scriptSummary} Emailed report to ${adminGroupName}. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}"
        } else {
            return "${scriptSummary} No emails found in ${adminGroupName} to send report to. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}" 
        }
    } else {
        return "No external users with internal project permissions found, so no email was sent."
    }
}

// functions

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