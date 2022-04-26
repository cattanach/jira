//JADMIN-140
//Not an initialiser!
//Field: Resolution

import com.atlassian.jira.issue.resolution.Resolution
 
def resolutionField = getFieldById("resolution")
def testVersionsField = getFieldByName('Tested Version(s)')
def resolution = resolutionField.getValue() as Resolution
 
if (resolution.name == 'Fixed in Forward') {
    testVersionsField.setRequired(true)
    testVersionsField.setHidden(false)
} else {
    testVersionsField.setRequired(false)
    testVersionsField.setHidden(true)
}
