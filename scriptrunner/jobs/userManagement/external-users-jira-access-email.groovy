/**
* Build a report of any members with an email outside the company's domain
* who are in the jira-access group and email it to the admins.
*/
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.mail.server.MailServerManager
import com.atlassian.mail.Email
import com.atlassian.jira.user.ApplicationUser
import com.onresolve.scriptrunner.parameters.annotation.*

// improve logging
import org.apache.log4j.Logger
import org.apache.log4j.Level
log = Logger.getLogger("log") 
log.setLevel(Level.DEBUG)

/** 
* UNCOMMENT THE FOLLOWING LINES IF THIS SCRIPT IS BEING RUN IN THE CONSOLE.
* THIS WILL ALLOW A "DRY RUN" OF THE REPORT TO SEE WHAT PERMISSIONS WILL BE REMOVED.
**/
/*
@Checkbox(label = "Email report", description = "If selected, a report of users with invalid permissions will be emailed to the admin group defined in the script.")
Boolean EMAIL_ADMINS
*/
Boolean EMAIL_ADMINS = true

// set userGroupName to check group for users without company domain emails
// set adminGroupName to email report to group members
// set companyDomain to check for the text emails should contain
final userGroupName = 'jira-access'
final adminGroupName = 'q2-admin-notifications' 
final companyDomain = 'q2'

// get users to check and admins to email from groups defined above
def groupManager = ComponentAccessor.groupManager
def userManager = ComponentAccessor.userManager
def userGroup = groupManager.getGroup(userGroupName)
def users = groupManager.getUsersInGroup(userGroup)
def adminGroup = groupManager.getGroup(adminGroupName)
def admins = groupManager.getUsersInGroup(adminGroup)

// add any emails that should be ignored in the report
// for example, add-on emails like "jira-sketch-integration@connect.atlassian.com"
List emailsToIgnore = ["com.github.integration.production@connect.atlassian.com","com.lucidchart.confluence.plugins.lucid-confluence@connect.atlassian.com","com.testrail.jira.testrail-plugin@connect.atlassian.com","jira-sketch-integration@connect.atlassian.com",
"jira-slack-integration@connect.atlassian.com","jira-trello-integration@connect.atlassian.com","stspg-jira-ops@connect.atlassian.com"]

// check user group for users without company domain emails
LinkedHashMap invalidUsers = checkEmailDomain(users, companyDomain, emailsToIgnore)

def emailBody = """Hello Jira Admins,

The following users are members of the ${userGroupName} group but do not have a company domain email. Please remove the external users.

${formatInvalidUsers(invalidUsers)}
"""

// notify admins if checkbox is checked
if (!EMAIL_ADMINS) {
	return "Found ${invalidUsers.size()} users without a company domain email. Please review output and then check 'Email report' box. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}"
} else {
    if (invalidUsers.size() > 0) {
        // get list of emails to send report to
        List adminEmails = getGroupEmails(admins)
        if (adminEmails) {
            log.info("Sent email to ${adminEmails}")
        	sendEmail(adminEmails, "[Automated Message] Users removed from ${userGroupName} group", emailBody)
            return "Found ${invalidUsers.size()} users without a company domain email and emailed report to ${adminGroupName}. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}"
        } else {
            return "Found ${invalidUsers.size()} users without a company domain email but no emails on file for ${adminGroupName}. <br/><br/>${formatInvalidUsers(invalidUsers, "<br />")}" 
        }
    } else {
        return "No invalid users found, so no email was sent."
    }
}

// functions
LinkedHashMap checkEmailDomain(Collection<ApplicationUser> users, String companyDomain, List emailsToIgnore) {
	def invalidUsers = new LinkedHashMap()
    
    users.each { user ->
        //log.info "Inspecting $user.name to determine whether user has a valid company email"
        def userEmail = user.getEmailAddress()

        // check if user has a company email
        if (userEmail && !emailsToIgnore.contains(userEmail) && !userEmail.toLowerCase().contains(companyDomain)) {
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

String formatInvalidUsers(LinkedHashMap invalidUsers, linebreak="\n") {
    String formattedInvalidUsers = "User email:${linebreak}${linebreak}"
    invalidUsers.each {
        formattedInvalidUsers += "${it.value}${linebreak}"
    }
    return formattedInvalidUsers
}