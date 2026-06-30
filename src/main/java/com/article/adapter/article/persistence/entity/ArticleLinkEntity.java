package com.article.adapter.article.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "article_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleLinkEntity {

    @Id
    private UUID id;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "article_url")
    private String articleUrl;

    @Column(name = "article_date")
    private LocalDate articleDate;

    private boolean parsed;

    public ArticleLinkEntity(String teamName, String articleUrl, LocalDate articleDate) {
        this.id = UUID.randomUUID();
        this.teamName = teamName;
        this.articleUrl = articleUrl;
        this.articleDate = articleDate;
        this.parsed = false;
    }
}