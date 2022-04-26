//JADMIN-2488

import com.atlassian.jira.component.*;
import com.deniz.jira.worklog.data.attr.AttrType;
import com.deniz.jira.worklog.data.attr.AttrValue;
import com.deniz.jira.worklog.services.attr.AttrTypeService;
import com.deniz.jira.worklog.data.attr.AttrValueImp;
import com.deniz.jira.worklog.scripting.WorklogPreEntryParameters;
import com.deniz.jira.worklog.services.*;
import com.atlassian.jira.issue.*;
import java.util.*;
import org.slf4j.*;

def issueManager = ComponentAccessor.getComponent(IssueManager.class);
def issue = issueManager.getIssueObject(worklogPreEntryParameters.issueKey);

def attrTypeService = ComponentAccessor.getOSGiComponentInstanceOfType(AttrTypeService.class);
def attrTypes = ComponentAccessor.getOSGiComponentInstanceOfType(AttrType.class);
def customFieldManager = ComponentAccessor.getComponent(CustomFieldManager.class);

def selectCustomField = customFieldManager.getCustomFieldObject(13013); //Getting custom field with custom field id
def selectedOption = issue.getCustomFieldValue(selectCustomField);

def singleSelectAttrId = attrTypeService.getAttrTypeWithName("Development Bucket").get().ID;//Getting attribute with attribute name
def singleSelectAttr = worklogPreEntryParameters.attrTypes.find {element -> element.id == singleSelectAttrId};
def singleSelectAttrValues=singleSelectAttr.attributeValues;

def attrNames=singleSelectAttrValues.collect {it -> it.name};
for(option in attrNames){
    def AttrOption=option.toString();
    if(AttrOption == selectedOption.toString()){
        def attrImp=singleSelectAttrValues.find {it -> it.name==AttrOption};
        attrImp.setDefaultValue(true);
    }
}