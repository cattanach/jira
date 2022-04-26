/* ODS in Q2EDW_QA */

select distinct Table_Name, Document_Id,Error_Message, cast(Created_Date as date) from  q2_stage.jira.Errors
where Table_Name = 'Issue_Sprints'
and Error_Message like 'Unable to read data file ; Caused by: Invalid UTF-8 middle byte%'
and CAST(created_date as date) = '2020-07-28'
order by cast(Created_Date as date) desc
