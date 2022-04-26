/* test comment */

USE Q2_ODS
SELECT issue_key
FROM Jira.Issue_raw
WHERE issue_key IS NOT NULL
ORDER BY issue_key ASC
