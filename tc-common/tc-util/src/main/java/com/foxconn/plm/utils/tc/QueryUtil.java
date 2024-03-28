package com.foxconn.plm.utils.tc;

import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ImanQuery;

import java.util.HashMap;
import java.util.Map;

public class QueryUtil {


    //调用查询
    public static Map<String, Object> executeQuery(SavedQueryService queryService, String searchName, String[] keys, String[] values) {
        Map<String, Object> queryResults = new HashMap<>();
        try {
            ImanQuery query = null;
            SavedQuery.GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
            for (int i = 0; i < savedQueries.queries.length; i++) {
                if (savedQueries.queries[i].name.equals(searchName)) {
                    query = savedQueries.queries[i].query;
                    break;
                }
            }
            if (query == null) {
                queryResults.put("failed", "系统中未找到【" + searchName + "】查询..");
                return queryResults;
            }
            Map<String, String> entriesMap = new HashMap<>();
            SavedQuery.DescribeSavedQueriesResponse describeSavedQueriesResponse = queryService.describeSavedQueries(new ImanQuery[]{query});
            for (SavedQuery.SavedQueryFieldObject field : describeSavedQueriesResponse.fieldLists[0].fields) {
                String attributeName = field.attributeName;
                String entryName = field.entryName;
                System.out.println(entryName);
                entriesMap.put(attributeName, entryName);
            }
            String[] entries = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                entries[i] = entriesMap.get(keys[i]);
            }
            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[] savedQueryInput = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[1];
            savedQueryInput[0] = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput();
            savedQueryInput[0].query = query;
            savedQueryInput[0].entries = entries;
            savedQueryInput[0].values = values;
            com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = queryService.executeSavedQueries(savedQueryInput);
            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults found = savedQueryResult.arrayOfResults[0];
            queryResults.put("succeeded", found.objects);
            return queryResults;
        } catch (Exception e) {
            queryResults.put("failed", e.getMessage());
            return queryResults;
        }
    }


    public static Map<String, Object> executeQueryByEntries(SavedQueryService queryService, String searchName, String[] entries, String[] values) {
        Map<String, Object> queryResults = new HashMap<>();
        try {
            ImanQuery query = null;
            SavedQuery.GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
            for (int i = 0; i < savedQueries.queries.length; i++) {
                if (savedQueries.queries[i].name.equals(searchName)) {
                    query = savedQueries.queries[i].query;
                    break;
                }
            }
            if (query == null) {
                queryResults.put("failed", "系统中未找到【" + searchName + "】查询..");
                return queryResults;
            }
            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[] savedQueryInput = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[1];
            savedQueryInput[0] = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput();
            savedQueryInput[0].query = query;
            savedQueryInput[0].entries = entries;
            savedQueryInput[0].values = values;
            com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = queryService.executeSavedQueries(savedQueryInput);
            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults found = savedQueryResult.arrayOfResults[0];
            queryResults.put("succeeded", found.objects);
            return queryResults;
        } catch (Exception e) {
            queryResults.put("failed", e.getMessage());
            return queryResults;
        }
    }


    //调用查询
    public static synchronized ModelObject[] executeSOAQuery(SavedQueryService queryService, String searchName, String[] keys, String[] values) throws Exception {
        ModelObject[] modelObject = null;
        ImanQuery query = null;
        SavedQuery.GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
        if (savedQueries.queries.length == 0) {
            throw new Exception("There are no saved queries in the system.");
        }
        for (int i = 0; i < savedQueries.queries.length; i++) {
            if (savedQueries.queries[i].name.equals(searchName)) {
                query = savedQueries.queries[i].query;
                System.out.println(query);
                break;
            }
        }
        Map<String, String> entriesMap = new HashMap<>();
        SavedQuery.DescribeSavedQueriesResponse describeSavedQueriesResponse = queryService.describeSavedQueries(new ImanQuery[]{query});
        SavedQuery.SavedQueryFieldObject[] fields = describeSavedQueriesResponse.fieldLists[0].fields;
        for (SavedQuery.SavedQueryFieldObject field : describeSavedQueriesResponse.fieldLists[0].fields) {
            String attributeName = field.attributeName;
            String entryName = field.entryName;
            entriesMap.put(attributeName, entryName);
        }
        String[] entries = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            entries[i] = entriesMap.get(keys[i]);
        }
        com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[] savedQueryInput = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[1];
        savedQueryInput[0] = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput();
        savedQueryInput[0].query = query;
        savedQueryInput[0].maxNumToReturn = 9999;
        savedQueryInput[0].entries = entries;
        savedQueryInput[0].values = values;
        com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = queryService.executeSavedQueries(savedQueryInput);
        com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults found = savedQueryResult.arrayOfResults[0];
        return found.objects;
    }


}
