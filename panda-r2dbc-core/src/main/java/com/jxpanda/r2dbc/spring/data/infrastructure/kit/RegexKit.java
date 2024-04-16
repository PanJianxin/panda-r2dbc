package com.jxpanda.r2dbc.spring.data.infrastructure.kit;



import com.jxpanda.r2dbc.spring.data.infrastructure.constant.RegexConstant;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式工具，先整一些常用的
 *
 * @author Panda
 */
public class RegexKit {

    /**
     * 判断是否是邮箱
     */
    public static boolean isEmail(String text) {
        return isMatch(RegexConstant.EMAIL, text);
    }

    /**
     * 是否是日期【yyyy-MM-dd】
     */
    public static boolean isDate(String text) {
        return isMatch(RegexConstant.DATE, text);
    }

    /**
     * 是否是时间【HH:mm:ss】
     */
    public static boolean isTime(String text) {
        return isMatch(RegexConstant.TIME, text);
    }

    /**
     * 是否是日期和时间【yyyy-MM-dd HH:mm:ss】
     */
    public static boolean isDateTime(String text) {
        return isMatch(RegexConstant.DATE_TIME, text);
    }

    /**
     * 是否是广义上的日期
     * 【yyyy-MM-dd HH:mm:ss】或【yyyy-MM-dd】
     */
    public static boolean isGeneralDateTime(String text) {
        return isMatch(RegexConstant.DATE_TIME_GENERAL, text);
    }

    /**
     * 是否是手机号码
     */
    public static boolean isPhone(String text) {
        return isMatch(RegexConstant.PHONE, text);
    }

    /**
     * 是否是纯数字
     */
    public static boolean isNumber(String text) {
        return isMatch(RegexConstant.NUMBER, text);
    }

    /**
     * 利用正则表达式，从文本中提取最近匹配到的内容
     * 如果提取不到，则返回空字符串
     *
     * @param pattern 正则表达式
     * @param text    待提取文本
     * @return 提取到的文本片段
     */
    public static String extract(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        boolean find = matcher.find();
        return find ? matcher.group() : StringConstant.BLANK;
    }

    /**
     * 判断是否被正则表达式匹配
     */
    public static boolean isMatch(Pattern pattern, String text) {
        return text != null && pattern.matcher(text).matches();
    }

    /**
     * 判断是否被正则表达式匹配
     */
    public static boolean isFind(Pattern pattern, String text) {
        return text != null && pattern.matcher(text).find();
    }

}
