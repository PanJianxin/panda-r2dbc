package com.jxpanda.r2dbc.spring.data.core.enhance.key;

import java.util.UUID;

public class UUIDGenerator implements IdGenerator<UUID> {
    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }

    @Override
    public boolean isIdEffective(UUID id) {
        return id != null;
    }

}
