package com.jxpanda.r2dbc.spring.data.core.enhance;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

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
        return toMask(BitmaskEnum::getCode, items);
    }

    /**
     * 将若干整型掩码拼成一个整型掩码。
     */
    static int toMask(Integer... codes) {
        return toMask(Integer::intValue, codes);
    }

    static int toMask(List<Object> objectList) {
        int mask = 0;
        for (Object it : objectList) {
            if (it instanceof BitmaskEnum be) {
                mask |= be.getCode();
            } else if (it instanceof Number num) {
                mask |= num.intValue();
            }
        }
        return mask;
    }

    /**
     * 通用版 toMask：
     * - extractor：告诉它怎么从 T 里拿到一个 int code
     * - items：任意类型的数组，只要你能给出 extractor，就能合并掩码
     */
    @SafeVarargs
    static <T> int toMask(ToIntFunction<T> extractor, T... items) {
        int mask = 0;
        for (T item : items) {
            mask |= extractor.applyAsInt(item);
        }
        return mask;
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
