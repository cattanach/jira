import com.deniz.jira.worklog.scripting.WorklogPreEntryParameters;
import java.util.*;

def script = '''
        function issueChanged() {
        var issueKey=AJS.$("#log-work-issue-picker").val()[0];

        fetch(`/rest/api/2/issue/${issueKey}`).then(function(response) {
        if (response.status === 200) {
        response.json().then(function(data) {

        if(data.fields.customfield_13013 !== null){ // Development Bucket custom field id
        var selectedOptionDevelopmentBucket=data.fields.customfield_13013.value.toString();
        var devBucId =AJS.$("option:contains('" + selectedOptionDevelopmentBucket + "')").addClass("on").val();
        AJS.$("#wa_3").val(devBucId).prop('selected', true); // Development Bucket worklog attribute
        AJS.$("#wa_3").trigger("change");
        }else{
        AJS.$("#wa_3").val('').prop('selected', true);
        AJS.$("#wa_3").trigger("change");
        }

        if(data.fields.customfield_13201 !== null){ // Product Bucket custom field
        var selectedOptionProductBucket=data.fields.customfield_13201.value.toString();
        var prodBucId =AJS.$("option:contains('" + selectedOptionProductBucket + "')").addClass("on").val();
        AJS.$("#wa_4").val(prodBucId).prop('selected', true) // Product Bucket worklog attribute id 
        AJS.$("#wa_4").trigger("change");
        }else{
        AJS.$("#wa_4").val('').prop('selected', true)
        AJS.$("#wa_4").trigger("change");
        }

        });
        }
        });
        }
        AJS.$(document).on("change", "#log-work-issue-picker-field", function(evt) {
        setTimeout(issueChanged, 10);
        });

        '''

worklogPreEntryParameters.jsScript = script;
return worklogPreEntryParameters;