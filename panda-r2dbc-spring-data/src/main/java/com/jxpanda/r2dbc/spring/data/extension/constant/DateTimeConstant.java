package com.jxpanda.r2dbc.spring.data.extension.constant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

public class DateTimeConstant {

    public static final String STRING_1970_01_01_00_00_00 = "1970-01-01 00:00:00";
    public static final String STRING_TIME_00_00_00 = "00:00:00";
    public static final String STRING_TIME_23_59_59 = "23:59:59";
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 逻辑删除时间标记（java Date类型）
     * 取值为：1970-01-01 00:00:00
     */
    public static final Date DATE_1970_01_01_00_00_00;
    /**
     * 1970-01-01 00:00:00
     */
    public static final LocalDateTime DATETIME_1970_01_01_00_00_00 = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);

    /**
     * 逻辑删除时间标记
     * 取值为：1970-01-01 00:00:00
     */
    public static final LocalDateTime DELETED_DATE = DATETIME_1970_01_01_00_00_00;


    static {
        try {
            DATE_1970_01_01_00_00_00 = new SimpleDateFormat(DATE_FORMAT_PATTERN).parse("1970-01-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


}
