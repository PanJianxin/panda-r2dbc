package com.jxpanda.r2dbc.spring.data.extension.kit;

import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Panda
 */
public class StringKit {

    private static final String CAMEL_CASE_REGEX = "(?<!^)(?=[A-Z])";

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    /**
     * 返回字符串列表中是否有空值
     * 列表为空或者列表中包含空值就返回true，否则false
     */
    public static boolean isAnyBlank(String... strings) {
        return strings == null || Arrays.stream(strings).anyMatch(StringKit::isBlank);
    }

    /**
     * 返回字符串列表中是否有空值
     * 列表为空或者列表中包含空值就返回true，否则false
     */
    public static boolean isAnyBlank(List<String> stringList) {
        return stringList == null || stringList.stream().anyMatch(StringKit::isBlank);
    }

    /**
     * 返回字符串列表中是否全都是空值
     * 列表为空或者列表中全都是空值就返回true，否则false
     */
    public static boolean isAllBlank(String... strings) {
        return strings == null || Arrays.stream(strings).allMatch(StringKit::isBlank);
    }

    /**
     * 返回字符串列表中是否全都是空值
     * 列表为空或者列表中全都是空值就返回true，否则false
     */
    public static boolean isAllBlank(List<String> stringList) {
        return stringList == null || stringList.stream().allMatch(StringKit::isBlank);
    }

    /**
     * 返回N个字符串中，第一个不为空（blank）的值
     *
     * @param strings 字符串数列表
     * @return 第一个不为空的字符串
     */
    public static String takeNotBlank(String... strings) {
        if (strings == null || strings.length == 0) {
            return StringConstant.BLANK;
        }
        return Arrays.stream(strings).filter(StringKit::isNotBlank).findFirst().orElse(StringConstant.BLANK);
    }

    /**
     * 返回N个字符串中，第一个不为空（blank）的值
     *
     * @param stringList 字符串数列表
     * @return 第一个不为空的字符串
     */
    public static String takeNotBlank(List<String> stringList) {
        if (CollectionKit.isEmpty(stringList)) {
            return StringConstant.BLANK;
        }
        return stringList.stream().filter(StringKit::isNotBlank).findFirst().orElse(StringConstant.BLANK);
    }

    /**
     * 字符串两端添加字符
     *
     * @param string    原字符串
     * @param padString 添加的字符
     * @return 两端添加了符号的字符串
     */
    public static String padding(String string, String padString) {
        return padString + string + padString;
    }

    /**
     * 字符串两端添加字符
     *
     * @param string   原字符串
     * @param leftPad  左边添加的字符
     * @param rightPad 右边添加的字符
     * @return 两端添加了符号的字符串
     */
    public static String padding(String string, String leftPad, String rightPad) {
        return leftPad + string + rightPad;
    }

    /**
     * 把字符串转为snake_case命名规范
     *
     * @param string 待转换字符串
     * @return 转换后的字符串
     */
    public static String snakeCase(@NonNull String string) {
        return Arrays.stream(string.split(CAMEL_CASE_REGEX))
                .map(String::toLowerCase)
                .collect(Collectors.joining(StringConstant.DASH));
    }

    /**
     * 把字符串转为camelCase命名规范
     *
     * @param string 待转换字符串
     * @return 转换后的字符串
     */
    public static String camelCase(@NonNull String string) {
        String camelString = Arrays.stream(string.split(StringConstant.DASH))
                .map(StringKit::capitalize)
                .collect(Collectors.joining(StringConstant.BLANK));
        return uncapitalize(camelString);
    }

    /**
     * 字符串首字母大写
     */
    public static String capitalize(@NonNull String string) {
        if (string.isBlank() || Character.isUpperCase(string.charAt(0))) {
            return string;
        } else {
            return String.valueOf(string.charAt(0)).toUpperCase() + string.substring(1);
        }
    }

    /**
     * 字符串首字母小写
     */
    public static String uncapitalize(@NonNull String string) {
        if (string.isBlank() || Character.isLowerCase(string.charAt(0))) {
            return string;
        } else {
            return String.valueOf(string.charAt(0)).toLowerCase() + string.substring(1);
        }
    }

}
