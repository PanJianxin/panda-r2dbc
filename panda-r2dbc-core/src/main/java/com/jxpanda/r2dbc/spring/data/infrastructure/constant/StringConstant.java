package com.jxpanda.r2dbc.spring.data.infrastructure.constant;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Panda
 */
public class StringConstant {
    /**
     * 空字符串
     */
    public static final String BLANK = "";
    /**
     * 空格
     */
    public static final String SPACE = " ";
    /**
     * 下划线
     */
    public static final String DASH = "_";
    /**
     * 点
     */
    public static final String DOT = ".";
    /**
     * 斜杠
     */
    public static final String SLASH = "/";
    /**
     * 逗号（半角）
     */
    public static final String COMMA = ",";
    /**
     * 反引号
     */
    public static final String BACK_QUOTE = "`";
    /**
     * 引号（双引号）
     */
    public static final String QUOTATION_MARK = "\"";
    /**
     * 单引号
     */
    public static final String SINGLE_QUOTATION_MARK = "'";
    /**
     * 减号
     */
    public static final String MINUS = "-";

    /**
     * 等于号
     */
    public static final String EQUAL = "=";
    /**
     * 问号
     */
    public static final String QUESTION_MARK = "?";
    /**
     * &符号
     */
    public static final String AMPERSAND = "&";
    /**
     * %百分号
     */
    public static final String PERCENT_SIGN = "%";
    /**
     * 空数组（Json）
     */
    public static final String EMPTY_ARRAY = "[]";
    /**
     * 空对象（Json）
     */
    public static final String EMPTY_OBJECT = "{}";

    public static final String OK = "OK";

    public static final String SUCCESS = "SUCCESS";

    public static final String FAIL = "FAIL";

    public static final String YES = "YES";

    public static final String NO = "NO";

    public static final String UNKNOWN = "UNKNOWN";

    /**
     * 数字1-10
     */
    public static final String NUMBER_ZERO = "0";
    public static final String NUMBER_ONE = "1";
    public static final String NUMBER_TWO = "2";
    public static final String NUMBER_THREE = "3";
    public static final String NUMBER_FOUR = "4";
    public static final String NUMBER_FIVE = "5";
    public static final String NUMBER_SIX = "6";
    public static final String NUMBER_SEVEN = "7";
    public static final String NUMBER_EIGHT = "8";
    public static final String NUMBER_NINE = "9";
    public static final String NUMBER_TEN = "10";

    /**
     * ID的默认值
     * 约定 "0" 和 空字符串（""）在系统设计的Entity对象的id字段上，都视为默认值
     */
    public static final String ID_DEFAULT = NUMBER_ZERO;

    /**
     * 约定的ID默认值列表，如果ID的值是这三个值其中之一，就视为默认值
     */
    public static final List<String> ID_DEFAULT_VALUES = Stream.of(null, BLANK, ID_DEFAULT).collect(Collectors.toList());

}
