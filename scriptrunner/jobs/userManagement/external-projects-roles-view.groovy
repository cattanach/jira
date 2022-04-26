/**
* Check project roles in external projects
* (projects in categories PS-External and CLS-External).
* IF any user without Q2 in the email domain is in the Administrators role,
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
// set userGroupName to check group for users without company domain emails
// set adminGroupName to email report to group members
// set companyDomain to check for the text emails should contain
// set categoriesToInclude to specify the project categories that will be checked
final userGroupName = 'jira-access'
final adminGroupName = 'q2-admin-notifications'
final companyDomain = 'q2'
final categoriesToInclude = ["PS-External", "CLS-External"]
 
// get users to check for valid emails
// get admins to email with report
// get all global project roles
def groupManager = ComponentAccessor.groupManager
def userManager = ComponentAccessor.userManager
def userSearchService = ComponentAccessor.getComponent(UserSearchService.class);
def userGroup = groupManager.getGroup(userGroupName)
def users = groupManager.getUsersInGroup(userGroup)
def projectManager = ComponentAccessor.projectManager
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def adminGroup = groupManager.getGroup(adminGroupName)
def admins = groupManager.getUsersInGroup(adminGroup)
def projectRoles = projectRoleManager.getProjectRoles()
def projectCategories = projectManager.getAllProjectCategories()
 
// check user group for users without company domain emails
LinkedHashMap invalidEmailUsers = checkEmailDomain(users, companyDomain)
// get usernames for all users and put them in a set
// using a set will be more performant than a list for doing lookups later
Set invalidEmailUsernames = invalidEmailUsers.keySet()
Set allUsernames = users.collect { it.name } as Set
 
// get all projects in categories we want to check
List<Project> projectsToCheck = []
categoriesToInclude.each { category ->
    def categoryObj = projectManager.getProjectCategoryObjectByNameIgnoreCase(category)
    Collection<Project> project = projectManager.getProjectObjectsFromProjectCategory(categoryObj.getId())
    projectsToCheck.addAll(project)
}
 
 
List<LinkedHashMap> invalidUsers = []
projectsToCheck.each { project ->
    log.info project
    projectRoles.each { role ->
        // we only need to check the Admin role
        if (role.toString().equals("Administrators")) {
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
                // check if any of these users are in our list of invalid users
                List<String> rolePermissionViolations = roleUsersUsernames.findAll { invalidEmailUsernames.contains(it) }
                // or if any are not in the jira-access group
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
}
 
// a summary to use in our output
String scriptSummary = "${invalidUsers.size()} external users were found who have Administrator permissions in external projects. Please review the output and remove any users necessary."
 
def emailBody = """Hello,
 
${scriptSummary}
 
${formatInvalidUsers(invalidUsers)}
"""
 
// notify admins if checkbox is checked
if (!EMAIL_ADMINS) {
    return "${scriptSummary} After review, check the 'Email report' box. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}"
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
LinkedHashMap checkEmailDomain(Collection<ApplicationUser> users, String companyDomain) {
    def invalidUsers = new LinkedHashMap()
     
    users.each { user ->
        //log.info "Inspecting $user.name to determine whether user has a valid company email"
        def userEmail = user.getEmailAddress()
 
        // check if user has a company email
        if (!userEmail.toLowerCase().contains(companyDomain)) {
            log.info "User ${user.name} does not have a valid ${companyDomain} email. Email on record is ${userEmail}"
            invalidUsers.put(user.name, userEmail)
 
        } 
    }
     
    return invalidUsers
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