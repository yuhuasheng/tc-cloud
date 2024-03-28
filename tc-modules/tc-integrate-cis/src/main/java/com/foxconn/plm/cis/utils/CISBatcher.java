package com.foxconn.plm.cis.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CISBatcher {

    public interface Handler<T1, T2> {
        public void onHandler(Map<T1, T2> m);
    }

    public static <T1, T2> void batcher(Map<T1, T2> dataMap, int size, Handler<T1, T2> handler) {
        if (null == handler) {
            return;
        }

        List<T1> dataLst = dataMap.keySet().stream().collect(Collectors.toList());

        int index = -1;
        Map<T1, T2> tempMap = new HashMap<>();
        List<T1> tempLst = new ArrayList<>();
        while (index < dataLst.size()) {
            for (int i = 0; i < size; i++) {
                index++;
                if (index == dataLst.size()) {
                    if (tempMap.size() > 0)
                        handler.onHandler(tempMap);
                    return;
                }
                T1 key = dataLst.get(index);
                tempMap.put(key, dataMap.get(key));
                tempLst.add(dataLst.get(index));
            }
            handler.onHandler(tempMap);
            tempMap.clear();
            tempLst.clear();
        }
    }

    public static void main(String[] args) {
        Map<String, String> testMap = new HashMap<>();
        testMap.put("1", "11");
        testMap.put("a", "aa");
        testMap.put("2", "22");
        testMap.put("b", "bb");
        testMap.put("3", "33");
        testMap.put("c", "cc");

        batcher(testMap, 2, e -> {
            e.forEach((key, value) -> {
                System.out.println(key);
                System.out.println(value);
            });
        });
    }

}
