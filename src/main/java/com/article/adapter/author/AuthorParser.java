package com.article.adapter.author;

import com.article.adapter.http.HttpContentLoader;
import com.article.domain.Author;
import com.article.domain.AuthorParseResult;
import com.article.port.AuthorParserPort;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorParser implements AuthorParserPort {

    private final HttpContentLoader loader;

    @Override
    public AuthorParseResult parse(Author author) {
        try {
            String url = author.authorUrl();
            Document page = Jsoup.parse(loader.loadText(url), url);
            String name = parseName(page);
            String bio = parseBio(page);
            String avatarUrl = parsePhotoUrl(page);

            return AuthorParseResult.ok(new Author(
                    author.id(),
                    name == null ? author.authorName() : name,
                    url,
                    bio,
                    avatarUrl,
                    null
            ));
        } catch (RuntimeException e) {
            return AuthorParseResult.error(author.authorUrl(), e.toString());
        }
    }

    private static String parseName(Document page) {
        return text(page.selectFirst("main h1"));
    }

    private static String parseBio(Document page) {
        return text(page.selectFirst("main .author-bio"));
    }

    private static String parsePhotoUrl(Document page) {
        Element photo = page.selectFirst("main img.author-photo");
        if (photo == null) {
            return null;
        }

        String url = photo.absUrl("src");
        return url.isBlank() ? null : url;
    }

    private static String text(Element element) {
        return element == null || element.text().isBlank() ? null : element.text();
    }
}
