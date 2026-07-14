package com.article.adapter.article.persistence.entity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "topic1_name")
    private String topic1Name;

    @Column(name = "topic1_url")
    private String topic1Url;

    @Column(name = "topic2_name")
    private String topic2Name;

    @Column(name = "topic2_url")
    private String topic2Url;

    @Column(name = "topic3_name")
    private String topic3Name;

    @Column(name = "topic3_url")
    private String topic3Url;

    @Column(name = "topic4_name")
    private String topic4Name;

    @Column(name = "topic4_url")
    private String topic4Url;

    @Column(name = "topic5_name")
    private String topic5Name;

    @Column(name = "topic5_url")
    private String topic5Url;

    @Column(name = "main_category")
    private String mainCategory;

    @Column(name = "topics_extracted")
    private boolean topicsExtracted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
