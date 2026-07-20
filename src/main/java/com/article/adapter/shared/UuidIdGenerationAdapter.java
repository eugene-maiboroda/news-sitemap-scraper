package com.article.adapter.shared;

import com.article.port.out.IdGenerationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidIdGenerationAdapter implements IdGenerationPort {
    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
