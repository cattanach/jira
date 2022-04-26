import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.filter.SearchRequestService
import com.atlassian.jira.bc.portal.PortalPageService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.permission.GlobalPermissionKey
import com.atlassian.jira.permission.GlobalPermissionType
import com.atlassian.jira.portal.PortalPage
import com.atlassian.jira.sharing.SharePermissionImpl
import com.atlassian.jira.sharing.SharedEntity
import com.atlassian.jira.sharing.search.GlobalShareTypeSearchParameter
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder
import com.atlassian.jira.sharing.SharePermission
import com.atlassian.jira.sharing.type.ShareType
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.UrlMode
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import groovy.xml.MarkupBuilder

/**
 * This script will search through all filters and dashboards
 * ... determine if it has been shared with "any authenticated user", aka "Any logged-in User"
 * ... and assign it to a pre-defined group. To chose the group, set the GROUP_NAME string to the desired group.
 * 
 * Run this with FIX_MODE = false to report on any problems.
 * To have changes apply, change to: FIX_MODE = true.
 * 
 * This was built off the public Adaptavist Library script here: 
 * https://library.adaptavist.com/entity/restrict-public-filters
 */


final FIX_MODE = true
final GROUP_NAME = "jira-access"


def searchRequestService = ComponentAccessor.getComponent(SearchRequestService)
def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
def portalPageService = ComponentAccessor.getComponent(PortalPageService)
def globalPermissionManager = ComponentAccessor.globalPermissionManager

def contextPath = applicationProperties.getBaseUrl(UrlMode.RELATIVE)

def writer = new StringWriter()
def markup = new MarkupBuilder(writer)

def serviceContext = new JiraServiceContextImpl(currentUser)

// This searchParameters will search for all items, regardless of the share type.
def searchParameters = new SharedEntitySearchParametersBuilder().setShareTypeParameter(null).toSearchParameters()


searchRequestService.validateForSearch(serviceContext, searchParameters)
assert !serviceContext.errorCollection.hasAnyErrors()

markup.h3('Filters')
def searchFilterResult = searchRequestService.search(serviceContext, searchParameters, 0, Integer.MAX_VALUE)

// Share Permissions object that is set for the group defined in the GROUP_NAME variable
final desiredPermissions = new SharedEntity.SharePermissions([
    new SharePermissionImpl(null, ShareType.Name.GROUP, GROUP_NAME, null)
] as Set)

def int filter_count = 0
searchFilterResult.results.each { filter ->   
    if (filter.getPermissions().isAuthenticated() == true){
        filter_count += 1
    	if (FIX_MODE) {
        // Business logic: If a filter exists, and it currently is shared with "Any logged in user", 
        // then this should be changed such that it is shared with the group defined in GROUP_NAME
            filter.setPermissions(desiredPermissions)

            def filterUpdateContext = new JiraServiceContextImpl(filter.owner)
            searchRequestService.updateFilter(filterUpdateContext, filter)
            if (filterUpdateContext.errorCollection.hasAnyErrors()) {
                log.warn("Error updating filter - possibly owner has been deleted. Just delete the filter. " + filterUpdateContext.errorCollection)
        	}
        }
		markup.p {
        	a(href: "$contextPath/issues/?filter=${filter.id}", target: '_blank', filter.name)
        	i(' has the permission for "Any logged-in user". ' + (FIX_MODE ? ' Fixed.' : ''))
    	}
    } 
}
if (filter_count == 0) {
    markup.p('No filters shared with "Any logged-in User" found')
}

markup.h3('Dashboards')
def searchDashResults = portalPageService.search(serviceContext, searchParameters, 0, Integer.MAX_VALUE).results.findAll {
    !it.systemDefaultPortalPage
}

def int dashboard_count = 0
searchDashResults.each { dashboard ->
    if (dashboard.getPermissions().isAuthenticated() == true){
        if (dashboard.isSystemDefaultPortalPage()) {
            // can't edit the system default dashboard
            return
        }
        dashboard_count += 1
        if (FIX_MODE) {
            def updatedDashboard = new PortalPage.Builder().portalPage(dashboard).permissions(desiredPermissions).build()
            portalPageService.updatePortalPageUnconditionally(serviceContext, currentUser, updatedDashboard)
        }
        markup.p {
            a(href: "$contextPath/secure/Dashboard.jspa?selectPageId=${dashboard.id}", target: '_blank', dashboard.name)
            i(' has the permission for "Any logged-in user". ' + (FIX_MODE ? ' Fixed.' : ''))
        }
    } 
}
if (dashboard_count == 0) {
        markup.p('No dashboards shared with "Any logged-in User" found')
    }

writer.toString()