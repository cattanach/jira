//JADMIN-1290
//Ref: https://scriptrunner.adaptavist.com/4.3.3/jira/recipes/behaviours/setting-default-fields.html

import com.atlassian.jira.component.ComponentAccessor

def devBucket = getFieldByName("Development Bucket") 
def optionsManager = ComponentAccessor.getOptionsManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def customField = customFieldManager.getCustomFieldObject(devBucket.getFieldId())
def config = customField.getRelevantConfig(getIssueContext())
def options = optionsManager.getOptions(config)
def optionToSelect = options.find { it.value == "Support" } 

devBucket.setFormValue(optionToSelect.optionId)
devBucket.setReadOnly(true)
