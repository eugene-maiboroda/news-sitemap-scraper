package com.article.adapter.article.persistence.entity;

import com.article.domain.status.CrawlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "article_links")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LinkArticleEntity {

    @Id
    private String id;

    @Column(name = "article_url")
    private String articleUrl;

    @Column(name = "article_date")
    private LocalDate articleDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CrawlStatus status = CrawlStatus.NEW;

    @Column(name = "error_message")
    private String errorMessage;
}
