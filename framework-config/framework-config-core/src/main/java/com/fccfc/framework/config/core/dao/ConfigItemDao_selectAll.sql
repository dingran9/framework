SELECT 
	A.MODULE_CODE,A.CONFIG_ITEM_CODE,B.PARAM_CODE,B.PARAM_VALUE 
FROM 
	CONFIG_ITEM A,CONFIG_ITEM_PARAM B
WHERE 
	A.CONFIG_ITEM_ID = B.CONFIG_ITEM_ID