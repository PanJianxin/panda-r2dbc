package com.jxpanda.r2dbc.spring.data.infrastructure.constant;

import java.util.regex.Pattern;

/**
 * 正则表达式常量
 *
 * @author Panda
 */
public class RegexConstant {

    /**
     * 所有平年，除了2月29日外的正则表达式
     */
    private static final String COMMON_YEAR_PATTERN = "((?!0000)[0-9]{4}-((0[1-9]|1[0-2])-(0[1-9]|1[0-9]|2[0-8])|(0[13-9]|1[0-2])-(29|30)|(0[13578]|1[02])-31)";
    /**
     * 所有闰年，2月29日的正则表达式
     */
    private static final String LEAP_YEAR_PATTERN = "([0-9]{2}(0[48]|[2468][048]|[13579][26])|(0[48]|[2468][048]|[13579][26])00)-02-29)";
    /**
     * 约定，日期格式为【yyyy-MM-dd】
     * 日期的正则表达式，由平年正则式和闰年正则式构成
     * 结果写在注释里面，方便看，java没有模板字符串真不好用
     * ((?!0000)[0-9]{4}-((0[1-9]|1[0-2])-(0[1-9]|1[0-9]|2[0-8])|(0[13-9]|1[0-2])-(29|30)|(0[13578]|1[02])-31)|([0-9]{2}(0[48]|[2468][048]|[13579][26])|(0[48]|[2468][048]|[13579][26])00)-02-29)
     */
    private static final String DATE_PATTERN = COMMON_YEAR_PATTERN + "|" + LEAP_YEAR_PATTERN;
    /**
     * 约定，时间的格式为【HH:mm:ss】
     * 时间的正则表达式
     */
    private static final String TIME_PATTERN = "(([0-1][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9]))";
    /**
     * 约定，格式为【yyyy-MM-dd HH:mm:ss】
     * 日期+时间的正则表达式
     */
    private static final String DATE_TIME_PATTERN = DATE_PATTERN + StringConstant.SPACE + TIME_PATTERN;

    /**
     * 广义的日期正则表达式
     * 这个与上面那个不同，上面那个只有格式严格匹配【yyyy-MM-dd HH:mm:ss】的时候才为true
     * 而这个可以容忍【yyyy-MM-dd HH:mm:ss】或 【yyyy-MM-dd】两种格式任意匹配即可
     */
    private static final String DATE_TIME_GENERAL_PATTERN = DATE_PATTERN + "(" + StringConstant.SPACE + TIME_PATTERN + ")?";

    /**
     * 手机号的正则表达式
     */
    private static final String PHONE_PATTERN = "1[3-9]\\d{9}";

    /**
     * 纯数字的正则表达式
     */
    private static final String NUMBER_PATTERN = "\\d+";

    /**
     * 空白字符正则表达式
     */
    private static final String BLANK_PATTERN = "\\s+";

    /**
     * 域名
     */
    public static final String DOMAIN_PATTERN = "(http|https)://[\\w\\d.]+/";

    /**
     * 邮箱的正则表达式
     */
    public static final Pattern EMAIL = Pattern.compile("[\\w\\d]+([-_.][\\w\\d]+)*@([\\w\\d]+[-.])+(com|cn|com.cn)");
    /**
     * 日期的正则表达式，匹配的是yyyy-MM-dd这种格式
     */
    public static final Pattern DATE = Pattern.compile(DATE_PATTERN);
    /**
     * 时间的正则表达式，匹配的是HH:mm:ss这个格式
     */
    public static final Pattern TIME = Pattern.compile(TIME_PATTERN);
    /**
     * 这个是上面两个的结合，匹配的是yyyy-MM-dd HH:mm:ss这个格式（只精确到秒）
     * 狭义的日期正则表达式
     */
    public static final Pattern DATE_TIME = Pattern.compile(DATE_TIME_PATTERN);
    /**
     * 广义的日期正则表达式
     * 这个与上面那个不同，上面那个只有格式严格匹配【yyyy-MM-dd HH:mm:ss】的时候才为true
     * 而这个可以容忍【yyyy-MM-dd HH:mm:ss】或 【yyyy-MM-dd】两种格式任意匹配即可
     */
    public static final Pattern DATE_TIME_GENERAL = Pattern.compile(DATE_TIME_GENERAL_PATTERN);

    /**
     * 手机号码的正则表达式
     */
    public static final Pattern PHONE = Pattern.compile(PHONE_PATTERN);

    /**
     * 纯数字的正则表达式
     */
    public static final Pattern NUMBER = Pattern.compile(NUMBER_PATTERN);

    /**
     * 空白字符
     */
    public static final Pattern BLANK = Pattern.compile(BLANK_PATTERN);

    /**
     * 域名
     */
    public static final Pattern DOMAIN = Pattern.compile(DOMAIN_PATTERN);

}
