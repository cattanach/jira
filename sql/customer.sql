SELECT Account_Number_and_Account_Name__c, AccountNumber
FROM Account
WHERE Open_Customer_Type__c = 'Partner' OR (AccountNumber != null AND AccountNumber NOT IN ('0000','0001','0002','0005-00','0006','7777-01','9945-01','9888-01','001-6505132-504'))
ORDER BY Account_Number_and_Account_Name__c ASC
