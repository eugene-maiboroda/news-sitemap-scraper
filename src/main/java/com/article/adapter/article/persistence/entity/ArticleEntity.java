package com.article.adapter.article.persistence.entity;

import com.article.domain.ArticleLinkValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "articles")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ArticleEntity {

    @Id
    private String id;

    @Column(name = "category")
    private String category;

    @Column(name = "article_url")
    private String url;

    @Column(name = "published_date")
    private LocalDate publishDate;

    private String title;

    @Column(name = "sub_title")
    private String subTitle;

    private String author;

    @Column(name = "author_link")
    private String authorLink;

    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topics", columnDefinition = "jsonb")
    private List<ArticleLinkValue> topics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<ArticleLinkValue> tags;

    @Column(name = "main_category")
    private String mainCategory;

    @Column(name = "topics_extracted")
    private boolean topicsExtracted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
