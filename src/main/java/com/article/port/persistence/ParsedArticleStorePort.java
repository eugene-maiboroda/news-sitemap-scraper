package com.article.port.persistence;

import com.article.domain.ParseResult;

public interface ParsedArticleStorePort {

    void save(ParseResult.MenuArticleSuccess result);
}
