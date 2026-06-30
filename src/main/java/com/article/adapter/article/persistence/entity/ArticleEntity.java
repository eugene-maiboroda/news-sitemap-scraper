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

    @Column(name = "image_url")
    private String imageUrl;

    private String caption;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
