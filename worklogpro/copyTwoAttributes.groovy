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

if (worklogPreEntryParameters == null) {
    return;
}

def issue = issueManager.getIssueObject(worklogPreEntryParameters.issueKey);

def attrTypeService = ComponentAccessor.getOSGiComponentInstanceOfType(AttrTypeService.class);
def attrTypes = ComponentAccessor.getOSGiComponentInstanceOfType(AttrType.class);
def customFieldManager = ComponentAccessor.getComponent(CustomFieldManager.class);

def selectDevelopmentBucket = customFieldManager.getCustomFieldObject(13013); //Get Development Bucket field with customFieldId
def selectedOptionDevelopmentBucket = issue.getCustomFieldValue(selectDevelopmentBucket);

def selectProductBucket = customFieldManager.getCustomFieldObject(13201); //Get Product Bucket Bucket with field customFieldId
def selectedOptionProductBucket = issue.getCustomFieldValue(selectProductBucket);

def singleSelectDevelopmentBucket = attrTypeService.getAttrTypeWithName("Development Bucket").get().ID; //Get Development Bucket attribute with attribute name
def singleSelectDevelopmentBucketAttr = worklogPreEntryParameters.attrTypes.find {element -> element.id == singleSelectDevelopmentBucket};
def singleSelectDevelopmentBucketAttrValues=singleSelectDevelopmentBucketAttr.attributeValues;

def singleSelectProductBucket = attrTypeService.getAttrTypeWithName("Product Bucket").get().ID; //Get Product Bucket attribute with attribute name
def singleSelectProductBucketAttr = worklogPreEntryParameters.attrTypes.find {element -> element.id == singleSelectProductBucket};
def singleSelectProductBucketAttrValues=singleSelectProductBucketAttr.attributeValues;

def developmentBucketAttrNames=singleSelectDevelopmentBucketAttrValues.collect {it -> it.name};
def productBucketAttrNames=singleSelectProductBucketAttrValues.collect {it -> it.name};

for(option in developmentBucketAttrNames) {
    def AttrOption1 = option.toString();
    if (AttrOption1 == selectedOptionDevelopmentBucket.toString()) {
        def attrImp2 = singleSelectDevelopmentBucketAttrValues.find { it -> it.name == AttrOption1 };
        attrImp2.setDefaultValue(true);
    }
}

for(option in productBucketAttrNames) {
    def AttrOption2 = option.toString();
    if (AttrOption2 == selectedOptionProductBucket.toString()) {
        def attrImp2 = singleSelectProductBucketAttrValues.find {it -> it.name == AttrOption2};
        attrImp2.setDefaultValue(true);
    }
}