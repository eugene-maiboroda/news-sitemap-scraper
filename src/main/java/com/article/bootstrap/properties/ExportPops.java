package com.article.bootstrap.properties;

import com.article.domain.status.ExportMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "export")
public record ExportPops(
        ExportMode mode
) {
    public ExportPops {
        mode = mode != null ? mode : ExportMode.CATEGORY;
    }
}
