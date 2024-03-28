package com.foxconn.plm.utils.batch;

import java.util.ArrayList;
import java.util.List;

public class BatchProcessor {

    public interface Handler<T>{
        void onBatch(List<T> list);
    }

    public static <T> void batch(List<T> list, int size, Handler<T> handler) {
        if(handler==null){
            return;
        }
        int index = -1;
        List<T> l = new ArrayList<>();
        while (index<list.size()){
            for (int i = 0; i < size; i++) {
                index++;
                if(index == list.size()){
                    handler.onBatch(l);
                    return;
                }
                l.add(list.get(index));
            }
            handler.onBatch(l);
            l.clear();
        }

    }

}
