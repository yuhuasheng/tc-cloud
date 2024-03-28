package com.foxconn.plm.utils.collect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class CollectUtil {


    /**
     * 判断list类型的数据是否为空 null,[] 为 true
     *
     * @return boolean
     */
    public static boolean isEmpty(List<?> list) {
        return (null == list || list.size() == 0);
    }

    /**
     * 判断list类型的数据是否为空 null,[] 为 false
     *
     * @return boolean
     */
    public static boolean isNotEmpty(List<?> list) {
        return !isEmpty(list);
    }


    /**
     * 判断Map类型的数据是否为空 null,[] 为true
     *
     * @return boolean
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return (null == map || map.size() == 0);
    }

    /**
     * 判断map类型的数据是否为空 null,[] 为 false
     *
     * @return boolean
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 数组类型是否为空null,[] 为true
     *
     * @param
     * @return
     */
    public static boolean isEmpty(Object[] objects) {
        return (null == objects || objects.length == 0);
    }

    /**
     * 数组类型是否为空null,[] 为false
     *
     * @param objects
     * @return
     */
    public static boolean isNotEmpty(Object[] objects) {
        return !isEmpty(objects);
    }





    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }



    /**
     * 将一组数据固定分组，每组n个元素
     *
     * @param source 要分组的数据源
     * @param n      每组n个元素
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> fixedGrouping(List<T> source, int n) {
        if (null == source || source.size() == 0 || n <= 0)
            return null;
        List<List<T>> result = new ArrayList<List<T>>();
        int remainder = source.size() % n;//余数
        int size = (source.size() / n);//商 不算余数 要分多少组。有余数的话下面有单独处理余数数据的
        for (int i = 0; i < size; i++) {//循环要分多少组
            List<T> subset = null;
            subset = source.subList(i * n, (i + 1) * n);//截取list
            result.add(subset);
        }
        if (remainder > 0) {//有余数的情况下把余数得到的数据再添加到list里面
            List<T> subset = null;
            subset = source.subList(size * n, size * n + remainder);
            result.add(subset);
        }
        return result;
    }
}
