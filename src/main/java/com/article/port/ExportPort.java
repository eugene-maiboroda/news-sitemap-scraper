package com.article.port;

import com.article.application.dto.ExportData;
import com.article.domain.Article;

import java.util.List;

public interface ExportPort {

    void export(ExportData data);
}
