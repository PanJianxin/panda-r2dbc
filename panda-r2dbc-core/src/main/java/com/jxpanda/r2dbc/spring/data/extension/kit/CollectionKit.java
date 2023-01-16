package com.jxpanda.r2dbc.spring.data.extension.kit;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Panda
 */
public class CollectionKit {

    /**
     * 去重合并集合，把多个集合合并到一个里面
     */
    @SafeVarargs
    public static <T> List<T> distinctMerge(List<T>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).distinct().collect(Collectors.toList());
    }

    /**
     * 去重合并集合，把多个集合合并到一个里面
     * 数组入参的重载
     */
    @SafeVarargs
    public static <T> List<T> distinctMerge(T[]... arrays) {
        return Arrays.stream(arrays).flatMap(Arrays::stream).distinct().collect(Collectors.toList());
    }

    /**
     * 去重合并集合，把多个集合合并到一个里面
     */
    @SafeVarargs
    public static <K, T> List<T> distinctMerge(Function<? super T, ? extends K> keySelector, List<T>... lists) {
        return distinctBy(keySelector, Arrays.stream(lists).flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * 去重合并集合，把多个集合合并到一个里面
     * 数组入参的重载
     */
    @SafeVarargs
    public static <K, T> List<T> distinctMerge(Function<? super T, ? extends K> keySelector, T[]... arrays) {
        return distinctBy(keySelector, Arrays.stream(arrays).flatMap(Arrays::stream).distinct().collect(Collectors.toList()));
    }

    /**
     * 合并集合，把多个集合合并到一个里面
     */
    @SafeVarargs
    public static <T> List<T> merge(List<T>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * 合并集合，把多个集合合并到一个里面
     * 数组入参的重载
     */
    @SafeVarargs
    public static <T> List<T> merge(T[]... arrays) {
        return Arrays.stream(arrays).flatMap(Arrays::stream).collect(Collectors.toList());
    }

    /**
     * 基于某个字段去重
     */
    public static <K, T> List<T> distinctBy(Function<? super T, ? extends K> keySelector, List<T> list) {
        LinkedHashMap<? extends K, T> collect = list.stream()
                .collect(Collectors.toMap(keySelector, Function.identity(), (k, t) -> k, LinkedHashMap::new));
        return new ArrayList<>(collect.values());
    }


    /**
     * 求笛卡尔积
     *
     * @param sourceList 数据源列表
     * @return 笛卡尔积的结果列表（是一个二维数组）
     */
    @SafeVarargs
    public static <T> List<List<T>> cartesianProduct(List<T>... sourceList) {
        return cartesianProduct(Function.identity(), sourceList);
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    /**
     * 求笛卡尔积
     *
     * @param mapper     值映射函数，支持在求笛卡尔积的过程中转换值类型
     * @param sourceList 数据源列表
     * @return 笛卡尔积的结果列表（是一个二维数组）
     */
    @SafeVarargs
    public static <T, R> List<List<R>> cartesianProduct(Function<T, R> mapper, List<T>... sourceList) {
        if (sourceList == null || sourceList.length == 0) {
            return Collections.emptyList();
        }
        return cartesianProduct(Arrays.stream(sourceList).collect(Collectors.toList()), mapper);
    }

    /**
     * 求笛卡尔积
     *
     * @param sourceList 数据源列表
     * @return 笛卡尔积的结果列表（是一个二维数组）
     */
    public static <T> List<List<T>> cartesianProduct(List<List<T>> sourceList) {
        return cartesianProduct(sourceList, Function.identity());
    }

    /**
     * 求笛卡尔积
     *
     * @param sourceList 数据源列表
     * @return 笛卡尔积的结果列表（是一个二维数组）
     */
    public static <T, R> List<List<R>> cartesianProduct(List<List<T>> sourceList, Function<T, R> mapper) {
        if (isEmpty(sourceList)) {
            return Collections.emptyList();
        }

        // 笛卡尔积的势
        int power = sourceList.stream()
                .filter(CollectionKit::isNotEmpty)
                .map(List::size)
                .reduce((s1, s2) -> s1 * s2)
                .orElse(0);

        if (power <= 0) {
            return Collections.emptyList();
        }

        List<List<R>> resultList = new ArrayList<>();

        for (int i = 0; i < power; i++) {
            int index = i;
            List<R> tempList = new ArrayList<>(sourceList.size());
            for (int j = sourceList.size() - 1; j >= 0; j--) {
                List<T> currentList = sourceList.get(j);
                tempList.add(mapper.apply(currentList.get(index % currentList.size())));
                index = index / currentList.size();
            }
            resultList.add(tempList);
        }

        return resultList;
    }

}
