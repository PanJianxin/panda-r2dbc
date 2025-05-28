package com.jxpanda.r2dbc.spring.data.core.enhance;

import java.util.LinkedHashSet;
import java.util.Set;

public interface BitmaskEnum extends StandardEnum {


    /**
     * 静态 API：判断 mask 是否“全匹配”所有指定位，
     * 等价于 (mask & toMask(items)) == toMask(items)。
     *
     * @param mask  整型掩码
     * @param items 要检查的枚举项
     */
    static boolean matchesAllBit(int mask, BitmaskEnum... items) {
        int m = toMask(items);
        return (mask & m) == m;
    }

    /**
     * 实例 API：判断当前枚举项的位是否在 mask 中被置位，
     * 等价于 (mask & getCode()) != 0。
     *
     * @param mask 整型掩码
     */
    default boolean matchesAnyBit(int mask) {
        return (mask & getCode()) != 0;
    }

    /**
     * 向 mask 中添加当前枚举位。
     */
    default int addToMask(int mask) {
        return mask | getCode();
    }

    /**
     * 从 mask 中移除当前枚举位。
     */
    default int removeFromMask(int mask) {
        return mask & ~getCode();
    }

    /**
     * 将若干 BitmaskEnum 拼成一个整型掩码。
     */
    static int toMask(BitmaskEnum... items) {
        int m = 0;
        for (BitmaskEnum it : items) {
            m |= it.getCode();
        }
        return m;
    }

    /**
     * （可选）从掩码解析出所有对应的枚举项。
     * 注意：使用时强转回具体的枚举类型。
     */
    static <E extends Enum<E> & BitmaskEnum> Set<E> fromMask(Class<E> enumClass, int mask) {
        Set<E> s = new LinkedHashSet<>();
        for (E e : enumClass.getEnumConstants()) {
            if (e.matchesAnyBit(mask)) {
                s.add(e);
            }
        }
        return s;
    }

}
