package com.article.adapter.article;

import com.article.adapter.export.dto.LinkDto;
import com.article.adapter.http.HttpContentLoader;
import com.article.bootstrap.properties.StorageProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.article.port.StateDirectoryExportPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "collector.site", havingValue = "AMERICAN_HUNTER", matchIfMissing = true)
public class HunterStateDirectory implements StateDirectoryExportPort {

    private static final String URL = "https://www.americanhunter.org/state-fish-game-information/";

    private final HttpContentLoader loader;
    private final ObjectMapper objectMapper;
    private final StorageProps storageProps;

    @Override
    public void export() {
        String html = loader.loadText(URL);
        Directory directory = parseHtml(html);
        Path output = Path.of(storageProps.exportDir()).resolve("state-fish-game-information.json");
        writeDirectory(directory, output);
        log.info("[HUNTER] Exported {} states to {}", directory.states().size(), output);
    }

    private void writeDirectory(Directory directory, Path output) {
        try {
            Files.createDirectories(output.getParent());
            Path temporary = Files.createTempFile(output.getParent(), "hunter-states-", ".tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), directory);
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write Hunter state directory", e);
        }
    }

    Directory parseHtml(String html) {
        Document document = Jsoup.parse(html == null ? "" : html, URL);
        Element title = document.selectFirst("main .web-page h1");
        Element table = document.selectFirst("main .web-page .body-content table");
        if (title == null || table == null) {
            throw new IllegalArgumentException("Missing Hunter state directory title or table");
        }

        Map<String, List<LinkDto>> linksByState = new LinkedHashMap<>();
        for (Element row : table.select("tbody > tr")) {
            Element state = row.selectFirst("td:first-child strong");
            if (state == null || state.text().isBlank()) {
                log.warn("[HUNTER] Directory row {} has no state name", row.elementSiblingIndex() + 1);
                continue;
            }
            String stateName = state.text();
            List<LinkDto> links = linksByState.computeIfAbsent(stateName, name -> new ArrayList<>());
            for (Element link : row.select("td a[href]")) {
                String url = link.absUrl("href");
                String name = link.text();
                if (!url.isBlank() && !name.isBlank()) {
                    links.add(new LinkDto(name, url));
                }
            }
        }
        if (linksByState.isEmpty()) {
            throw new IllegalArgumentException("Hunter state directory contains no recognized states");
        }

        List<State> states = linksByState.entrySet().stream()
                .map(entry -> new State(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
        return new Directory(URL, title.text(), states);
    }

    record Directory(

            String url,

            String title,

            List<State> states
    ) {
    }

    record State(

            String name,

            List<LinkDto> links
    ) {
    }
}
