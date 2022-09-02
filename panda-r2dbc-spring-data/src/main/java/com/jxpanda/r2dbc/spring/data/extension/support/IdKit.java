package com.jxpanda.r2dbc.spring.data.extension.support;

import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;

public class IdKit {

    public static <ID> boolean isIdNotEffective(ID id) {
        return !isIdEffective(id);
    }

    public static <ID> boolean isIdEffective(ID id) {
        if (id == null) {
            return false;
        }
        if (id instanceof Number idNumber) {
            return idNumber.intValue() != 0;
        }
        if (id instanceof String idString) {
            return !StringConstant.ID_DEFAULT_VALUES.contains(idString);
        }
        return false;
    }

}
