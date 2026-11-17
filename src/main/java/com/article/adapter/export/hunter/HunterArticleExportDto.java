package com.article.adapter.export.hunter;

import com.article.domain.NamedLink;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
        "articleUrl", "title", "description", "publishedDate", "author", "authorLink", "menuPaths",
        "tags", "images", "videos", "affiliateDisclosure", "content", "footerTags", "relatedStories"
})
record HunterArticleExportDto(

        String articleUrl,

        String title,

        String description,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
        LocalDate publishedDate,

        String author,

        String authorLink,

        Map<String, List<List<String>>> menuPaths,

        List<NamedLink> tags,

        List<Photo> images,

        List<Video> videos,

        String affiliateDisclosure,

        String content,

        List<NamedLink> footerTags,

        List<NamedLink> relatedStories
) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"caption", "originalImageUrl", "position"})
    record Photo(

            String caption,

            String originalImageUrl,

            int position
    ) {}

    @JsonPropertyOrder({"url", "position"})
    record Video(

            String url,

            int position
    ) {}
}
