package com.article.adapter.shared;

import com.article.port.IdGenerationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PgIdGenerationAdapter implements IdGenerationPort {
    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
