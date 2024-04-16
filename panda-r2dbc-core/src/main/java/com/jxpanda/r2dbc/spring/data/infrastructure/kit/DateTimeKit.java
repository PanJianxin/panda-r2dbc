package com.jxpanda.r2dbc.spring.data.infrastructure.kit;

import com.jxpanda.r2dbc.spring.data.infrastructure.constant.DateTimeConstant;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.RegexConstant;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DateTimeKit {

    public static LocalDateTime parse(String text) {
        return parse(DateTimeFormatter.ISO_DATE_TIME, text);
    }

    public static LocalDateTime parse(DateTimeFormatter formatter, String text) {
        return formatter.parse(alignText(text, TimeAlignStrategy.NOW, false), LocalDateTime::from);
    }

    public static String format(LocalDateTime localDateTime) {
        return format(DateTimeFormatter.ISO_DATE_TIME, localDateTime);
    }

    public static String format(DateTimeFormatter formatter, LocalDateTime localDateTime) {
        return formatter.format(localDateTime);
    }

    public static String format(LocalDate localDate) {
        return format(DateTimeFormatter.ISO_DATE, localDate);
    }

    public static String format(DateTimeFormatter formatter, LocalDate localDate) {
        return formatter.format(localDate);
    }

    public static String format(LocalTime localTime) {
        return format(DateTimeFormatter.ISO_TIME, localTime);
    }

    public static String format(DateTimeFormatter formatter, LocalTime localTime) {
        return formatter.format(localTime);
    }

    /**
     * 对齐时间文本
     * 直接处理字符串，减少对象的操作，以此提升性能
     * 把yyyy-MM-dd格式的文本对齐为 yyyy-MM-dd HH:mm:ss
     */
    private static String alignText(String text, TimeAlignStrategy strategy, boolean override) {
        var result = text;
        if ((override && RegexKit.isDateTime(text)) || RegexKit.isDate(text)) {
            if (RegexKit.isGeneralDateTime(text)) {
                String dateString = RegexKit.extract(RegexConstant.DATE, text);
                result = dateString + StringConstant.SPACE + strategy.getPadding();
            }
        }
        // 规范化的转为 ISO 8601格式，主要Spring内置的默认转换器是用的ISO 8601格式
        return result.replace(StringConstant.SPACE, "T");
    }

    /**
     * 对齐日期字符串
     * 使用一天的开始时间来填充
     *
     * @param text     日期字符串
     * @param override 是否覆盖已有时间，为true的话，会把字符串上原有的时间覆盖掉
     * @return 对齐后的时间
     */
    public static String alignToBegin(String text, boolean override) {
        return alignText(text, TimeAlignStrategy.BEGIN, override);
    }

    /**
     * 对齐日期字符串
     * 使用一天的结束时间来填充
     *
     * @param text     日期字符串
     * @param override 是否覆盖已有时间，为true的话，会把字符串上原有的时间覆盖掉
     * @return 对齐后的时间
     */
    public static String alignToEnd(String text, boolean override) {
        return alignText(text, TimeAlignStrategy.END, override);
    }

    public static List<String> alignToBeginAndEnd(String text, boolean override) {
        return Stream.of(DateTimeKit.alignToBegin(text, override), DateTimeKit.alignToEnd(text, override)).collect(Collectors.toList());
    }

    public static List<String> alignToBeginAndEnd(String begin, boolean beginOverride, String end, boolean endOverride) {
        return Stream.of(DateTimeKit.alignToBegin(begin, beginOverride), DateTimeKit.alignToEnd(end, endOverride)).collect(Collectors.toList());
    }

    public static LocalDateTime dayBegin(LocalDateTime dateTime) {
        return LocalDateTime.of(dateTime.toLocalDate(), LocalTime.MIN);
    }

    public static LocalDateTime dayEnd(LocalDateTime dateTime) {
        return LocalDateTime.of(dateTime.toLocalDate(), LocalTime.MAX);
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime fromDate(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static boolean isEffective(LocalDateTime dateTime) {
        return dateTime != null && DateTimeConstant.DATETIME_1970_01_01_00_00_00.isBefore(dateTime);
    }

    /**
     * 时间对齐策略
     * 分为3个枚举，具体影响的是时间拼接到日志后面的值
     */
    @AllArgsConstructor
    private enum TimeAlignStrategy {
        /**
         * 用当前时间凭借到日志后面
         */
        NOW() {
            @Override
            public String getPadding() {
                return LocalTime.now().format(DateTimeFormatter.ISO_TIME);
            }
        },
        /**
         * 使用00:00:00拼接到日期后面
         */
        BEGIN() {
            @Override
            public String getPadding() {
                return DateTimeConstant.STRING_TIME_00_00_00;
            }
        },
        /**
         * 使用23:59:59拼接到日期后面
         */
        END() {
            @Override
            public String getPadding() {
                return DateTimeConstant.STRING_TIME_23_59_59;
            }
        };

        public String getPadding() {
            return StringConstant.BLANK;
        }

    }

    public static void main(String[] args) {
        // 指定转换格式
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate startDate = LocalDate.parse("2020-04-03", fmt);
        LocalDate endDate = LocalDate.parse("2020-04-02", fmt);

        System.out.println("总相差的天数:" + endDate.until(startDate, ChronoUnit.DAYS));

    }

}
